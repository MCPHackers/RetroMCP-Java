package org.mcphackers.mcp.tasks;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;
import org.mcphackers.mcp.tools.versions.json.Version;

public class TaskSetup extends Task {

	private int libsSize = 0;

	public TaskSetup(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		new TaskCleanup(mcp).cleanup();
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

		JSONObject versionJsonObj = new JSONObject(new String(Util.readAllBytes(new URL(chosenVersionData.url).openStream()), StandardCharsets.UTF_8));
		VersionParser.fixLibraries(versionJsonObj);
		Version versionJson = Version.from(versionJsonObj);
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.CONF));

		if(chosenVersionData.resources != null) {
			setProgress(getLocalizedStage("download", chosenVersionData.resources), 2);
			FileUtil.extract(new URL(chosenVersionData.resources).openStream(), MCPPaths.get(mcp, MCPPaths.CONF));
		}

		DownloadData dlData = new DownloadData(mcp, versionJson);
		dlData.performDownload((dl, totalSize) -> {
			libsSize += dl.size();
			int percent = (int)((double)libsSize / totalSize * 87D);
			setProgress(getLocalizedStage("download", dl.name()), 3 + percent);
		});
		Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES);
		for(Path nativeArchive : DownloadData.getNatives(mcp, versionJson)) {
			FileUtil.extract(nativeArchive, natives);
		}
		try(BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, MCPPaths.VERSION))) {
			versionJsonObj.write(writer, 1, 0);
		}
		mcp.setCurrentVersion(versionJson);

		setProgress(getLocalizedStage("workspace"), 90);
		setWorkspace(versionJson);
	}

	private void setWorkspace(Version version) throws Exception {
		//FileUtil.extract(TaskSetup.class.getClassLoader().getResourceAsStream("gradle.zip"), mcp.getWorkingDir().toAbsolutePath());
		//TODO generate build.gradle
	}
}
