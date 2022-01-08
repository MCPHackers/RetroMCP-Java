package org.mcphackers.mcp.tasks;

import org.fusesource.jansi.Ansi;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tasks.info.TaskInfoCleanup;
import org.mcphackers.mcp.tools.Util;

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

    public TaskSetup(TaskInfo info) {
		super(-1 , info);
	}

	private static Map<String, String> natives = new HashMap<>();

    static {
        natives.put("windows", "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
        natives.put("macosx", "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
        natives.put("linux", "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get("src"))) {
            MCP.logger.println(new Ansi().a("Sources folder found! Type \"yes\" if you want to continue setup").fgRgb(255,255,255));
            String confirm = MCP.input.nextLine().toLowerCase();
            MCP.logger.print(new Ansi().fgDefault());
            if(!confirm.equals("yes")) {
            	throw new Exception("Setup aborted");
            }
        }
        
        MCPConfig.srcCleanup = false;
        new TaskCleanup(new TaskInfoCleanup()).doTask();

        if (Files.exists(Paths.get("conf"))) {
            Util.deleteDirectory(Paths.get("conf"));
        }

        MCP.logger.info(" Setting up your workspace...");
        if (!Files.exists(Util.getPath(MCPConfig.NATIVES))) {
            Files.createDirectories(Util.getPath(MCPConfig.NATIVES));
        }
        MCP.logger.info(" Downloading libraries...");
        long startTime = System.nanoTime();
        Util.downloadFile(new URI("https://files.betacraft.uk/launcher/assets/libs-windows.zip").toURL(), Paths.get("jars", "bin", "libs.zip").toString());

        Util.downloadFile(new URI(natives.get(Util.getOperatingSystem())).toURL(), Paths.get("jars", "bin", "natives.zip").toString());
        long endTime = System.nanoTime();

        long seconds = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        long nanoSeconds = endTime - startTime;

        MCP.logger.info(" Done in " + (seconds == 0 ? nanoSeconds + " ns" : seconds + " s"));
        Util.unzip(Paths.get("jars", "bin", "libs.zip"), Paths.get("jars", "bin"), true);
        Util.unzip(Paths.get("jars", "bin", "natives.zip"), Paths.get("jars", "bin", "natives"), true);
        MCP.logger.info(" Setting up minecraft...");
        setupMC();
    }

    public void setupMC() throws IOException, URISyntaxException {
        String versionsFolder = Files.list(Paths.get("versions")).filter(Files::isDirectory).map(path -> path.getFileName().toString()).filter((fileName) -> !fileName.equals("workspace")).collect(Collectors.joining(","));
        JSONObject json = Util.parseJSONFile(Paths.get("versions", "versions.json"));
        MCP.logger.info(new Ansi().fgMagenta().a("================ ").fgDefault().a("Current versions").fgMagenta().a(" ================").fgDefault().toString());
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
            tableList[row_index].add(new Ansi().fgBrightCyan().a(" - ").fgDefault().fgCyan().a(String.format("%-12s", ver)).fgDefault().toString());
            index++;
        }
        for (int i = 0; i < tableList.length; i++)
        {
            for (String ver : tableList[i]) {
            	table_str += ver;
            }
        	if(i < tableList.length - 1) table_str += "\n";
        }
        MCP.logger.info(table_str);
        MCP.logger.info(new Ansi().fgMagenta().a("==================================================").fgDefault().toString());
        MCP.logger.info(new Ansi().fgYellow().a("If you wish to supply your own configuration, type \"custom\".").fgDefault().toString());
        String chosenVersion = null;
        // Keep asking until they have a valid option
        List<String> versions = Arrays.stream(versionsFolder.toLowerCase().split(",")).collect(Collectors.toList());
        while (!versions.contains(chosenVersion)) {
            MCP.logger.print(new Ansi().a("Select version: ").fgBrightGreen());
            chosenVersion = MCP.input.nextLine().toLowerCase();
            MCP.logger.print(new Ansi().fgDefault());
        }
        long startCopyTime = System.nanoTime();
        MCP.logger.info(" Copying config");
        Util.copyDirectory(Paths.get("versions", chosenVersion), Paths.get("conf"));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("conf", "version.cfg"))) {
            writer.write("ClientVersion = " + chosenVersion + "\n");
            if (chosenVersion.equals("custom")) {
                writer.write("ServerVersion = " + chosenVersion + "\n");
            }
            else if(json.getJSONObject("client").has(chosenVersion) && json.getJSONObject("client").getJSONObject(chosenVersion).has("server")) {
                writer.write("ServerVersion = " + json.getJSONObject("client").getJSONObject(chosenVersion).getString("server") + "\n");
            }
        }
        // Create Eclipse workspace
        MCP.logger.info(" Copying workspace");

        if (Files.exists(Paths.get("eclipse"))) {
            Util.deleteDirectory(Paths.get("eclipse"));
        }
        int workspaceVersion = json.getJSONObject("client").getJSONObject(chosenVersion).getInt("workspace_version");
        Util.copyDirectory(Paths.get("versions", "workspace", "eclipse_" + workspaceVersion), Paths.get("eclipse"));

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
        MCP.logger.info(" Done in " + (seconds == 0 ? nanoSeconds + " ns" : seconds + " s"));

        // Delete Minecraft.jar and Minecraft_server.jar if they exist.
        Files.deleteIfExists(Util.getPath(MCPConfig.CLIENT));
        Files.deleteIfExists(Util.getPath(MCPConfig.SERVER));

        // Download Minecraft
        long startClientDownloadTime = System.nanoTime();
        if (!chosenVersion.equals("custom")) {
            MCP.logger.info(" Downloading Minecraft client...");
            String clientUrl = json.getJSONObject("client").getJSONObject(chosenVersion).getString("url");
            try {
                Util.downloadFile(new URI(clientUrl).toURL(), Util.getPath(MCPConfig.CLIENT).toAbsolutePath().toString());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            long downloadNanoTime = System.nanoTime() - startClientDownloadTime;
            long downloadSeconds = TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS);
            MCP.logger.info(" Done in " + (downloadSeconds == 0 ? downloadNanoTime + " ns" : downloadSeconds + " s"));

            // Download Minecraft Server
            if(json.getJSONObject("client").has(chosenVersion) && json.getJSONObject("client").getJSONObject(chosenVersion).has("server")) {
	            String serverVersion = json.getJSONObject("client").getJSONObject(chosenVersion).getString("server");
	            String serverUrl = json.getJSONObject("server").getJSONObject(serverVersion).getString("url");
	            MCP.logger.info(" Downloading Minecraft server...");
	            long startServerDownloadTime = System.nanoTime();
	            if (serverUrl.endsWith(".jar")) {
	                Util.downloadFile(new URI(serverUrl).toURL(), Util.getPath(MCPConfig.SERVER).toAbsolutePath().toString());
	            } else if (serverUrl.endsWith(".zip")) {
	                Util.downloadFile(new URI(serverUrl).toURL(), Util.getPath(MCPConfig.SERVER_ZIP).toAbsolutePath().toString());
	                MCP.logger.info(" Extracting Minecraft server...");
	                Util.unzip(Util.getPath(MCPConfig.SERVER_ZIP).toAbsolutePath(), Paths.get("jars"), true);
	                File jarFile = Paths.get("jars", "minecraft-server.jar").toFile();
	                jarFile.renameTo(Util.getPath(MCPConfig.SERVER).toFile());
	            }
	            downloadNanoTime = System.nanoTime() - startServerDownloadTime;
	            downloadSeconds = TimeUnit.SECONDS.convert(downloadNanoTime, TimeUnit.NANOSECONDS);
	            MCP.logger.info(" Done in " + (downloadSeconds == 0 ? downloadNanoTime + " ns" : downloadSeconds + " s"));
            }
        }
    }
}
