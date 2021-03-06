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

package org.artifactory.ui.rest.model.builds;

import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.rest.common.model.RestPaging;
import org.artifactory.rest.common.util.BintrayRestHelper;

/**
 * @author Chen Keinan
 */
public class GeneralBuildInfo extends BaseModel implements RestPaging {

    private String buildName;
    private String buildNumber;
    private String ciUrl;
    private String releaseStatus;
    private String agent;
    private String buildAgent;
    private String lastBuildTime;
    private String duration;
    private String principal;
    private  String artifactoryPrincipal;
    private String url;
    private  Long time;
    private String buildStat;
    // private Boolean allowPushToBintray;
    private Boolean userCanDistribute;


    public GeneralBuildInfo() {}

    public GeneralBuildInfo(BuildBuilder buildBuilder) {
        this.buildName = buildBuilder.buildName;
        this.lastBuildTime = buildBuilder.lastBuildTime;
        this.buildNumber = buildBuilder.buildNumber;
        this.ciUrl = buildBuilder.ciUrl;
        this.releaseStatus = buildBuilder.releaseStatus;
        this.agent = buildBuilder.agent;
        this.buildAgent = buildBuilder.buildAgent;
        this.duration = buildBuilder.duration;
        this.principal = buildBuilder.principal;
        this.artifactoryPrincipal = buildBuilder.artifactoryPrincipal;
        this.url = buildBuilder.url;
        this.time = (buildBuilder.time == null) ? null : buildBuilder.time;
        this.buildStat = buildBuilder.buildStat;
        //TODO [by dan]: Decide if we're bringing back push to bintray for builds -> remove this if not
        // allowPushToBintray = BintrayRestHelper.isPushToBintrayAllowed(null, null);
        userCanDistribute = BintrayRestHelper.userCanDistributeBuild();
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getLastBuildTime() {
        return lastBuildTime;
    }

    public void setLastBuildTime(String lastBuildTime) {
        this.lastBuildTime = lastBuildTime;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getCiUrl() {
        return ciUrl;
    }

    public void setCiUrl(String ciUrl) {
        this.ciUrl = ciUrl;
    }

    public String getReleaseStatus() {
        return releaseStatus;
    }

    public void setReleaseStatus(String releaseStatus) {
        this.releaseStatus = releaseStatus;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getBuildAgent() {
        return buildAgent;
    }

    public void setBuildAgent(String buildAgent) {
        this.buildAgent = buildAgent;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getArtifactoryPrincipal() {
        return artifactoryPrincipal;
    }

    public void setArtifactoryPrincipal(String artifactoryPrincipal) {
        this.artifactoryPrincipal = artifactoryPrincipal;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBuildStat() {
        return buildStat;
    }

    public void setBuildStat(String buildStat) {
        this.buildStat = buildStat;
    }

    /*public Boolean getAllowPushToBintray() {
        return allowPushToBintray;
    }*/

    public Boolean getUserCanDistribute() {
        return userCanDistribute;
    }

    public static class BuildBuilder {

        private String buildName;
        private String lastBuildTime;
        private String buildNumber;
        private String ciUrl;
        private String releaseStatus;
        private String agent;
        private String buildAgent;
        private String duration;
        private String principal;
        private String artifactoryPrincipal;
        private String url;
        private Long time;
        private String buildStat;

        public BuildBuilder() {
        }

        public GeneralBuildInfo build() {
            return new GeneralBuildInfo(this);
        }

        public BuildBuilder buildName(String buildName) {
            this.buildName = buildName;
            return this;
        }

        public BuildBuilder lastBuildTime(String lastBuildTime) {
            this.lastBuildTime = lastBuildTime;
            return this;
        }

        public BuildBuilder buildNumber(String buildNumber) {
            this.buildNumber = buildNumber;
            return this;
        }

        public BuildBuilder ciUrl(String ciUrl) {
            this.ciUrl = ciUrl;
            return this;
        }

        public BuildBuilder releaseStatus(String releaseStatus) {
            this.releaseStatus = releaseStatus;
            return this;
        }
        public BuildBuilder agent(String agent) {
            this.agent = agent;
            return this;
        }
        public BuildBuilder buildAgent(String buildAgent) {
            this.buildAgent = buildAgent;
            return this;
        }
        public BuildBuilder duration(String duration) {
            this.duration = duration;
            return this;
        }
        public BuildBuilder principal(String principal) {
            this.principal = principal;
            return this;
        }
        public BuildBuilder artifactoryPrincipal(String artifactoryPrincipal) {
            this.artifactoryPrincipal = artifactoryPrincipal;
            return this;
        }
        public BuildBuilder url(String url) {
            this.url = url;
            return this;
        }

        public BuildBuilder time(Long time) {
            this.time = time;
            return this;
        }

        public BuildBuilder buildStat(String buildStat) {
            this.buildStat = buildStat;
            return this;
        }
    }
}
