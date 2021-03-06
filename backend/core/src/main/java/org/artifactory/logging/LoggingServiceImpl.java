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

package org.artifactory.logging;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.logging.version.LoggingVersion;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.update.utils.BackupUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Random;

/**
 * Logging service main implementation
 *
 * @author Noam Y. Tenne
 */
@Service
public class LoggingServiceImpl implements LoggingService {
    private static final Logger log = LoggerFactory.getLogger(LoggingServiceImpl.class);

    Random random = new Random(System.currentTimeMillis());

    @Override
    public void exportTo(ExportSettings settings) {
        // export is handled by the application context (all the etc directory is copied)
    }

    @Override
    public void importFrom(ImportSettings settings) {
        File logFileToImport = new File(settings.getBaseDir(), "etc/logback.xml");
        if (logFileToImport.exists()) {
            try {
                // Backup the target logback file
                File targetEtcDir = ArtifactoryHome.get().getEtcDir();
                File existingLogbackFile = new File(targetEtcDir, "logback.xml");
                if (existingLogbackFile.exists()) {
                    Files.move(existingLogbackFile.toPath(), new File(targetEtcDir, "logback.original.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // Copy file into a temporary working directory
                long randomNumber = random.nextLong();
                File workDir = Files.createTempDirectory("artifactory_"+randomNumber).toFile();
                File workFile = new File(workDir, logFileToImport.getName());
                if (!workDir.exists()) {
                    throw new RuntimeException("Working Directory "+workDir+" doesn't exist");
                }
                if (!logFileToImport.exists()) {
                    throw new RuntimeException("Log file "+logFileToImport+" doesn't exist");
                }
                Files.copy(logFileToImport.toPath(), workFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                //FileUtils.copyFile(logFileToImport, workFile);
                convertAndSave(workFile, settings);
                // Copy the converted file to the target dir
                Files.copy(workFile.toPath(), existingLogbackFile.toPath());
                //FileUtils.copyFileToDirectory(workFile, targetEtcDir);
                FileUtils.deleteQuietly(workFile);
                deleteTempWorkDir(workDir);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                settings.getStatusHolder().error("Failed to import and convert logback file", e, log);
            }
        }
    }

    private void deleteTempWorkDir(File workDir) throws IOException {
        for (File fileTodelete: workDir.listFiles()){
            Files.delete(fileTodelete.toPath());
        }
        Files.delete(workDir.toPath());
    }

    private void convertAndSave(File from, ImportSettings settings) throws IOException {
        CompoundVersionDetails source = ContextHelper.get().getVersionProvider().getOriginalDbVersion();
        //Might be first run, protect
        if (source != null) {
            LoggingVersion.values();
            ArtifactoryVersion importedVersion = BackupUtils.findVersion(settings.getBaseDir());
            LoggingVersion originalVersion = importedVersion.getSubConfigElementVersion(LoggingVersion.class);
            originalVersion.convert(from.getParentFile(), from.getParentFile());
        }
    }
}