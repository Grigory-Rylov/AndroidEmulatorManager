package com.github.grishberg.androidemulatormanager

import com.github.grishberg.androidemulatormanager.ext.EmulatorManagerConfig
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Created by grishberg on 19.11.17.
 */
class CreateEmulatorsTask extends DefaultTask {
    public static final String NAME = "createEmulators"
    AndroidEmulatorManager emulatorManager
    EmulatorManagerConfig extConfig

    @TaskAction
    void runTask() {
        if (extConfig.emulatorArgs == null) {
            throw new GradleException("Need to setup EmulatorManagerConfig extension object")
        }
        emulatorManager.initIfNeeded()

        emulatorManager.createEmulators(extConfig.emulatorArgs)
    }
}
