package com.github.grishberg.androidemulatormanager.avdmanager;

import com.github.grishberg.androidemulatormanager.EmulatorConfig;
import org.apache.commons.io.IOUtils;
import org.gradle.api.logging.Logger;

import java.io.*;
import java.util.*;

/**
 * Creates config init.
 */
public class HardwareManager {
    private static final String CONFIG_FILE_NAME = "config.ini";
    private final Logger logger;
    private final File avdHomeDir;

    public HardwareManager(File avdHomeDir, Logger logger) {
        this.logger = logger;
        this.avdHomeDir = avdHomeDir;
    }

    public void writeHardwareFile(EmulatorConfig config) {
        String configName = new File(avdHomeDir,
                String.format(Locale.US, "%s.avd/config.ini", config.getName()))
                .getAbsolutePath();
        HashMap<String, String> defaultParams = readDefaultConfig(configName);

        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream(configName);

            for (Map.Entry<String, String> entry : defaultParams.entrySet()) {
                prop.setProperty(entry.getKey(), entry.getValue());
            }
            // set the properties value
            prop.setProperty("hw.lcd.density", String.valueOf(config.getDisplayMode().getDensity()));
            prop.setProperty("hw.lcd.height", String.valueOf(config.getDisplayMode().getHeight()));
            prop.setProperty("hw.lcd.width", String.valueOf(config.getDisplayMode().getWidth()));
            prop.setProperty("skin.name", String.format("%dx%d",
                    config.getDisplayMode().getWidth(),
                    config.getDisplayMode().getHeight()));

            // save properties to project root folder
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private HashMap<String, String> readDefaultConfig(String fileName) {

        HashMap<String, String> params = new HashMap<>();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileName);
            Properties prop = new Properties();
            String propFileName = "config.ini";

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
            HashSet<String> argNames = new HashSet<>();
            argNames.add("PlayStore.enabled");
            argNames.add("abi.type");
            argNames.add("avd.ini.encoding");
            argNames.add("hw.cpu.arch");
            argNames.add("image.sysdir.1");
            argNames.add("tag.display");
            argNames.add("tag.id");

            // get the property value and print it out
            for (String key : argNames) {
                params.put(key, prop.getProperty(key));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return params;
    }
}
