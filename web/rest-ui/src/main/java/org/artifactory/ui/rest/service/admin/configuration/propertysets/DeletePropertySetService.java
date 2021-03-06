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

package org.artifactory.ui.rest.service.admin.configuration.propertysets;

import org.apache.http.HttpStatus;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.configuration.propertysets.DeletePropertySetModel;
import org.artifactory.util.stream.BiOptional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeletePropertySetService<T extends DeletePropertySetModel> implements RestService<T> {

    @Autowired
    CentralConfigService configService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        T model = request.getImodel();
        for (String toDelete : model.getPropertySetNames()) {
            MutableCentralConfigDescriptor descriptor = configService.getMutableDescriptor();
            BiOptional.of(descriptor.getPropertySets().stream()
                    .filter(prop -> prop.getName().equals(toDelete))
                    .findFirst())
                    .ifPresent(foundProp -> deleteProp(descriptor, foundProp.getName()))
                    .ifNotPresent(() -> response.error("Property set '" + toDelete + "' does not exist.")
                            .responseCode(HttpStatus.SC_NOT_FOUND));
        }
        if(model.getPropertySetNames().size()>1){
            response.info("Successfully removed "+model.getPropertySetNames().size()+" property sets");
        }else if(model.getPropertySetNames().size()==1){
            response.info("Successfully removed property set '" + model.getPropertySetNames().get(0) + "'");
        }
    }

    private void deleteProp(MutableCentralConfigDescriptor descriptor, String toDelete) {
        descriptor.removePropertySet(toDelete);
        configService.saveEditedDescriptorAndReload(descriptor);
    }
}
