package org.mcphackers.mcp.tasks;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.MCPPaths;
import org.mcphackers.mcp.tools.Os;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskSetup extends Task {

	public TaskSetup(MCP instance) {
		super(Side.ANY, instance);
	}

	private static final Map<Os, String> natives = new HashMap<>();
	private static final String libsURL = "https://files.betacraft.uk/launcher/assets/libs-windows.zip";

	static {
		natives.put(Os.WINDOWS, "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
		natives.put(Os.MAC, "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
		natives.put(Os.LINUX, "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
	}

	@Override
	public void doTask() throws Exception {
		new TaskCleanup(mcp).doTask();
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.JARS));
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.LIB));
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.NATIVES));
		
		log("Setting up Minecraft...");
		List<String> versions = VersionsParser.getVersionList();
		String chosenVersion = mcp.getOptions().getStringParameter(TaskParameter.SETUP_VERSION);
		if(!versions.contains(chosenVersion)) {
			log(new Ansi().fgMagenta().a("================ ").fgDefault().a("Current versions").fgMagenta().a(" ================").fgDefault().toString());
			log(getTable(versions));
			log(new Ansi().fgMagenta().a("==================================================").fgDefault().toString());
			//mcp.log(new Ansi().fgYellow().a("If you wish to supply your own configuration, type \"custom\".").fgDefault().toString());
		}

		// Keep asking until they have a valid option
		while (!versions.contains(chosenVersion)) {
			chosenVersion = mcp.inputString(TaskMode.SETUP.getFullName(), "Select version:");
		}
		
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.CONF));
		mcp.setCurrentVersion(VersionsParser.setCurrentVersion(mcp, chosenVersion));
		String currentVersion = mcp.getCurrentVersion();
		
		long startTime = System.currentTimeMillis();
		if(Files.notExists(MCPPaths.get(mcp, "versions.json"))) {
			log("Downloading mappings");
			FileUtil.downloadFile(VersionsParser.downloadVersion(currentVersion), MCPPaths.get(mcp, MCPPaths.CONF + "conf.zip"));
			FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.CONF + "conf.zip"), MCPPaths.get(mcp, MCPPaths.CONF), true);
		}
		
		log("Setting up workspace");
		FileUtil.deleteDirectoryIfExists(MCPPaths.get(mcp, "workspace"));
		FileUtil.copyResource(MCP.class.getClassLoader().getResourceAsStream("workspace.zip"), MCPPaths.get(mcp, "workspace.zip"));
		FileUtil.unzip(MCPPaths.get(mcp, "workspace.zip"), MCPPaths.get(mcp, "workspace"), true);
		
		setWorkspace();
		log("Done in " + Util.time(System.currentTimeMillis() - startTime));

		// Delete Minecraft.jar and Minecraft_server.jar if they exist.
		Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.CLIENT));
		Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.SERVER));

		// Download Minecraft
		{
			for(int side = 0; side <= (VersionsParser.hasServer(currentVersion) ? 1 : 0); side++) {
				startTime = System.currentTimeMillis();
				FileUtil.createDirectories(MCPPaths.get(mcp, side == Side.CLIENT.index ? MCPPaths.LIB_CLIENT : MCPPaths.LIB_SERVER));
				log("Downloading Minecraft " + (side == Side.CLIENT.index ? "client" : "server") + "...");
				String url = VersionsParser.getDownloadURL(currentVersion, side);
				String out = side == Side.CLIENT.index ? MCPPaths.CLIENT : MCPPaths.SERVER;
				Path pathOut = MCPPaths.get(mcp, out);
				if(url.endsWith(".jar")) {
					FileUtil.downloadFile(new URL(url), pathOut);
				}
				else {
					Path zip = MCPPaths.get(mcp, out.replace(".jar", ".zip"));
					FileUtil.downloadFile(new URL(url), zip);
					FileUtil.copyFileFromAZip(zip, "minecraft-server.jar", pathOut);
					Files.deleteIfExists(zip);
				}
				log("Done in " + Util.time(System.currentTimeMillis() - startTime));
			}
		}
		
		log("Downloading libraries...");
		startTime = System.currentTimeMillis();
		FileUtil.downloadFile(new URL(libsURL), MCPPaths.get(mcp, MCPPaths.LIB_CLIENT + "libs.zip"));
		String nativesURL = natives.get(Os.getOs());
		if(nativesURL == null) {
			throw new Exception("Could not find natives for your operating system");
		}
		FileUtil.downloadFile(new URL(nativesURL), MCPPaths.get(mcp, MCPPaths.LIB_CLIENT + "natives.zip"));
		log("Done in " + Util.time(System.currentTimeMillis() - startTime));
		FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.LIB_CLIENT + "libs.zip"), MCPPaths.get(mcp, MCPPaths.LIB_CLIENT), true);
		FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.LIB_CLIENT + "natives.zip"), MCPPaths.get(mcp, MCPPaths.NATIVES), true);
	}

	private void setWorkspace() throws Exception {
		String currentVersion = mcp.getCurrentVersion();
		String[] projects = { "Client", "Server" };
		for (int i2 = 0; i2 < projects.length; i2++) {
			String project = projects[i2];
			String startclass = VersionsParser.hasServer(currentVersion) && VersionsParser.getServerVersion(currentVersion).startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer";
			Path[] filetoRead = new Path[] {
					Paths.get("workspace", ".metadata", ".plugins", "org.eclipse.debug.core", ".launches", project + ".launch"),
					Paths.get("workspace", project, ".idea", "workspace.xml"),
					Paths.get("workspace", project, project + ".iml"),
					Paths.get("workspace", ".metadata", ".plugins", "org.eclipse.debug.ui", "launchConfigurationHistory.xml")};
			for(int j = 0; j < filetoRead.length; j++) {
				if (Files.exists(filetoRead[j])) {
					List<String> lines = Files.readAllLines(filetoRead[j]);
					for (int i = 0; i < lines.size(); i++) {
						switch (j) {
						case 0:
							if (i2 == Side.SERVER.index) {
								String[] replace = {"value=\"/Server/src/%s.java\"", "key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"%s\""};
								lines.set(i, lines.get(i).replace(replace[0], String.format(replace[0], startclass.replace(".", "/"))));
								lines.set(i, lines.get(i).replace(replace[1], String.format(replace[1], startclass)));
							}
						case 1:
							String replace = "-Dhttp.proxyPort=%s";
							lines.set(i, lines.get(i).replace(replace, String.format(replace, VersionsParser.getProxyPort(currentVersion))));
							if (i2 == Side.SERVER.index) {
								String replace2 = "name=\"MAIN_CLASS_NAME\" value=\"%s\"";
								lines.set(i, lines.get(i).replace(replace2, String.format(replace2, startclass)));
							}
							break;
						case 2:
							lines.set(i, lines.get(i).replace("$MCP_LOC$", Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString().replace("\\", "/")));
							break;
						case 3:
							if(i2 == Side.SERVER.index && !VersionsParser.hasServer(currentVersion) && lines.get(i).contains("path=&quot;Server&quot;")) {
								lines.remove(i);
							}
							break;
						}
					}
					Files.write(filetoRead[j], lines);
				}
			}
		}
		if(!VersionsParser.hasServer(currentVersion)) {
			FileUtil.deleteDirectoryIfExists(Paths.get("workspace", "Server"));
			FileUtil.deleteDirectoryIfExists(Paths.get("workspace/.metadata/.plugins/org.eclipse.core.resources/.projects/Server"));
			Files.deleteIfExists(Paths.get("workspace/.metadata/.plugins/org.eclipse.debug.core/.launches/Server.launch"));
			Files.deleteIfExists(Paths.get("workspace/.metadata/.plugins/org.eclipse.core.resources/.root/0.tree"));
			FileUtil.copyResource(MCP.class.getClassLoader().getResourceAsStream("0.tree"), Paths.get("workspace/.metadata/.plugins/org.eclipse.core.resources/.root/0.tree"));
		}
	}

	private static String getTable(List<String> versions) {
		int rows = (int)Math.ceil(versions.size() / 3D);
		List<String>[] tableList = (List<String>[]) new List[rows];
		for (int i = 0; i < tableList.length; i++)
		{
			tableList[i] = new ArrayList<>();
		}
		StringBuilder table = new StringBuilder();
		int index = 0;
		for (String ver : versions) {
			tableList[index % rows].add(new Ansi().fgBrightCyan().a(" - ").fgDefault().fgCyan().a(String.format("%-16s", ver)).fgDefault().toString());
			index++;
		}
		for (int i = 0; i < tableList.length; i++)
		{
			for (String ver : tableList[i]) {
				table.append(ver);
			}
			if(i < tableList.length - 1) table.append("\n");
		}
		return table.toString();
	}
}
