package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

public class TaskSetup implements Task {

    private int step = 0;

    private boolean downloadingLibraries = false;
    private boolean downloadingNatives = false;
    private boolean extracting = false;

    private Map<String, String> natives = new HashMap<>();

    public TaskSetup() {
        natives.put("windows", "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
        natives.put("macosx", "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
        natives.put("linux", "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get("src"))) {
            System.out.println("! /src exists! Aborting.");
            System.out.println("! Run cleanup in order to run setup again.");
            System.exit(-1);
        }

        System.out.println("> Setting up your workspace...");
        System.out.println("> Making sure temp exists...");

        if (!Files.exists(Paths.get("temp"))) {
            Files.createDirectory(Paths.get("temp"));
        }

        System.out.println("> Making sure jars/bin/natives exists.");
        if (!Files.exists(Paths.get("jars", "bin", "natives"))) {
            Files.createDirectories(Paths.get("jars", "bin", "natives"));
        }

        downloadingLibraries = true;
        step = 0;
        long startTime = System.currentTimeMillis();
        Utility.downloadFile(new URI("https://files.betacraft.uk/launcher/assets/libs-windows.zip").toURL(), Paths.get("jars", "bin", "libs.zip").toString());
        downloadingLibraries = false;

        downloadingNatives = true;
        step = 0;
        Utility.downloadFile(new URI(natives.get(Utility.getOperatingSystem())).toURL(), Paths.get("jars", "bin", "natives.zip").toString());
        downloadingNatives = false;
        long endTime = System.currentTimeMillis();

        long seconds = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        long nanoSeconds = endTime - startTime;

        System.out.println("> Done in " + (seconds == 0 ? nanoSeconds + " nano seconds" : seconds + " seconds"));
        extracting = true;
        try {
            Utility.unzip(Files.newInputStream(Paths.get("jars", "bin", "libs.zip")), Paths.get("jars", "bin"));
            Utility.unzip(Files.newInputStream(Paths.get("jars", "bin", "natives.zip")), Paths.get("jars", "bin", "natives"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ProgressInfo getProgress() {
        if (downloadingLibraries) {
            return new ProgressInfo("> Downloading libraries...", step, 1);
        } else if (downloadingNatives) {
            return new ProgressInfo("> Downloading natives for your platform...", step, 1);
        } else if (extracting) {
            return new ProgressInfo("> Extracting...", step, 1);
        } else {
            return null;
        }
    }
}
