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

package org.artifactory.repo.interceptor.storage;

import org.artifactory.common.MutableStatusHolder;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.exception.CancelException;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.interceptor.Interceptors;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.sapi.interceptor.DeleteContext;
import org.artifactory.sapi.interceptor.StorageInterceptor;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.db.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author yoav
 */
@Service
@Reloadable(beanClass = StorageInterceptors.class, initAfter = DbService.class,
        listenOn = CentralConfigKey.none)
public class StorageInterceptorsImpl extends Interceptors<StorageInterceptor> implements StorageInterceptors {
    private static final Logger log = LoggerFactory.getLogger(StorageInterceptorsImpl.class);

    @Override
    public void beforeCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        try {
            if (!isTrash(fsItem.getRepoPath())) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.beforeCreate(fsItem, statusHolder);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("Before create rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void afterCreate(VfsItem fsItem, MutableStatusHolder statusHolder) {
        try {
            if (!isTrash(fsItem.getRepoPath())) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.afterCreate(fsItem, statusHolder);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("After create rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void beforeDelete(VfsItem fsItem, MutableStatusHolder statusHolder, boolean moved) {
        try {
            if (!isTrash(fsItem.getRepoPath())) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.beforeDelete(fsItem, statusHolder, moved);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("Delete rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void afterDelete(VfsItem fsItem, MutableStatusHolder statusHolder, DeleteContext ctx) {
        if (!isTrash(fsItem.getRepoPath())) {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.afterDelete(fsItem, statusHolder, ctx);
            }
        }
    }

    @Override
    public void beforePropertyCreate(VfsItem fsItem, MutableStatusHolder statusHolder, String name, String... values) {
        try {
            if (!isTrash(fsItem.getRepoPath())) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.beforePropertyCreate(fsItem, statusHolder, name, values);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("Property create rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void afterPropertyCreate(VfsItem fsItem, MutableStatusHolder statusHolder, String name,
            String... values) {
        if (!isTrash(fsItem.getRepoPath())) {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.afterPropertyCreate(fsItem, statusHolder, name, values);
            }
        }
    }

    @Override
    public void beforePropertyDelete(VfsItem fsItem, MutableStatusHolder statusHolder, String name) {
        try {
            if (!isTrash(fsItem.getRepoPath())) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.beforePropertyDelete(fsItem, statusHolder, name);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("Property delete rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void afterPropertyDelete(VfsItem fsItem, MutableStatusHolder statusHolder, String name) {
        if (!isTrash(fsItem.getRepoPath())) {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.afterPropertyDelete(fsItem, statusHolder, name);
            }
        }
    }

    @Override
    public boolean isCopyOrMoveAllowed(VfsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder) {
        if (!isTrash(targetRepoPath)) {
            for (StorageInterceptor storageInterceptor : this) {
                if (!storageInterceptor.isCopyOrMoveAllowed(sourceItem, targetRepoPath, statusHolder)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void beforeMove(VfsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder, Properties properties) {
        try {
            if (!isTrash(sourceItem.getRepoPath())) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.beforeMove(sourceItem, targetRepoPath, statusHolder, properties);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("Move rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void afterMove(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder, Properties properties) {
        for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.afterMove(sourceItem, targetItem, statusHolder, properties);
        }
    }

    @Override
    public void beforeCopy(VfsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder, Properties properties) {
        try {
            if (!isTrash(targetRepoPath)) {
                for (StorageInterceptor storageInterceptor : this) {
                    storageInterceptor.beforeCopy(sourceItem, targetRepoPath, statusHolder, properties);
                }
            }
        } catch (CancelException e) {
            statusHolder.error("Copy rejected: " + e.getMessage(), e.getErrorCode(), e, log);
        }
    }

    @Override
    public void afterCopy(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder, Properties properties) {
        if (!isTrash(targetItem.getRepoPath())) {
            for (StorageInterceptor storageInterceptor : this) {
                storageInterceptor.afterCopy(sourceItem, targetItem, statusHolder, properties);
            }
        }
    }

    private boolean isTrash(RepoPath repoPath) {
        return TrashService.TRASH_KEY.equals(repoPath.getRepoKey());
    }
}