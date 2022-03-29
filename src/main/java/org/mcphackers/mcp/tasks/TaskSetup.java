package org.mcphackers.mcp.tasks;

import java.net.URL;
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
			if(!confirm.equalsIgnoreCase("yes")) {
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
			//MCP.logger.info(new Ansi().fgYellow().a("If you wish to supply your own configuration, type \"custom\".").fgDefault().toString());
		}
		// Keep asking until they have a valid option
		while (!versions.contains(chosenVersion)) {
			MCP.logger.print(new Ansi().a("Select version: ").fgBrightGreen());
			chosenVersion = MCP.input.nextLine().toLowerCase();
			MCP.logger.print(new Ansi().fgDefault());
		}
		
		FileUtil.createDirectories(Paths.get(MCPConfig.CONF));
		VersionsParser.setCurrentVersion(chosenVersion);
		
		long startTime = System.currentTimeMillis();
		if(Files.notExists(Paths.get("versions.json"))) {
			MCP.logger.info(" Downloading mappings");
			FileUtil.downloadFile(VersionsParser.downloadVersion(), Paths.get(MCPConfig.CONF, "conf.zip"));
			FileUtil.unzip(Paths.get(MCPConfig.CONF, "conf.zip"), Paths.get(MCPConfig.CONF), true);
		}
		
		MCP.logger.info(" Setting up workspace");
		FileUtil.deleteDirectoryIfExists(Paths.get("workspace"));
		FileUtil.copyResource(MCP.class.getClassLoader().getResourceAsStream("workspace.zip"), Paths.get("workspace.zip"));
		FileUtil.unzip(Paths.get("workspace.zip"), Paths.get("workspace"), true);
		
		setWorkspace();
		MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));

		// Delete Minecraft.jar and Minecraft_server.jar if they exist.
		Files.deleteIfExists(Paths.get(MCPConfig.CLIENT));
		Files.deleteIfExists(Paths.get(MCPConfig.SERVER));

		// Download Minecraft
		//if (!chosenVersion.equals("custom")) TODO
		{
			for(int side = 0; side <= (VersionsParser.hasServer() ? 1 : 0); side++) {
				startTime = System.currentTimeMillis();
				MCP.logger.info(" Downloading Minecraft " + (side == CLIENT ? "client" : "server") + "...");
				String url = VersionsParser.getDownloadURL(side);
				String out = side == CLIENT ? MCPConfig.CLIENT : MCPConfig.SERVER;
				Path pathOut = Paths.get(out);
				if(url.endsWith(".jar")) {
					FileUtil.downloadFile(new URL(url), pathOut);
				}
				else {
					Path zip = Paths.get(out.replace(".jar", ".zip"));
					FileUtil.downloadFile(new URL(url), zip);
					FileUtil.copyFileFromAZip(zip, "minecraft-server.jar", pathOut);
					Files.deleteIfExists(zip);
				}
				MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
			}
		}
		
		MCP.logger.info(" Downloading libraries...");
		startTime = System.currentTimeMillis();
		FileUtil.downloadFile(new URL(libsURL), Paths.get(MCPConfig.LIB + "libs.zip"));
		String nativesURL = natives.get(Util.getOperatingSystem());
		if(nativesURL == null) {
			throw new Exception("Could not find natives for your operating system");
		}
		FileUtil.downloadFile(new URL(nativesURL), Paths.get(MCPConfig.LIB + "natives.zip"));
		MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
		FileUtil.unzip(Paths.get(MCPConfig.LIB + "libs.zip"), Paths.get(MCPConfig.LIB), true);
		FileUtil.unzip(Paths.get(MCPConfig.LIB + "natives.zip"), Paths.get(MCPConfig.NATIVES), true);

		FileUtil.createDirectories(Paths.get(MCPConfig.DEPS_C));
		FileUtil.createDirectories(Paths.get(MCPConfig.DEPS_S));
	}

	private void setWorkspace() throws Exception {
		String[] projects = { "Client", "Server" };
		for (int i2 = 0; i2 < projects.length; i2++) {
			String project = projects[i2];
			String startclass = VersionsParser.hasServer() && VersionsParser.getServerVersion().startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer";
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
							if (i2 == SERVER) {
								String[] replace = new String[] {"value=\"/Server/src/%s.java\"", "key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"%s\""};
								lines.set(i, lines.get(i).replace(replace[0], String.format(replace[0], startclass.replace(".", "/"))));
								lines.set(i, lines.get(i).replace(replace[1], String.format(replace[1], startclass)));
							}
						case 1:
							String replace = "-Dhttp.proxyPort=%s";
							lines.set(i, lines.get(i).replace(replace, String.format(replace, VersionsParser.getProxyPort())));
							if (i2 == SERVER) {
								String replace2 = "name=\"MAIN_CLASS_NAME\" value=\"%s\"";
								lines.set(i, lines.get(i).replace(replace2, String.format(replace2, startclass)));
							}
							break;
						case 2:
							lines.set(i, lines.get(i).replace("$MCP_LOC$", Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString().replace("\\", "/")));
							break;
						case 3:
							if(i2 == SERVER && !VersionsParser.hasServer() && lines.get(i).contains("path=&quot;Server&quot;")) {
								lines.remove(i);
							}
							break;
						}
					}
					Files.write(filetoRead[j], lines);
				}
			}
		}
		if(!VersionsParser.hasServer()) {
			FileUtil.deleteDirectoryIfExists(Paths.get("workspace", "Server"));
			FileUtil.deleteDirectoryIfExists(Paths.get("workspace/.metadata/.plugins/org.eclipse.core.resources/.projects/Server"));
			Files.deleteIfExists(Paths.get("workspace/.metadata/.plugins/org.eclipse.debug.core/.launches/Server.launch"));
			Files.deleteIfExists(Paths.get("workspace/.metadata/.plugins/org.eclipse.core.resources/.root/0.tree"));
			FileUtil.copyResource(MCP.class.getClassLoader().getResourceAsStream("0.tree"), Paths.get("workspace/.metadata/.plugins/org.eclipse.core.resources/.root/0.tree"));
		}
	}

	private static String getTable(List<String> versions) {
		int rows = (int)Math.ceil(versions.size() / 3D);
		@SuppressWarnings("unchecked")
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
