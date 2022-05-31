package org.mcphackers.mcp.tasks;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Os;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskSetup extends Task {

	public TaskSetup(MCP instance) {
		super(Side.ANY, instance);
	}

	private static final Map<Os, String> natives = new HashMap<>();
	private static final String libsURL = "https://files.betacraft.uk/launcher/assets/libs-windows.zip";

	static {
		natives.put(Os.WINDOWS, "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
		natives.put(Os.OSX, "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
		natives.put(Os.LINUX, "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
	}

	@Override
	public void doTask() throws Exception {
		new TaskCleanup(mcp).doTask();
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.JARS));
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.LIB));
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.NATIVES));
		
		setProgress("Setting up", 1);
		List<String> versions = VersionsParser.getVersionList();
		String chosenVersion = mcp.getOptions().getStringParameter(TaskParameter.SETUP_VERSION);

		// Keep asking until they have a valid option
		while (!versions.contains(chosenVersion)) {
			chosenVersion = mcp.inputString(TaskMode.SETUP.getFullName(), "Select version:");
		}
		
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.CONF));
		mcp.setCurrentVersion(VersionsParser.setCurrentVersion(mcp, chosenVersion));
		String currentVersion = mcp.getCurrentVersion();
		
		setProgress("Downloading mappings", 5);
		FileUtil.downloadFile(VersionsParser.downloadVersion(currentVersion), MCPPaths.get(mcp, MCPPaths.CONF + "conf.zip"));
		FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.CONF + "conf.zip"), MCPPaths.get(mcp, MCPPaths.CONF), true);

		Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT));
		Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER));

		// Download Minecraft
		Side[] sides = VersionsParser.hasServer(currentVersion) ? new Side[] {Side.CLIENT, Side.SERVER} : new Side[] {Side.CLIENT};

		setProgress(30);
		for(Side side : sides) {
			FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.LIBS, side));
			setProgress("Downloading Minecraft " + side.name.toLowerCase());
			String url = VersionsParser.getDownloadURL(currentVersion, side);
			String out = MCPPaths.JAR_ORIGINAL;
			Path pathOut = MCPPaths.get(mcp, out, side);
			if(url.endsWith(".jar")) {
				FileUtil.downloadFile(new URL(url), pathOut);
			}
			else {
				Path zip = MCPPaths.get(mcp, out.replace(".jar", ".zip"), side);
				FileUtil.downloadFile(new URL(url), zip);
				FileUtil.copyFileFromAZip(zip, "minecraft-server.jar", pathOut);
				Files.deleteIfExists(zip);
			}
		}
		
		setProgress("Downloading libraries", 50);
		FileUtil.downloadFile(new URL(libsURL), MCPPaths.get(mcp, MCPPaths.LIBS + "libs.zip", Side.CLIENT));
		String nativesURL = natives.get(Os.getOs());
		if(nativesURL == null) {
			throw new Exception("Could not find natives for your operating system");
		}
		setProgress(70);
		FileUtil.downloadFile(new URL(nativesURL), MCPPaths.get(mcp, MCPPaths.LIBS + "natives.zip", Side.CLIENT));
		FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.LIBS + "libs.zip", Side.CLIENT), MCPPaths.get(mcp, MCPPaths.LIBS, Side.CLIENT), true);
		FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.LIBS + "natives.zip", Side.CLIENT), MCPPaths.get(mcp, MCPPaths.NATIVES), true);

		setProgress("Setting up workspace", 75);
		FileUtil.deleteDirectoryIfExists(MCPPaths.get(mcp, "workspace"));
		FileUtil.copyResource(ClassUtils.getResource(MCP.class, "workspace/workspace.zip"), MCPPaths.get(mcp, "workspace.zip"));
		FileUtil.unzip(MCPPaths.get(mcp, "workspace.zip"), MCPPaths.get(mcp, "workspace"), true);
		setWorkspace();
	}

	private void setWorkspace() throws Exception {
		String currentVersion = mcp.getCurrentVersion();
		Side[] sides = { Side.CLIENT, Side.SERVER };
		for (Side side : sides) {
			String project = side.name;
			String startclass;
			try {
				startclass = TaskRun.findStartClass(mcp, side);
			} catch (Exception e) {
				startclass = "Start";
			}
			Path[] filetoRead = new Path[] {
					MCPPaths.get(mcp, "workspace/.metadata/.plugins/org.eclipse.debug.core/.launches/" + project + ".launch"),
					MCPPaths.get(mcp, "workspace/" + project + "/.idea/workspace.xml"),
					MCPPaths.get(mcp, "workspace/" + project + "/" + project + ".iml"),
					MCPPaths.get(mcp, "workspace/.metadata/.plugins/org.eclipse.debug.ui/launchConfigurationHistory.xml")};
			for(int j = 0; j < filetoRead.length; j++) {
				if (Files.exists(filetoRead[j])) {
					List<String> lines = Files.readAllLines(filetoRead[j]);
					for (int i = 0; i < lines.size(); i++) {
						switch (j) {
						//TODO This should be remade completely
						case 0:
							if (side == Side.SERVER) {
								String[] replace = {"value=\"/Server/src/%s.java\"", "key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"%s\""};
								lines.set(i, lines.get(i).replace(replace[0], String.format(replace[0], startclass.replace(".", "/"))));
								lines.set(i, lines.get(i).replace(replace[1], String.format(replace[1], startclass)));
							}
						case 1:
							String replace = "-Dhttp.proxyPort=%s";
							lines.set(i, lines.get(i).replace(replace, String.format(replace, VersionsParser.getProxyPort(currentVersion))));
							if (side == Side.SERVER) {
								String replace2 = "name=\"MAIN_CLASS_NAME\" value=\"%s\"";
								lines.set(i, lines.get(i).replace(replace2, String.format(replace2, startclass)));
							}
							break;
						case 2:
							lines.set(i, lines.get(i).replace("$MCP_LOC$", mcp.getWorkingDir().toAbsolutePath().toString().replace("\\", "/")));
							break;
						case 3:
							if(side == Side.SERVER && !VersionsParser.hasServer(currentVersion) && lines.get(i).contains("path=&quot;Server&quot;")) {
								lines.remove(i);
							}
							break;
						}
					}
					Files.write(filetoRead[j], lines);
				}
			}
		}
		if(!TaskMode.DECOMPILE.requirement.get(mcp, Side.SERVER)) {
			FileUtil.deleteDirectoryIfExists(MCPPaths.get(mcp, "workspace/Server"));
			FileUtil.deleteDirectoryIfExists(MCPPaths.get(mcp, "workspace/.metadata/.plugins/org.eclipse.core.resources/.projects/Server"));
			Files.deleteIfExists(MCPPaths.get(mcp, "workspace/.metadata/.plugins/org.eclipse.debug.core/.launches/Server.launch"));
			Files.deleteIfExists(MCPPaths.get(mcp, "workspace/.metadata/.plugins/org.eclipse.core.resources/.root/0.tree"));
			FileUtil.copyResource(ClassUtils.getResource(MCP.class, "workspace/0.tree"), MCPPaths.get(mcp, "workspace/.metadata/.plugins/org.eclipse.core.resources/.root/0.tree"));
		}
	}
}
