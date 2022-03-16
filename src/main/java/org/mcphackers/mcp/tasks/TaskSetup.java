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
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskSetup extends Task {

	public TaskSetup(MCP mcp) {
		super(0, mcp);
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
		if (Files.exists(Paths.get(MCPPaths.SRC))) {
			//mcp.log(new Ansi().a("Sources folder found! Type \"yes\" if you want to continue setup").fgRgb(255,255,255).toString());
			// FIXME
//			String confirm = MCP.input.nextLine().toLowerCase();
//			MCP.logger.print(new Ansi().fgDefault());
//			if(!confirm.toLowerCase().equals("yes")) {
//				//info.addInfo("Setup aborted!");
//				return;
//			}
		}

		//MCP.config.srcCleanup = false;
		new TaskCleanup(mcp).doTask();
		mcp.log(" Setting up your workspace...");
		FileUtil.createDirectories(Paths.get(MCPPaths.JARS));
		FileUtil.createDirectories(Paths.get(MCPPaths.NATIVES));
		
		mcp.log(" Setting up Minecraft...");
		List<String> versions = VersionsParser.getVersionList();
		String chosenVersion = mcp.getStringParam(TaskParameter.SETUP_VERSION);
		if(!versions.contains(chosenVersion)) {
			mcp.log(new Ansi().fgMagenta().a("================ ").fgDefault().a("Current versions").fgMagenta().a(" ================").fgDefault().toString());
			mcp.log(getTable(versions));
			mcp.log(new Ansi().fgMagenta().a("==================================================").fgDefault().toString());
			//mcp.log(new Ansi().fgYellow().a("If you wish to supply your own configuration, type \"custom\".").fgDefault().toString());
		}
		//FIXME
		// Keep asking until they have a valid option
//		while (!versions.contains(chosenVersion)) {
//			MCP.logger.print(new Ansi().a("Select version: ").fgBrightGreen());
//			chosenVersion = MCP.input.nextLine().toLowerCase();
//			MCP.logger.print(new Ansi().fgDefault());
//		}
		
		FileUtil.createDirectories(Paths.get(MCPPaths.CONF));
		VersionsParser.setCurrentVersion(chosenVersion);
		
		long startTime = System.currentTimeMillis();
		if(Files.notExists(Paths.get("versions.json"))) {
			mcp.log(" Downloading mappings");
			FileUtil.downloadFile(VersionsParser.downloadVersion(), Paths.get(MCPPaths.CONF, "conf.zip"));
			FileUtil.unzip(Paths.get(MCPPaths.CONF, "conf.zip"), Paths.get(MCPPaths.CONF), true);
		}
		
		mcp.log(" Setting up workspace");
		FileUtil.deleteDirectoryIfExists(Paths.get("workspace"));
		FileUtil.copyResource(MCP.class.getClassLoader().getResourceAsStream("workspace.zip"), Paths.get("workspace.zip"));
		FileUtil.unzip(Paths.get("workspace.zip"), Paths.get("workspace"), true);
		
		setWorkspace();
		mcp.log(" Done in " + Util.time(System.currentTimeMillis() - startTime));

		// Delete Minecraft.jar and Minecraft_server.jar if they exist.
		Files.deleteIfExists(Paths.get(MCPPaths.CLIENT));
		Files.deleteIfExists(Paths.get(MCPPaths.SERVER));

		// Download Minecraft
		//if (!chosenVersion.equals("custom")) TODO
		{
			for(int side = 0; side <= (VersionsParser.hasServer() ? 1 : 0); side++) {
				startTime = System.currentTimeMillis();
				mcp.log(" Downloading Minecraft " + (side == CLIENT ? "client" : "server") + "...");
				String url = VersionsParser.getDownloadURL(side);
				String out = side == CLIENT ? MCPPaths.CLIENT : MCPPaths.SERVER;
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
				mcp.log(" Done in " + Util.time(System.currentTimeMillis() - startTime));
			}
		}
		
		mcp.log(" Downloading libraries...");
		startTime = System.currentTimeMillis();
		FileUtil.downloadFile(new URL(libsURL), Paths.get(MCPPaths.LIB + "libs.zip"));
		String nativesURL = natives.get(Util.getOperatingSystem());
		if(nativesURL == null) {
			throw new Exception("Could not find natives for your operating system");
		}
		FileUtil.downloadFile(new URL(nativesURL), Paths.get(MCPPaths.LIB + "natives.zip"));
		mcp.log(" Done in " + Util.time(System.currentTimeMillis() - startTime));
		FileUtil.unzip(Paths.get(MCPPaths.LIB + "libs.zip"), Paths.get(MCPPaths.LIB), true);
		FileUtil.unzip(Paths.get(MCPPaths.LIB + "natives.zip"), Paths.get(MCPPaths.NATIVES), true);
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
		List<String>[] tableList = new List[rows];
		for (int i = 0; i < tableList.length; i++)
		{
			tableList[i] = new ArrayList();
		}
		String table = "";
		int index = 0;
		for (String ver : versions) {
			tableList[index % rows].add(new Ansi().fgBrightCyan().a(" - ").fgDefault().fgCyan().a(String.format("%-16s", ver)).fgDefault().toString());
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
