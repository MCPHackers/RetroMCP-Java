package org.mcphackers.mcp.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.ResourceManager;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskSetup extends Task {

    public TaskSetup(TaskInfo info) {
		super(-1 , info);
	}

	private static final Map<Util.OS, String> natives = new HashMap<>();
	private static final String libsURL = "https://files.betacraft.uk/launcher/assets/libs-windows.zip";

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
            if(!confirm.toLowerCase().equals("yes")) {
            	info.addInfo("Setup aborted!");
            	return;
            }
        }

        MCP.config.srcCleanup = false;
        new TaskCleanup(info).doTask();
        MCP.logger.info(" Setting up your workspace...");
        FileUtil.createDirectories(Paths.get(MCPConfig.JARS));
        FileUtil.createDirectories(Paths.get(MCPConfig.NATIVES));
        
        MCP.logger.info(" Setting up Minecraft...");
        List<String> versions = VersionsParser.getVersionList();
        versions.sort(Comparator.naturalOrder());
        String chosenVersion = MCP.config.setupVersion;
        if(!versions.contains(chosenVersion)) {
	        MCP.logger.info(new Ansi().fgMagenta().a("================ ").fgDefault().a("Current versions").fgMagenta().a(" ================").fgDefault().toString());
	        MCP.logger.info(getTable(versions));
	        MCP.logger.info(new Ansi().fgMagenta().a("==================================================").fgDefault().toString());
	        MCP.logger.info(new Ansi().fgYellow().a("If you wish to supply your own configuration, type \"custom\".").fgDefault().toString());
        }
        // Keep asking until they have a valid option
        while (!versions.contains(chosenVersion)) {
            MCP.logger.print(new Ansi().a("Select version: ").fgBrightGreen());
            chosenVersion = MCP.input.nextLine().toLowerCase();
            MCP.logger.print(new Ansi().fgDefault());
        }
        
        FileUtil.createDirectories(Paths.get(MCPConfig.CONF));
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(MCPConfig.VERSION))) {
        	writer.write(chosenVersion);
        }
        
        long startTime = System.currentTimeMillis();
        MCP.logger.info(" Downloading mappings");
        FileUtil.downloadFile(VersionsParser.downloadVersion(chosenVersion), Paths.get(MCPConfig.CONF, "conf.zip"));
        FileUtil.unzip(Paths.get(MCPConfig.CONF, "conf.zip"), Paths.get(MCPConfig.CONF), true);
    	
        // Create Eclipse workspace
        MCP.logger.info(" Setting up workspace");
        FileUtil.deleteDirectoryIfExists(Paths.get("eclipse"));
        ResourceManager.copyResource("/eclipse.zip", "eclipse.zip");
        FileUtil.unzip(Paths.get("eclipse.zip"), Paths.get("eclipse"));
        Files.delete(Paths.get("eclipse.zip"));

        // Create Intellij workspace
        String[] projects = { "Client", "Server" };
        for (String project : projects) {
            Path launch = Paths.get("eclipse", ".metadata", ".plugins", "org.eclipse.debug.core", ".launches", project + ".launch");
            if (Files.exists(launch)) {
                List<String> lines = Files.readAllLines(launch);
                String replace = "-Dhttp.proxyPort=%s";
                for (int i = 0; i < lines.size(); i++) {
                    lines.set(i, lines.get(i).replace(replace, String.format(replace, VersionsParser.getProxyPort(chosenVersion))));
                }
                Files.write(launch, lines);
            }
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
        if (!chosenVersion.equals("custom")) {
		    for(int side = 0; side <= (VersionsParser.hasServer(chosenVersion) ? 1 : 0); side++) {
		        startTime = System.currentTimeMillis();
		        MCP.logger.info(" Downloading Minecraft " + (side == CLIENT ? "client" : "server") + "...");
		        String url = VersionsParser.getDownloadURL(chosenVersion, side);
		        // TODO Classic server zips
		        Path pathOut = Paths.get(side == CLIENT ? MCPConfig.CLIENT : MCPConfig.SERVER);
		        FileUtil.downloadFile(new URI(url).toURL(), pathOut);
		        MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
		    }
        }
        
        MCP.logger.info(" Downloading libraries...");
        startTime = System.currentTimeMillis();
        FileUtil.downloadFile(new URI(libsURL).toURL(), Paths.get(MCPConfig.LIB + "libs.zip"));
        String nativesURL = natives.get(Util.getOperatingSystem());
        if(nativesURL == null) {
        	throw new Exception("Could not find natives for your operating system");
        }
        FileUtil.downloadFile(new URI(nativesURL).toURL(), Paths.get(MCPConfig.LIB + "natives.zip"));
        MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
        FileUtil.unzip(Paths.get(MCPConfig.LIB + "libs.zip"), Paths.get(MCPConfig.LIB), true);
        FileUtil.unzip(Paths.get(MCPConfig.LIB + "natives.zip"), Paths.get(MCPConfig.NATIVES), true);
    }

	private static String getTable(List<String> versions) {
        int rows = (int)Math.ceil(versions.size() / 3D);
        List<String>[] tableList = new List[rows];
        for (int i = 0; i < tableList.length; i++)
        {
        	tableList[i] = new ArrayList();
        }
        String table = "";
        int index = 0;
        for (String ver : versions) {
            tableList[index % rows].add(new Ansi().fgBrightCyan().a(" - ").fgDefault().fgCyan().a(String.format("%-12s", ver)).fgDefault().toString());
            index++;
        }
        for (int i = 0; i < tableList.length; i++)
        {
            for (String ver : tableList[i]) {
            	table += ver;
            }
        	if(i < tableList.length - 1) table += "\n";
        }
        return table;
	}
}
