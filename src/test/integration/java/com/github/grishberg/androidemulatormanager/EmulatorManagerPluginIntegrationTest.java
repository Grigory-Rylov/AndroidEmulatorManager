package com.github.grishberg.androidemulatormanager;

import com.github.grishberg.androidemulatormanager.ext.EmulatorManagerConfig;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.Project;
import org.junit.Test;

/**
 * Created by grishberg on 19.11.17.
 */
public class EmulatorManagerPluginIntegrationTest extends BaseTestCaseWithLogger {
    private Project project;
    private EmulatorConfig argPhone = new EmulatorConfig("test_phone",
            DisplayMode.getPhoneHdpi(), 26);
    private EmulatorConfig argTablet = new EmulatorConfig("test_tablet",
            DisplayMode.getTabletXhdpi(), 26);

    public EmulatorManagerPluginIntegrationTest() {
        super();
        project = getProject();
        project.getPluginManager().apply(EmulatorManagerPlugin.class);

        EmulatorManagerConfig ext = project.getExtensions().findByType(EmulatorManagerConfig.class);
        NamedDomainObjectCollection<EmulatorConfig> emulatorConfigs =
                (NamedDomainObjectCollection<EmulatorConfig>) project.getExtensions()
                        .findByName(EmulatorManagerPlugin.EMULATOR_CONFIGS);

        emulatorConfigs.add(argPhone);
        emulatorConfigs.add(argTablet);
        ext.setWaitingTimeout(60 * 1000);
    }

    @Test
    public void executeTasks() {

        CreateEmulatorsTask createTask = (CreateEmulatorsTask) project.getTasks()
                .getByName("createEmulators");

        StartEmulatorsTask startEmulatorsTask = (StartEmulatorsTask) project.getTasks()
                .getByName("startEmulators");

        WaitForEmulatorsTask waitForEmulatorsTask = (WaitForEmulatorsTask) project.getTasks()
                .getByName(WaitForEmulatorsTask.NAME);

        StopEmulatorsTask stopEmulatorsTask = (StopEmulatorsTask) project.getTasks()
                .getByName(StopEmulatorsTask.NAME);

        DeleteEmulatorsTask deleteTasks = (DeleteEmulatorsTask) project.getTasks()
                .getByName(DeleteEmulatorsTask.NAME);

        createTask.runTask();
        startEmulatorsTask.runTask();
        waitForEmulatorsTask.runTask();
        stopEmulatorsTask.runTask();
        deleteTasks.runTask();
    }
}
