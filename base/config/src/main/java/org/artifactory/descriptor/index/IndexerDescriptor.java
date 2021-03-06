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

package org.artifactory.descriptor.index;

import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.TaskDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.jfrog.common.DiffAtomic;
import org.jfrog.common.DiffReference;
import org.jfrog.common.GenerateDiffFunction;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.SortedSet;

@XmlType(name = "IndexerType", propOrder = {"enabled", "cronExp", "includedRepositories"},
        namespace = Descriptor.NS)
@GenerateDiffFunction
public class IndexerDescriptor implements TaskDescriptor {

    private static final long serialVersionUID = 1L;

    private boolean enabled;

    private String cronExp;

    @XmlIDREF
    @XmlElementWrapper(name = "includedRepositories")
    @XmlElement(name = "repositoryRef", type = RepoBaseDescriptor.class, required = false)
    private SortedSet<? extends RepoBaseDescriptor> includedRepositories;

    public IndexerDescriptor() {
        // By Default index once a day at 05:23AM
        this.cronExp = "0 23 5 * * ?";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    @Nullable
    @DiffReference
    @DiffAtomic
    public SortedSet<? extends RepoBaseDescriptor> getIncludedRepositories() {
        return includedRepositories;
    }

    public void setIncludedRepositories(
            SortedSet<? extends RepoBaseDescriptor> includedRepositories) {
        this.includedRepositories = includedRepositories;
    }

    public void removeIncludedRepository(RepoBaseDescriptor repoBaseDescriptor) {
        if (includedRepositories != null) {
            includedRepositories.remove(repoBaseDescriptor);
        }
    }

    @Override
    public boolean sameTaskDefinition(TaskDescriptor otherDescriptor) {
        if (otherDescriptor == null || !(otherDescriptor instanceof IndexerDescriptor)) {
            throw new IllegalArgumentException(
                    "Cannot compare indexer descriptor " + this + " with " + otherDescriptor);
        }
        IndexerDescriptor indexerDesc = (IndexerDescriptor) otherDescriptor;
        return indexerDesc.enabled == this.enabled &&
                StringUtils.equals(indexerDesc.cronExp, this.cronExp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexerDescriptor that = (IndexerDescriptor) o;

        if (enabled != that.enabled) {
            return false;
        }
        if(cronExp != null ? !cronExp.equals(that.cronExp) : that.cronExp != null) {
            return false;
        }
        if (includedRepositories != null ? !includedRepositories.equals(that.includedRepositories) :
                that.includedRepositories != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + (cronExp != null ? cronExp.hashCode() : 0);
        result = 31 * result + (includedRepositories != null ? includedRepositories.hashCode() : 0);
        return result;
    }
}