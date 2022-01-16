package org.mcphackers.mcp.tasks;

import org.fusesource.jansi.Ansi;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TaskSetup extends Task {

    public TaskSetup(TaskInfo info) {
		super(-1 , info);
	}

	private static Map<Util.OS, String> natives = new HashMap<>();

    static {
        natives.put(Util.OS.windows, "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
        natives.put(Util.OS.macos, "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
        natives.put(Util.OS.linux, "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get(MCPConfig.SRC))) {
            MCP.logger.println(new Ansi().a("Sources folder found! Type \"yes\" if you want to continue setup").fgRgb(255,255,255));
            String confirm = MCP.input.nextLine().toLowerCase();
            MCP.logger.print(new Ansi().fgDefault());
            if(!confirm.equals("yes")) {
            	throw new Exception("Setup aborted");
            }
        }

        MCPConfig.srcCleanup = false;
        new TaskCleanup(info).doTask();
        Util.deleteDirectoryIfExists(Paths.get(MCPConfig.CONF));
        MCP.logger.info(" Setting up your workspace...");
        Util.createDirectories(Paths.get(MCPConfig.JARS));
        Util.createDirectories(Paths.get(MCPConfig.NATIVES));
        
        MCP.logger.info(" Downloading libraries...");
        long startTime = System.currentTimeMillis();
        Util.downloadFile(new URI("https://files.betacraft.uk/launcher/assets/libs-windows.zip").toURL(), Paths.get(MCPConfig.LIB + "libs.zip").toString());
        String nativesURL = natives.get(Util.getOperatingSystem());
        if(nativesURL == null) {
        	throw new Exception("Could not find natives for your operating system");
        }
        Util.downloadFile(new URI(nativesURL).toURL(), Paths.get(MCPConfig.LIB + "natives.zip").toString());
        MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
        Util.unzip(Paths.get(MCPConfig.LIB + "libs.zip"), Paths.get(MCPConfig.LIB), true);
        Util.unzip(Paths.get(MCPConfig.LIB + "natives.zip"), Paths.get(MCPConfig.NATIVES), true);
        
        MCP.logger.info(" Setting up minecraft...");
        String versionsFolder = Files.list(Paths.get("versions")).filter(Files::isDirectory).map(path -> path.getFileName().toString()).filter((fileName) -> !fileName.equals("workspace")).collect(Collectors.joining(","));
        JSONObject json = Util.parseJSONFile(Paths.get("versions", "versions.json"));
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
        String chosenVersion = MCPConfig.setupVersion;
        List<String> versions = Arrays.stream(versionsFolder.toLowerCase().split(",")).collect(Collectors.toList());
        if(!versions.contains(chosenVersion)) {
	        MCP.logger.info(new Ansi().fgMagenta().a("================ ").fgDefault().a("Current versions").fgMagenta().a(" ================").fgDefault().toString());
	        MCP.logger.info(table_str);
	        MCP.logger.info(new Ansi().fgMagenta().a("==================================================").fgDefault().toString());
	        MCP.logger.info(new Ansi().fgYellow().a("If you wish to supply your own configuration, type \"custom\".").fgDefault().toString());
        }
        // Keep asking until they have a valid option
        while (!versions.contains(chosenVersion)) {
            MCP.logger.print(new Ansi().a("Select version: ").fgBrightGreen());
            chosenVersion = MCP.input.nextLine().toLowerCase();
            MCP.logger.print(new Ansi().fgDefault());
        }
        startTime = System.currentTimeMillis();
        MCP.logger.info(" Copying config");
        Util.copyDirectory(Paths.get("versions", chosenVersion), Paths.get(MCPConfig.CONF));
        Properties props = new Properties();
    	props.setProperty("version", chosenVersion);
    	props.store(new BufferedWriter(new FileWriter(Paths.get(MCPConfig.PROPERTIES_CLIENT).toFile())), null);
        props = new Properties();
    	//TODO
        if (chosenVersion.equals("custom")) {
        	props.setProperty("version", chosenVersion);
        }
        else if(json.getJSONObject("client").has(chosenVersion) && json.getJSONObject("client").getJSONObject(chosenVersion).has("server")) {
        	props.setProperty("version", json.getJSONObject("client").getJSONObject(chosenVersion).getString("server"));
        }
        props.store(new BufferedWriter(new FileWriter(Paths.get(MCPConfig.PROPERTIES_SERVER).toFile())), null);
    	
        // Create Eclipse workspace
        MCP.logger.info(" Copying workspace");
        Util.deleteDirectoryIfExists(Paths.get("eclipse"));
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
        MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));

        // Delete Minecraft.jar and Minecraft_server.jar if they exist.
        Files.deleteIfExists(Paths.get(MCPConfig.CLIENT));
        Files.deleteIfExists(Paths.get(MCPConfig.SERVER));

        // Download Minecraft
        startTime = System.currentTimeMillis();
        if (!chosenVersion.equals("custom")) {
            MCP.logger.info(" Downloading Minecraft client...");
            String clientUrl = json.getJSONObject("client").getJSONObject(chosenVersion).getString("url");
            try {
                Util.downloadFile(new URI(clientUrl).toURL(), Paths.get(MCPConfig.CLIENT).toAbsolutePath().toString());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));

            // Download Minecraft Server
            if(json.getJSONObject("client").has(chosenVersion) && json.getJSONObject("client").getJSONObject(chosenVersion).has("server")) {
	            String serverVersion = json.getJSONObject("client").getJSONObject(chosenVersion).getString("server");
	            String serverUrl = json.getJSONObject("server").getJSONObject(serverVersion).getString("url");
	            MCP.logger.info(" Downloading Minecraft server...");
	            startTime = System.currentTimeMillis();
	            if (serverUrl.endsWith(".jar")) {
	                Util.downloadFile(new URI(serverUrl).toURL(), Paths.get(MCPConfig.SERVER).toAbsolutePath().toString());
	            } else if (serverUrl.endsWith(".zip")) {
	                Util.downloadFile(new URI(serverUrl).toURL(), Paths.get("jars/minecraft_server.zip").toAbsolutePath().toString());
	                MCP.logger.info(" Extracting Minecraft server...");
	                Util.unzip(Paths.get(MCPConfig.JARS + "minecraft_server.zip").toAbsolutePath(), Paths.get(MCPConfig.JARS), true);
	                File jarFile = Paths.get(MCPConfig.JARS + "minecraft-server.jar").toFile();
	                jarFile.renameTo(Paths.get(MCPConfig.SERVER).toFile());
	            }
	            MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
            }
        }
    }
}
