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
import org.mcphackers.mcp.tools.OS;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;
import org.mcphackers.mcp.tools.versions.json.Download;
import org.mcphackers.mcp.tools.versions.json.Version;

public class TaskSetup extends Task {

	public TaskSetup(MCP instance) {
		super(Side.ANY, instance);
	}

//	private static final Map<OS, String> natives = new HashMap<>();
//	private static final String libsURL = "https://files.betacraft.uk/launcher/assets/libs-windows.zip";
//
//	static {
//		natives.put(OS.windows, "https://files.betacraft.uk/launcher/assets/natives-windows.zip");
//		natives.put(OS.osx, "https://files.betacraft.uk/launcher/assets/natives-osx.zip");
//		natives.put(OS.linux, "https://files.betacraft.uk/launcher/assets/natives-linux.zip");
//	}

	@Override
	public void doTask() throws Exception {
		new TaskCleanup(mcp).doTask();
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.JARS));
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.LIB));
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.NATIVES));
		
		setProgress(getLocalizedStage("setup"), 1);
		List<VersionData> versions = VersionParser.INSTANCE.getVersions();
		String chosenVersion = mcp.getOptions().getStringParameter(TaskParameter.SETUP_VERSION);
		VersionData chosenVersionData;

		// Keep asking until chosenVersion equals one of the versionData
		input:
		while (true) {
			for(VersionData data : versions) {
				if(data.id.equals(chosenVersion)) {
					chosenVersionData = data;
					break input;
				}
			}
			chosenVersion = mcp.inputString(TaskMode.SETUP.getFullName(), MCP.TRANSLATOR.translateKey("task.setup.selectVersion"));
		}
		
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.CONF));
		mcp.setCurrentVersion(chosenVersion);
		
		setProgress(getLocalizedStage("downloadMappings"), 2);
		FileUtil.downloadFile(new URL(chosenVersionData.resources), MCPPaths.get(mcp, MCPPaths.CONF + "conf.zip"));
		FileUtil.unzip(MCPPaths.get(mcp, MCPPaths.CONF + "conf.zip"), MCPPaths.get(mcp, MCPPaths.CONF), true);

		Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT));
		Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER));

		Version versionJson = Version.from(Util.parseJSON(new URL(chosenVersionData.versionJson).openStream()));
		Download[] downloads = new Download[] {versionJson.downloads.client, versionJson.downloads.server};
		
		setProgress(5);
		for(int i = 0; i < 2; i++) {
			Download dl = downloads[i];
			if(dl == null) continue;
			Side side = i == 0 ? Side.CLIENT : Side.SERVER;
			setProgress(MCP.TRANSLATOR.translateKeyWithFormatting("task.stage.downloadMC", side.getName().toLowerCase()));
			FileUtil.downloadFile(new URL(dl.url), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
		}
		
		setProgress(getLocalizedStage("downloadLibs"), 10);

		setProgress(getLocalizedStage("workspace"), 90);
		FileUtil.deleteDirectoryIfExists(MCPPaths.get(mcp, "workspace"));
		FileUtil.copyResource(ClassUtils.getResource(MCP.class, "workspace/workspace.zip"), MCPPaths.get(mcp, "workspace.zip"));
		FileUtil.unzip(MCPPaths.get(mcp, "workspace.zip"), MCPPaths.get(mcp, "workspace"), true);
		setWorkspace();
	}

	private void setWorkspace() throws Exception {
		String currentVersion = mcp.getCurrentVersion();
		Side[] sides = { Side.CLIENT, Side.SERVER };
		for (Side side : sides) {
			String project = side == Side.CLIENT ? "Client" : "Server";
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
							lines.set(i, lines.get(i).replace(replace, String.format(replace, "11702" /*FIXME*/)));
							if (side == Side.SERVER) {
								String replace2 = "name=\"MAIN_CLASS_NAME\" value=\"%s\"";
								lines.set(i, lines.get(i).replace(replace2, String.format(replace2, startclass)));
							}
							break;
						case 2:
							lines.set(i, lines.get(i).replace("$MCP_LOC$", mcp.getWorkingDir().toAbsolutePath().toString().replace("\\", "/")));
							break;
						case 3:
							if(side == Side.SERVER && !VersionParser.INSTANCE.getVersion(currentVersion).hasServer() && lines.get(i).contains("path=&quot;Server&quot;")) {
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
