package com.github.grishberg.androidemulatormanager

import com.github.grishberg.androidemulatormanager.avdmanager.AvdManagerFabric
import com.github.grishberg.androidemulatormanager.avdmanager.HardwareManager
import com.github.grishberg.androidemulatormanager.emulatormanager.EmulatorManagerFabric
import com.github.grishberg.androidemulatormanager.utils.SysUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class EmulatorManagerPlugin implements Plugin<Project> {
    private AndroidEmulatorManager androidEmulatorManager

    @Override
    void apply(Project project) {
        def config = project.extensions.create('emulatorManagerConfig', EmulatorManagerConfig, project)

        PreferenceContext context = new PreferenceContext()
        final AdbFacade adbFacade = new AdbFacade(project.logger)
        EmulatorManagerFabric emulatorManagerFabric = new EmulatorManagerFabric(project.logger)
        HardwareManager hardwareManager = new HardwareManager(SysUtils.getAvdHomeDir(), project.logger)
        AvdManagerFabric avdManagerFabric = new AvdManagerFabric(context, hardwareManager, project.logger)
        androidEmulatorManager = new AndroidEmulatorManager(context, adbFacade,
                emulatorManagerFabric, avdManagerFabric, project.logger)

        adbFacade.init()

        project.tasks.create('createEmulators', CreateEmulatorsTask) {
            emulatorManager = androidEmulatorManager
            extConfig = config
        }

        project.tasks.create('startEmulators', StartEmulatorsTask) {
            emulatorManager = androidEmulatorManager
            extConfig = config
        }

        project.tasks.create('waitForEmulators', WaitForEmulatorsTask) {
            emulatorManager = androidEmulatorManager
            extConfig = config
        }

        project.tasks.create('stopRunningEmulators', StopEmulatorsTask) {
            emulatorManager = androidEmulatorManager
        }

        project.tasks.create('deleteEmulators', DeleteEmulatorsTask) {
            emulatorManager = androidEmulatorManager
            extConfig = config
        }
    }
}
