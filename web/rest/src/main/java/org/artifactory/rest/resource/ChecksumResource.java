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

package org.artifactory.rest.resource;
import org.artifactory.rest.common.model.artifact.BaseArtifact;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.services.ConfigServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@RolesAllowed({AuthorizationService.ROLE_USER,AuthorizationService.ROLE_ADMIN})
@Component
@Path("checksum/sha256")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ChecksumResource extends BaseResource{

    @Autowired
    protected ConfigServiceFactory configServiceFactory;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSha256Property(BaseArtifact baseArtifact) throws Exception {
        return runService(configServiceFactory.addSha256ToArtifact(), baseArtifact);
    }
}
