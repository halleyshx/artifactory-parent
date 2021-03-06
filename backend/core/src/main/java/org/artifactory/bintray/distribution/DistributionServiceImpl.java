/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.bintray.distribution;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.jfrog.bintray.client.api.BintrayCallException;
import com.jfrog.bintray.client.api.handle.RepositoryHandle;
import com.jfrog.bintray.client.api.handle.SubjectHandle;
import com.jfrog.bintray.client.api.model.Pkg;
import com.jfrog.bintray.client.api.model.Repository;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.artifactory.api.bintray.distribution.DistributionService;
import org.artifactory.api.bintray.distribution.model.BintrayDistInfoModel;
import org.artifactory.api.bintray.distribution.reporting.DistributionReporter;
import org.artifactory.api.bintray.distribution.resolver.DistributionCoordinatesResolver;
import org.artifactory.api.bintray.distribution.resolver.DistributionRuleFilterType;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.util.AqlSearchablePath;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.bintray.distribution.util.DistributionUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.descriptor.repo.distribution.rule.DistributionRule;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.DistributionRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.distribution.DistributionConstants;
import org.jfrog.client.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.artifactory.bintray.distribution.util.DistributionUtils.createTimingOutMatcher;
import static org.artifactory.util.distribution.DistributionConstants.MANIFEST_FILENAME;

/**
 * @author Dan Feldman
 */
@Service
public class DistributionServiceImpl implements DistributionService {
    private static final Logger log = LoggerFactory.getLogger(DistributionServiceImpl.class);

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private AqlService aqlService;

    private static final int PROP_SEARCH_BATCH = 5;

    @Override
    public Map<RepoPath, Properties> getPathInformation(List<RepoPath> paths, @Nullable String productName,
            DistributionReporter status) {
        paths = adjustDockerDirPaths(paths, status);
        Map<RepoPath, Properties> pathProperties = getPathProperties(paths);
        if (pathProperties.isEmpty()) {
            //If any paths were found they will have entries in the map with empty Properties as values
            status.error("No Artifacts found for any of the paths you specified, nothing to distribute.",
                    SC_NOT_FOUND, log);
        } else {
            DistributionUtils.insertProductNameDummyProp(productName, pathProperties);
            addLayoutTokens(pathProperties, status);
        }
        return pathProperties;
    }

    /**
     * {@param distPaths} are required to be a list of full repo paths in the form "repoKey/path"
     */
    @Override
    public List<RepoPath> inputPathsToRepoPaths(List<String> distPaths, DistributionReporter status) {
        return distPaths.stream()
                .map(path -> pathToRepoPath(status, path))
                .filter(Objects::nonNull)
                .filter(repoPath -> isValidPath(repoPath, status))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Path must exist, and be in a local repo
     */
    private boolean isValidPath(RepoPath repoPath, DistributionReporter status) {
        LocalRepoDescriptor descriptor = repoService.localCachedOrDistributionRepoDescriptorByKey(repoPath.getRepoKey());
        boolean isValidPath = true;
        if (descriptor == null) {
            status.warn(repoPath.toPath(), "No such repository: " + repoPath.getRepoKey(), SC_NOT_FOUND, log);
            isValidPath = false;
        } else if (descriptor.isCache()) {
            status.warn(repoPath.toPath(), "Distribution skipping path from remote repository :" + repoPath, SC_BAD_REQUEST, log);
            isValidPath = false;
        }
        return isValidPath;
    }

    private RepoPath pathToRepoPath(DistributionReporter status, String path) {
        try {
            return RepoPathFactory.create(path);
        } catch (Exception e) {
            status.warn(path, "Invalid path given: '" + path, HttpStatus.SC_NOT_FOUND, e, log);
        }
        return null;
    }

    /**
     * Adds capture groups to the resolver if there is a filter to use and the given text matches it.
     *
     * @return <code>true</code> if the given text matches the filter, or <code>false</code> otherwise.
     * Blank filter always returns <code>true</code>.
     */
    @Override
    public boolean addCaptureGroupsToRuleResolverIfMatches(DistributionRule rule, DistributionRuleFilterType filterType,
            String filterRegex, String textToMatch, DistributionCoordinatesResolver resolver, DistributionReporter status) {
        if (StringUtils.isNotBlank(filterRegex)) {
            Pattern filterPattern = Pattern.compile(filterRegex);
            Matcher filterMatcher = createTimingOutMatcher(textToMatch, filterPattern);
            if (filterMatcher.matches()) {
                resolver.addCaptureGroups(filterType, filterMatcher, status);
            } else {
                status.debug("Failed to match rule " + rule.getName() + "(type: " + rule.getType() + " with "
                        + filterType.getQualifier() + " filter " + filterRegex + " to value " + textToMatch, log);
                return false;
            }
        }
        return true;
    }

    @Override
    public BintrayDistInfoModel buildInfoModel(RepoPath repoPath) throws IOException {
        if (isRepoLevel(repoPath)) {
            return prepareBintrayRepoModel(repoPath);
        } else if (isPackageLevel(repoPath)) {
            return prepareBintrayPackageModel(repoPath);
        }
        return null;
    }

    private BintrayDistInfoModel prepareBintrayRepoModel(RepoPath repoPath) throws IOException {
        BintrayDistInfoModel model = new BintrayDistInfoModel();
        String repoName = PathUtils.getFirstPathElement(repoPath.getPath());
        try {
            DistributionRepo distRepo = repoService.distributionRepoByKey(repoPath.getRepoKey());
            DistributionRepoDescriptor descriptor = distRepo.getDescriptor();
            SubjectHandle subject = distRepo.getClient().subject(descriptor.getBintrayApplication().getOrg());
            Repository repository = subject.repository(repoName).get();
            model.packageType = repository.getType();
            model.visibility = repository.isPrivate() ? "Private" : "Public";
        } catch (BintrayCallException e) {
            if (e.getStatusCode() == 404) {
                model.errorMessage = "Repo " + repoName + " was deleted from Bintray.";
            }
            log.error("Error while fetching Bintray repo info: " + e.getMessage(), e);
        }
        return model;
    }

    private BintrayDistInfoModel prepareBintrayPackageModel(RepoPath repoPath) throws IOException {
        BintrayDistInfoModel model = new BintrayDistInfoModel();
        String repoName = PathUtils.getFirstPathElement(repoPath.getPath());
        String packageName = PathUtils.getLastPathElement(repoPath.getPath());
        try {
            DistributionRepo distRepo = repoService.distributionRepoByKey(repoPath.getRepoKey());
            DistributionRepoDescriptor descriptor = distRepo.getDescriptor();
            RepositoryHandle repository = distRepo.getClient().subject(descriptor.getBintrayApplication().getOrg()).repository(repoName);
            Pkg pkg = repository.pkg(packageName).get();
            model.licenses = Joiner.on(", ").join(pkg.licenses());
            model.vcsUrl = pkg.vcsUrl();
        } catch (BintrayCallException e) {
            if (e.getStatusCode() == 404) {
                model.errorMessage = "Package " + packageName + " was deleted from Bintray.";
            }
            log.error("Error while fetching Bintray repo info: " + e.getMessage(), e);
        }
        return model;
    }

    private boolean isRepoLevel(RepoPath repoPath) {
        return PathUtils.getPathElements(repoPath.getPath()).length == 1;
    }

    private boolean isPackageLevel(RepoPath repoPath) {
        return PathUtils.getPathElements(repoPath.getPath()).length == 2;
    }

    /**
     * @return a mapping of {@param paths} and the properties set on them, returns an empty (not null)
     * {@link Properties} object for paths that had no properties
     *
     * Searches run in batches of {@link DistributionServiceImpl#PROP_SEARCH_BATCH} to avoid encountering over complex
     * exceptions from garbage derby (RTFACT-10322)
     */
    private Map<RepoPath, Properties> getPathProperties(List<RepoPath> paths) {
        return IntStream.range(0, ((paths.size() + (PROP_SEARCH_BATCH - 1)) / PROP_SEARCH_BATCH))
                .mapToObj(i -> paths.subList((i * PROP_SEARCH_BATCH), Math.min(paths.size(), ((i + 1) * PROP_SEARCH_BATCH))))
                .map(this::processPropQueryBatch)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<RepoPath, Properties> processPropQueryBatch(List<RepoPath> paths) {
        AqlBase pathProperties = getAqlSearchClauseForPaths(paths);
        log.trace("Built aql query {} to search path properties on paths: {}", pathProperties.toNative(0), paths.toString());
        AqlEagerResult<AqlBaseFullRowImpl> results = aqlService.executeQueryEager(pathProperties);
        HashMultimap<RepoPath, AqlBaseFullRowImpl> pathRows = getPathRows(results);
        return pathsWithPropsFromAql(pathRows);
    }

    private AqlBase getAqlSearchClauseForPaths(Collection<RepoPath> paths) {
        AqlBase.OrClause aqlPaths = AqlUtils.getSearchClauseForPaths(paths
                .stream()
                .map(AqlSearchablePath::new)
                .collect(Collectors.toList()));
        return AqlApiItem.create().filter(aqlPaths).include(AqlApiItem.property().key(), AqlApiItem.property().value());
    }

    private HashMultimap<RepoPath, AqlBaseFullRowImpl> getPathRows(AqlEagerResult<AqlBaseFullRowImpl> results) {
        return AqlUtils.aggregateResultsByPath(results.getResults());
    }

    /**
     * @return each of the paths (the keyset of {@param resultsByPath}) with all properties the AQL search returned
     * for it.
     */
    private Map<RepoPath, Properties> pathsWithPropsFromAql(Multimap<RepoPath, AqlBaseFullRowImpl> pathRows) {
        Map<RepoPath, Properties> pathProps = Maps.newHashMap();
        for (RepoPath path : pathRows.keySet()) {
            Properties props = new PropertiesImpl();
            pathRows.get(path).stream()
                    .filter(row -> row.getKey() != null && row.getValue() != null)
                    .forEach(row -> props.put(row.getKey(), row.getValue()));
            pathProps.put(path, props);
        }
        log.trace("Returning path->properties mapping: {}", pathProps.toString());
        return pathProps;
    }

    /**
     * Adds layout tokens and values as {@param pathProperties} according to each path
     */
    private void addLayoutTokens(Map<RepoPath, Properties> pathProperties, DistributionReporter status) {
        Map<String, List<String>> layoutTokensByRepoKey = Maps.newHashMap();
        for (Map.Entry<RepoPath, Properties> entry : pathProperties.entrySet()) {
            RepoPath path = entry.getKey();
            String repoKey = path.getRepoKey();
            List<String> layoutTokens;
            if (layoutTokensByRepoKey.containsKey(repoKey)) {
                layoutTokens = layoutTokensByRepoKey.get(repoKey);
            } else {
                LocalRepoDescriptor descriptor = repoService.localOrCachedRepoDescriptorByKey(repoKey);
                if (descriptor == null) {
                    String fullPath = path.toPath();
                    status.warn(fullPath, "No such repo " + repoKey + " , Distribution coordinates will not be " +
                            "resolved for artifact " + fullPath + " .", SC_NOT_FOUND, log);
                    continue;
                }
                layoutTokens = RepoLayoutUtils.getLayoutTokens(descriptor.getRepoLayout());
                log.trace("Adding layout tokens {} retrieved from repo {} layout {}.", layoutTokens, repoKey,
                        descriptor.getRepoLayout().getName());
                layoutTokensByRepoKey.put(repoKey, layoutTokens);
            }
            addLayoutTokenValues(pathProperties, path, layoutTokens, status);
        }
    }

    private void addLayoutTokenValues(Map<RepoPath, Properties> pathProperties, RepoPath path, List<String> layoutTokens,
            DistributionReporter status) {
        try {
            ModuleInfo moduleInfo = repoService.getItemModuleInfo(path);
            for (String token : layoutTokens) {
                String strippedToken = DistributionConstants.stripTokenBrackets(token);
                String value = ModuleInfoUtils.getTokenValue(moduleInfo, strippedToken);
                log.trace("Layout token matched value {} for layout token {} on path {}", value, token, path.toPath());
                pathProperties.get(path).put(DistributionConstants.wrapToken(strippedToken), value);
            }
        } catch (Exception e) {
            String fullPath = path.toPath();
            status.warn(fullPath, "Failed to get layout information for artifact '" + fullPath + "', layout tokens " +
                    "will not be resolved for it.", SC_BAD_REQUEST, log);
        }
    }

    /**
     * Identifies folders in Docker repos that were given in {@param paths} and adjust the path to point to the
     * manifest file.
     */
    private List<RepoPath> adjustDockerDirPaths(List<RepoPath> paths, DistributionReporter status) {
        //Holds newly adjusted paths and the paths that were not identified as docker folders
        Set<RepoPath> adjustedPaths = Sets.newHashSet();
        for (RepoPath path : paths) {
            if(isFolder(path, status) && isDockerRepo(path)) {
                adjustedPaths.addAll(adjustDirToManifests(path, status));
            } else {
                adjustedPaths.add(path);
            }
        }
        return new ArrayList<>(adjustedPaths);
    }

    /**
     * @return the path to the manifest.json file if this {@param path} points to a docker tag directory, or the path
     * to ALL manifest files under this parent if path points to a higher node in the tree
     */
    private List<RepoPath> adjustDirToManifests(RepoPath path, DistributionReporter status) {
        List<RepoPath> adjustedPaths = Lists.newArrayList();
        List<ItemInfo> manifestChildren = Lists.newArrayList();
        status.status(path.toPath(), "Path " + path.toPath() + " is a folder in a Docker repo, checking for tag " +
                "manifests.", log);
        path = adjustDockerSearchablePathToActualPath(path, status);
        manifestChildren = getManifestsUnderPath(path, status, manifestChildren);
        if (manifestChildren.size() < 1) {
            status.warn(path.toPath(), "Directory " + path.toPath() + " has no manifest.json files under it, skipping.",
                    SC_NOT_FOUND, log);
            adjustedPaths.add(path);
        } else {
            adjustedPaths.addAll(manifestChildren.stream()
                    .map(ItemInfo::getRepoPath)
                    .collect(Collectors.toList()));
            status.debug("Adjusting path " + path.toPath() + " to its underlying manifest.json(s) in "
                    + adjustedPaths.toString(), log);
            status.status("Found manifest under " + path.toPath(), log);
        }
        return adjustedPaths;
    }

    /**
     * If {@param path} is in Aql Searchable Path form (i.e. \**\* or \*) this method adjusts it to the closest parent
     * representing an actual folder.
     */
    private RepoPath adjustDockerSearchablePathToActualPath(RepoPath path, DistributionReporter status) {
        try {
            if (path.toPath().endsWith("**/*")) {
                path = path.getParent().getParent(); //ends with **/* --> 2 getParent is safe
            } else if (path.toPath().endsWith("/*")) {
                path = path.getParent();
            }
        } catch (Exception e) {
            status.debug("Error adjusting docker folder path: '" + path.toPath(), log);
        }
        return path;
    }

    private List<ItemInfo> getManifestsUnderPath(RepoPath path, DistributionReporter status, List<ItemInfo> manifestChildren) {
        try {
            manifestChildren = repoService.getChildren(path).stream()
                    .filter(item -> !item.isFolder())
                    .filter(item -> MANIFEST_FILENAME.equals(item.getName()))
                    .collect(Collectors.toList());
        } catch (ItemNotFoundRuntimeException nfe) {
            status.error("Path " + path.toPath() + " not found.", SC_NOT_FOUND, log);
        }
        return manifestChildren;
    }

    /**
     * @return true if {@param path} is in a Docker repo.
     */
    private boolean isDockerRepo(RepoPath path) {
        RepoDescriptor descriptor = repoService.repoDescriptorByKey(path.getRepoKey());
        return descriptor != null && RepoType.Docker.equals(descriptor.getType());
    }

    private boolean isFolder(RepoPath fullPath, DistributionReporter status) {
        String path = fullPath.getPath();
        try {
            return path.endsWith("**/*") || path.endsWith("/*") || path.endsWith("/")
                    || repoService.getItemInfo(fullPath).isFolder();
        } catch (ItemNotFoundRuntimeException inf) {
            status.error("Path " + fullPath.toPath() + " not found.", SC_NOT_FOUND, log);
        }
        return false;
    }
}
