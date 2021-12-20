package org.mcphackers.mcp.tasks;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.Utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskSetup extends Task {

    private static Map<String, String> natives = new HashMap<>();

    static {
        natives.put("windows", "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
        natives.put("macosx", "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
        natives.put("linux", "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get("src"))) {
            MCP.logger.info("! /src exists! Aborting.");
            MCP.logger.info("! Run cleanup in order to run setup again.");
            throw new IOException();
        }

        MCP.logger.info("> Setting up your workspace...");
        if (!Files.exists(Paths.get("temp"))) {
            Files.createDirectory(Paths.get("temp"));
        }
        if (!Files.exists(Paths.get("jars", "bin", "natives"))) {
            Files.createDirectories(Paths.get("jars", "bin", "natives"));
        }
        step = 0;
        MCP.logger.info("> Downloading libraries...");
        long startTime = System.nanoTime();
        Utility.downloadFile(new URI("https://files.betacraft.uk/launcher/assets/libs-windows.zip").toURL(), Paths.get("jars", "bin", "libs.zip").toString());

        step = 1;
        Utility.downloadFile(new URI(natives.get(Utility.getOperatingSystem())).toURL(), Paths.get("jars", "bin", "natives.zip").toString());
        long endTime = System.nanoTime();

        long seconds = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        long nanoSeconds = endTime - startTime;

        MCP.logger.info("> Done in " + (seconds == 0 ? nanoSeconds + " ns" : seconds + " s"));
        step = 2;
        try {
            Utility.unzip(Paths.get("jars", "bin", "libs.zip"), Paths.get("jars", "bin"));
            Utility.unzip(Paths.get("jars", "bin", "natives.zip"), Paths.get("jars", "bin", "natives"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        MCP.logger.info("> Setting up minecraft...");
        setupMC();
    }

    public void setupMC() throws IOException {
        MCP.logger.info("> If you wish to supply your own configuration, type \"custom\".");
        String versionsFolder = Files.list(Paths.get("versions")).filter(Files::isDirectory).map(path -> path.getFileName().toString()).filter((fileName) -> !fileName.equals("workspace")).collect(Collectors.joining(","));
        JSONObject json = Utility.parseJSONFile(Paths.get("versions", "versions.json"));
        MCP.logger.info("Current versions are:");
        List<String> verList = new ArrayList<String>();
        for (String versionFolder : versionsFolder.split(",")) {
        	verList.add(versionFolder);
        }
        verList.sort(Comparator.naturalOrder());
        int rows = (int)Math.ceil(verList.size() / 3D);
        List<String>[] tableList = new List[rows];
        for (int i = 0; i < tableList.length; i++)
        {
        	tableList[i] = new ArrayList();
        }
        String table_str = "";
        int index = 0;
        for (String ver : verList) {
            int row_index = index % rows;
            tableList[row_index].add(" - " + String.format("%-12s", ver));
            index++;
        }
        for (int i = 0; i < tableList.length; i++)
        {
            for (String ver : tableList[i]) {
            	table_str += ver;
            }
        	table_str += "\n";
        }
        MCP.logger.info(table_str);
        MCP.logger.info("> What version would you like to install?");
        MCP.logger.print(": ");
        String chosenVersion = MCP.input.nextLine().toLowerCase();
        // Keep asking until they have a valid option
        List<String> versions = Arrays.stream(versionsFolder.toLowerCase().split(",")).collect(Collectors.toList());
        while (!versions.contains(chosenVersion)) {
            MCP.logger.print(": ");
            chosenVersion = MCP.input.nextLine().toLowerCase();
        }

        if (Files.exists(Paths.get("conf"))) {
            Utility.deleteDirectoryStream(Paths.get("patches_client"));
            Utility.deleteDirectoryStream(Paths.get("patches_server"));
        }
        long startCopyTime = System.nanoTime();
        MCP.logger.info("> Copying config");
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("conf", "version.cfg"))) {
            writer.write("ClientVersion = " + chosenVersion + "\n");
            if (chosenVersion.equals("custom")) {
                writer.write("ServerVersion = " + chosenVersion + "\n");
            } // TODO: Fix this to get server version from JSON else if
        }

        if (Files.exists(Paths.get("eclipse"))) {
            Utility.deleteDirectoryStream(Paths.get("eclipse"));
        }
        // Create Eclipse workspace
        MCP.logger.info("> Copying workspace");
        int workspaceVersion = json.getJSONObject("client").getJSONObject(chosenVersion).getInt("workspace_version");
        Utility.copyDirectory(Paths.get("versions", "workspace", "eclipse_" + workspaceVersion), Paths.get("eclipse"));

        // Create Intellij workspace
        String[] projects = { "Client", "Server" };
        for (String project : projects) {
            Path imlPath = Paths.get("eclipse", project, project + ".iml");
            if (Files.exists(imlPath)) {
                List<String> lines = Files.readAllLines(imlPath);
                for (int i = 0; i < lines.size(); i++) {
                    lines.set(i, lines.get(i).replace("$MCP_LOC$", Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString().replace("\\", "/")));
                }
                Files.write(imlPath, lines);
            }
        }

        long nanoSeconds = System.nanoTime() - startCopyTime;
        long seconds = TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS);
        MCP.logger.info("> Done in " + (seconds == 0 ? nanoSeconds + " ns" : seconds + " s"));

        // Delete Minecraft.jar and Minecraft_server.jar if they exist.
        Files.deleteIfExists(Paths.get("jars", "minecraft.jar"));
        Files.deleteIfExists(Paths.get("jars", "minecraft_server.jar"));

        // Download Minecraft
        long startClientDownloadTime = System.nanoTime();
        if (!chosenVersion.equals("custom")) {
            MCP.logger.info("> Downloading Minecraft client...");
            String clientUrl = json.getJSONObject("client").getJSONObject(chosenVersion).getString("url");
            try {
                Utility.downloadFile(new URI(clientUrl).toURL(), Paths.get("jars", "bin", "minecraft.jar").toAbsolutePath().toString());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            long downloadNanoTime = System.nanoTime() - startClientDownloadTime;
            long downloadSeconds = TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS);
            MCP.logger.info("> Done in " + (downloadSeconds == 0 ? downloadNanoTime + " ns" : downloadSeconds + " s"));

            // Download Minecraft Server
            try {
                String serverVersion = json.getJSONObject("client").getJSONObject(chosenVersion).getString("server");
                String serverUrl = json.getJSONObject("server").getJSONObject(serverVersion).getString("url");
                try {
                    MCP.logger.info("> Downloading Minecraft server...");
                    long startServerDownloadTime = System.nanoTime();
                    if (serverUrl.endsWith(".jar")) {
                        Utility.downloadFile(new URI(serverUrl).toURL(), Paths.get("jars", "minecraft_server.jar").toAbsolutePath().toString());
                    } else if (serverUrl.endsWith(".zip")) {
                        Utility.downloadFile(new URI(serverUrl).toURL(), Paths.get("jars", "minecraft_server.zip").toAbsolutePath().toString());
                        MCP.logger.info("> Extracting Minecraft server...");
                        Utility.unzip(Paths.get("jars", "minecraft_server.zip").toAbsolutePath(), Paths.get("jars"));
                        Files.delete(Paths.get("jars", "minecraft_server.zip"));
                        File jarFile = Paths.get("jars", "minecraft-server.jar").toFile();
                        jarFile.renameTo(Paths.get("jars", "minecraft_server.jar").toFile());
                    }
                    downloadNanoTime = System.nanoTime() - startServerDownloadTime;
                    downloadSeconds = TimeUnit.SECONDS.convert(downloadNanoTime, TimeUnit.NANOSECONDS);
                    MCP.logger.info("> Done in " + (downloadSeconds == 0 ? downloadNanoTime + " ns" : downloadSeconds + " s"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    // TODO: Let the doTask throw this or only print exception in debug mode
                }
            } catch (JSONException ex) {
            	//MCP.logger.error("Server not found for " + chosenVersion);
            }
        }
    }
}
