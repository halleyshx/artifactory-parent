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

package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.descriptor.repo.vcs.VcsGitProvider;
import org.artifactory.descriptor.repo.vcs.VcsType;
import org.artifactory.repo.config.RepoConfigDefaultValues;
import org.artifactory.rest.common.util.JsonUtil;
import org.codehaus.jackson.annotate.JsonIgnore;

import static org.artifactory.repo.config.RepoConfigDefaultValues.*;

/**
 * @author Dan Feldman
 */
public class VcsTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //remote
    // TODO: [by dan] when this enum has more than one value and UI will give choice - remove annotation and add enum to RepositoryFieldValues
    @JsonIgnore
    protected VcsType vcsType = DEFAULT_VCS_TYPE;
    protected VcsGitProvider gitProvider = DEFAULT_GIT_PROVIDER;
    protected Integer maxUniqueSnapshots = DEFAULT_MAX_UNIQUE_SNAPSHOTS;
    protected String downloadUrl;
    protected Boolean listRemoteFolderItems = DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE;

    public VcsType getVcsType() {
        return vcsType;
    }

    public void setVcsType(VcsType vcsType) {
        this.vcsType = vcsType;
    }

    public VcsGitProvider getGitProvider() {
        return gitProvider;
    }

    public void setGitProvider(VcsGitProvider gitProvider) {
        this.gitProvider = gitProvider;
    }

    public Integer getMaxUniqueSnapshots() {
        return maxUniqueSnapshots;
    }

    public void setMaxUniqueSnapshots(Integer maxUniqueSnapshots) {
        this.maxUniqueSnapshots = maxUniqueSnapshots;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Boolean isListRemoteFolderItems() {
        return listRemoteFolderItems;
    }

    public void setListRemoteFolderItems(Boolean listRemoteFolderItems) {
        this.listRemoteFolderItems = listRemoteFolderItems;
    }

    @Override
    public RepoType getRepoType() {
        return RepoType.VCS;
    }

    @Override
    public String getUrl() {
        return RepoConfigDefaultValues.VCS_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
