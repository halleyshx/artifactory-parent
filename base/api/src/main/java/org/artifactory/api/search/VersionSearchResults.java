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

package org.artifactory.api.search;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;

import java.util.Set;

/**
 * Holds a list of version search results
 *
 * @author Dan Feldman
 */
@XStreamAlias("searchResults")
public class VersionSearchResults extends ItemSearchResults<VersionUnitSearchResult> {

    //Signifies if the query exceeded the allowed query limit
    private boolean queryLimitExceeded;

    private boolean searchHadErrors;

    public VersionSearchResults(Set<VersionUnitSearchResult> results, long count, boolean queryLimitExceeded,
            boolean searchHadErrors) {
        super(Lists.newArrayList(results), count);
        this.queryLimitExceeded = queryLimitExceeded;
        this.searchHadErrors = searchHadErrors;
    }

    public boolean isQueryLimitExceeded() {
        return queryLimitExceeded;
    }

    public boolean isSearchHadErrors() {
        return searchHadErrors;
    }
}