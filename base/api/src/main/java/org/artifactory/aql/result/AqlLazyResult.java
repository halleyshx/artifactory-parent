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

package org.artifactory.aql.result;

import org.artifactory.aql.action.AqlAction;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.aql.model.AqlRepoProvider;
import org.artifactory.aql.model.DomainSensitiveField;

import java.sql.ResultSet;
import java.util.List;


/**
 * @author Gidi Shabat
 */
public interface AqlLazyResult extends AutoCloseable {

    AqlPermissionProvider getPermissionProvider();

    AqlRepoProvider getRepoProvider();

    List<DomainSensitiveField> getFields();

    ResultSet getResultSet();

    long getLimit();

    long getOffset();

    AqlDomainEnum getDomain();

    AqlAction getAction();
}
