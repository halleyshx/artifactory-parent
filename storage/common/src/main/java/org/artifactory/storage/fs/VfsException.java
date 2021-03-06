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

package org.artifactory.storage.fs;

import org.artifactory.storage.StorageException;

/**
 * Signals an error with a virtual file system operation.
 *
 * @author Yossi Shaul
 */
public class VfsException extends StorageException {
    public VfsException(String message) {
        super(message);
    }

    public VfsException(String message, Throwable cause) {
        super(message, cause);
    }

    public VfsException(Throwable cause) {
        super(cause);
    }
}
