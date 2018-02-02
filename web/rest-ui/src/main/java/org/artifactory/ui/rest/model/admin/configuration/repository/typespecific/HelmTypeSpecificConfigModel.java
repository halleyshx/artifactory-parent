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
import org.artifactory.rest.common.util.JsonUtil;

import static org.artifactory.repo.config.RepoConfigDefaultValues.DEFAULT_VIRTUAL_RETRIEVAL_CACHE_PERIOD;
import static org.artifactory.repo.config.RepoConfigDefaultValues.HELM_URL;

/**
 * @author Nadav Yogev
 */
public class HelmTypeSpecificConfigModel implements TypeSpecificConfigModel {

    //virtual
    private Long virtualRetrievalCachePeriodSecs = DEFAULT_VIRTUAL_RETRIEVAL_CACHE_PERIOD;

    @Override
    public RepoType getRepoType() {
        return RepoType.Helm;
    }

    @Override
    public String getUrl() {
        return HELM_URL;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }

    public Long getVirtualRetrievalCachePeriodSecs() {
        return virtualRetrievalCachePeriodSecs;
    }

    public void setVirtualRetrievalCachePeriodSecs(Long virtualRetrievalCachePeriodSecs) {
        this.virtualRetrievalCachePeriodSecs = virtualRetrievalCachePeriodSecs;
    }
}