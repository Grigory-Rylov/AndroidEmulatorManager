package com.github.grishberg.androidemulatormanager.avdmanager;

/**
 * Exceptions for AvdManager.
 */
public class AvdManagerException extends Exception {
    public AvdManagerException(String message, Throwable e) {
        super(message, e);
    }

    public AvdManagerException(String errorString) {
        super(errorString);
    }
}
