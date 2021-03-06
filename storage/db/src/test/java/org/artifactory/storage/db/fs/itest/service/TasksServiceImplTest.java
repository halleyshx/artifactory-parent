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

package org.artifactory.storage.db.fs.itest.service;

import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.fs.service.TasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests the {@link org.artifactory.storage.fs.service.TasksService}
 *
 * @author Yossi Shaul
 */
@Test
public class TasksServiceImplTest extends DbBaseTest {

    @Autowired
    private TasksService tasksService;

    @BeforeClass
    public void setup() {
        importSql("/sql/nodes-for-service.sql");
    }

    public void getIndexTasks() {
        List<RepoPath> tasks = tasksService.getIndexTasks();
        assertNotNull(tasks);
        assertEquals(tasks.size(), 2);
        assertTrue(tasks.contains(InternalRepoPathFactory.createRepoPath("repo1:ant/ant/1.5/ant-1.5.jar")));
        assertTrue(tasks.contains(InternalRepoPathFactory.createRepoPath("reponone:ant/ant/1.5/ant-1.5.jar")));
    }

    public void getOrderedTasks() {
        List<String> tasks = tasksService.getXrayEventTasks();
        assertNotNull(tasks);
        assertEquals(tasks.size(), 2);
        assertEquals(tasks.get(0), "xray-event-1");
        assertEquals(tasks.get(1), "xray-event-2");
    }

    public void hasIndexTask() {
        assertTrue(tasksService.hasIndexTask(InternalRepoPathFactory.createRepoPath("repo1:ant/ant/1.5/ant-1.5.jar")));
    }

    @Test(dependsOnMethods = "getIndexTasks")
    public void addIndexTask() {
        RepoPath repoPath = InternalRepoPathFactory.createRepoPath("repo2:test");
        assertFalse(tasksService.hasIndexTask(repoPath));
        tasksService.addIndexTask(repoPath);
        assertTrue(tasksService.hasIndexTask(repoPath));
    }

    @Test(dependsOnMethods = "addIndexTask")
    public void removeIndexTask() {
        assertTrue(tasksService.removeIndexTask(InternalRepoPathFactory.createRepoPath("repo2:test")));
        assertFalse(tasksService.hasIndexTask(InternalRepoPathFactory.createRepoPath("repo2:test")));
    }

    @Test(dependsOnMethods = "hasIndexTask")
    public void removeIndexTaskByRepoPath() {
        assertTrue(
                tasksService.removeIndexTask(InternalRepoPathFactory.createRepoPath("repo1:ant/ant/1.5/ant-1.5.jar")));
    }

    public void removeIndexTaskByRepoPathNotExist() {
        assertFalse(tasksService.removeIndexTask(InternalRepoPathFactory.createRepoPath("nosuch:path.txt")));
    }

    public void getXrayEventTasks() {
        List<String> events = tasksService.getXrayEventTasks();
        assertNotNull(events);
        assertEquals(events.size(), 2);
        assertTrue(events.contains("xray-event-1"));
        assertTrue(events.contains("xray-event-2"));
    }

    public void hasXrayEventTask() {
        assertTrue(tasksService.hasXrayEventTask("xray-event-1"));
        assertFalse(tasksService.hasXrayEventTask("xray-event-999"));
    }

    @Test(dependsOnMethods = "getXrayEventTasks")
    public void addXrayEventTask() {
        tasksService.addXrayEventTask("xray-event-3");
        assertTrue(tasksService.hasXrayEventTask("xray-event-3"));
    }

    @Test(dependsOnMethods = "addXrayEventTask")
    public void removeXrayEventTask() {
        assertTrue(tasksService.removeXrayEventTask("xray-event-3"));
        assertFalse(tasksService.hasXrayEventTask("xray-event-3"));
    }

    public void removeXrayEventByKeyNotExist() {
        assertFalse(tasksService.removeXrayEventTask("nosuchevent"));
    }
}
