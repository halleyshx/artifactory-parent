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

package org.artifactory.ui.rest.model.admin.advanced.configdescriptor;

import org.artifactory.api.rest.restmodel.JsonUtil;
import org.artifactory.rest.common.model.RestModel;

/**
 * @author Chen Keinan
 */
public class ConfigDescriptorModel implements RestModel {

    private String configXml;

    public ConfigDescriptorModel() {
    }

    public ConfigDescriptorModel(String configXml) {
        this.configXml = configXml;
    }

    public String getConfigXml() {
        return configXml;
    }

    public void setConfigXml(String configXml) {
        this.configXml = configXml;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(configXml);
    }
}
