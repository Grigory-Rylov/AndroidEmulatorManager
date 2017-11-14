package com.github.grishberg.avdmanager.avdmanager;

import com.github.grishberg.avdmanager.PreferenceContext;
import com.github.grishberg.avdmanager.utils.AbsProvider;
import org.gradle.api.logging.Logger;

/**
 * Creates AvdManagerWrapper for current OS.
 */
public class AvdManagerFabric extends AbsProvider {
    public AvdManagerWrapper createAvdManagerForOs(PreferenceContext context, Logger logger) {

        if (isWindows()) {
            throw new NoSuchMethodError();
        } else if (isMac()) {
            return new UnixAvdManagerWrapper(context, logger);
        } else if (isUnix()) {
            return new UnixAvdManagerWrapper(context, logger);
        } else if (isSolaris()) {
            throw new NoSuchMethodError();
        } else {
            throw new NoSuchMethodError("Your OS is not support!!");
        }
    }
}
