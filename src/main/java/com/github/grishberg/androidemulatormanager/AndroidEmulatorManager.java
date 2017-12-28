package com.github.grishberg.androidemulatormanager;

import com.android.ddmlib.IDevice;
import com.github.grishberg.androidemulatormanager.avdmanager.AvdManagerException;
import com.github.grishberg.androidemulatormanager.avdmanager.AvdManagerFabric;
import com.github.grishberg.androidemulatormanager.avdmanager.AvdManagerWrapper;
import com.github.grishberg.androidemulatormanager.emulatormanager.EmulatorManagerException;
import com.github.grishberg.androidemulatormanager.emulatormanager.EmulatorManagerFabric;
import com.github.grishberg.androidemulatormanager.emulatormanager.EmulatorManagerWrapper;
import com.github.grishberg.androidemulatormanager.utils.SysUtils;
import org.gradle.api.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * Facade for creating, deleting and starting emulators.
 */
public class AndroidEmulatorManager {
    private static final int TIMEOUT_FOR_CYCLE = 5;
    private static final String STOPPED = "stopped";
    private static final String PROPERTY_BOOTANIM = "init.svc.bootanim";
    public static final int TRIES_COUNT = 3;
    private final EmulatorManagerWrapper emulatorManager;
    private final AdbFacade adbFacade;
    private final AvdManagerWrapper avdManager;
    private final Map<EmulatorConfig, AndroidEmulator> startedEmulators = new HashMap<>();
    private final Logger logger;

    public AndroidEmulatorManager(PreferenceContext context,
                                  AdbFacade adbFacade,
                                  EmulatorManagerFabric emulatorManagerFabric,
                                  AvdManagerFabric avdManagerFabric,
                                  Logger logger) {
        this.logger = logger;
        emulatorManager = emulatorManagerFabric.createEmulatorManagerForOs(context);
        avdManager = avdManagerFabric.createAvdManagerForOs();
        this.adbFacade = adbFacade;
    }

    /**
     * Creates emulators with parameters.
     *
     * @param args parameters for creating AVDs.
     * @throws InterruptedException
     * @throws AvdManagerException
     */
    public void createEmulators(EmulatorConfig[] args) throws InterruptedException,
            AvdManagerException {
        for (EmulatorConfig arg : args) {
            if (!SysUtils.getAvdConfig(arg.getName()).exists()) {
                logger.info("createEmulators {}", arg.getName());
                avdManager.createAvd(arg);
            } else {
                logger.info("createEmulators {}: emulator exists.", arg.getName());
            }
        }
    }

    /**
     * Deletes emulators with name arg.getName()
     *
     * @param args array of parameters, contains AVD names.
     * @throws AvdManagerException
     */
    public void deleteEmulators(EmulatorConfig[] args) throws AvdManagerException {
        for (EmulatorConfig arg : args) {
            logger.info("deleteEmulators {}", arg.getName());
            avdManager.deleteAvd(arg);
        }
    }

    /**
     * Starts emulators with names arg.getName().
     *
     * @param args args that emulators name.
     * @throws EmulatorManagerException
     */
    public AndroidEmulator[] startEmulators(EmulatorConfig[] args) throws EmulatorManagerException {
        for (EmulatorConfig arg : args) {
            logger.info("startEmulators {}", arg.getName());
            startedEmulators.put(arg, emulatorManager.startEmulator(arg));
        }

        return startedEmulators.values().toArray(new AndroidEmulator[startedEmulators.size()]);
    }

    /**
     * Stops launched emulators.
     */
    public void stopRunningEmulators() throws InterruptedException {
        logger.info("stopRunningEmulators");
        CountDownLatch countDownLatch = new CountDownLatch(startedEmulators.size());

        for (final Map.Entry<EmulatorConfig, AndroidEmulator> entry : startedEmulators.entrySet()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        entry.getValue().stopEmulator();
                    } catch (InterruptedException e) {
                        logger.error("stopRunningEmulators {}", entry.getValue().getAvdName(), e);
                        Thread.currentThread().interrupt();
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            }).start();
        }
        countDownLatch.await();

        startedEmulators.clear();
    }

    /**
     * Waits until all emulators are launched.
     *
     * @param args    emulator args
     * @param timeout timeout for waiting emulators in milliseconds.
     */
    public void waitForEmulatorStarts(EmulatorConfig[] args, long timeout)
            throws InterruptedException, AvdTimeoutException, EmulatorManagerException {
        for (int i = 0; i < TRIES_COUNT; i++) {
            try {
                checkAllEmulatorsAreOnline(args, timeout);
                return;
            } catch (AvdTimeoutException e) {
                logger.error("WaitForEmulatorStarts: timeout exception", e);
                stopRunningEmulatorsForcibly();
                adbFacade.terminate();
                adbFacade.init();
                startEmulators(args);
            }
        }
    }

    private void checkAllEmulatorsAreOnline(EmulatorConfig[] args, long timeout) throws InterruptedException, AvdTimeoutException {
        boolean allEmulatorsAreOnline = false;
        HashSet<String> onlineDevices = new HashSet<>();
        long timeoutTime = System.currentTimeMillis() + timeout;

        while (!allEmulatorsAreOnline && System.currentTimeMillis() < timeoutTime) {
            Thread.sleep(1000L * TIMEOUT_FOR_CYCLE);
            for (EmulatorConfig arg : args) {
                IDevice device = findOnlineDeviceForConfig(arg);
                if (device != null) {
                    logger.info("found online emulator: {}", arg.getName());
                    onlineDevices.add(arg.getName());
                    updateAndroidEmulatorWithDevice(arg, device);
                }
            }
            allEmulatorsAreOnline = onlineDevices.size() == args.length;
        }
        if (!allEmulatorsAreOnline) {

            throw new AvdTimeoutException(String.format("Not all emulators online: %d of %d",
                    onlineDevices.size(), args.length));
        }
    }

    private void stopRunningEmulatorsForcibly() {
        logger.info("stopRunningEmulatorsForcibly");

        for (final Map.Entry<EmulatorConfig, AndroidEmulator> entry : startedEmulators.entrySet()) {
            entry.getValue().stopEmulatorForcibly();

            startedEmulators.clear();
        }
    }

    private void updateAndroidEmulatorWithDevice(EmulatorConfig arg, IDevice device) {
        AndroidEmulator currentEmulator = startedEmulators.get(arg);
        if (currentEmulator != null) {
            currentEmulator.setConnectedDevice(new AvdStopperImpl(device, logger));
        }
    }

    private IDevice findOnlineDeviceForConfig(EmulatorConfig arg) throws InterruptedException {
        IDevice[] devices = adbFacade.getDevices();
        for (IDevice device : devices) {
            logger.info("findOnlineDeviceForConfig: current device = {}, arg = {}",
                    device.getName(), arg.getName());
            if (arg.getName().equals(device.getAvdName()) && device.isOnline()
                    && isDeviceReady(device)) {
                return device;
            }
        }
        return null;
    }

    private boolean isDeviceReady(IDevice device) throws InterruptedException {
        try {
            return STOPPED.equals(device.getSystemProperty(PROPERTY_BOOTANIM).get());
        } catch (ExecutionException e) {
            logger.error("GetSystemProperty exception:", e);
            return false;
        }
    }
}
