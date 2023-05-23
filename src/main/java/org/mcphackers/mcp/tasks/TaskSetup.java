package org.mcphackers.mcp.tasks;

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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.mcphackers.mcp.MCPPaths.*;

public class TaskSetup extends Task {

	private int libsSize = 0;

	public TaskSetup(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		new TaskCleanup(mcp).cleanup();
		FileUtil.createDirectories(MCPPaths.get(mcp, JARS));
		FileUtil.createDirectories(MCPPaths.get(mcp, LIB));
		FileUtil.createDirectories(MCPPaths.get(mcp, NATIVES));

		setProgress(getLocalizedStage("setup"), 1);
		List<VersionData> versions = VersionParser.INSTANCE.getVersions();
		String chosenVersion = mcp.getOptions().getStringParameter(TaskParameter.SETUP_VERSION);
		VersionData chosenVersionData;

		// Keep asking until chosenVersion equals one of the versionData
		input:
		while (true) {
			for (VersionData data : versions) {
				if (data.id.equals(chosenVersion)) {
					chosenVersionData = data;
					break input;
				}
			}
			chosenVersion = mcp.inputString(TaskMode.SETUP.getFullName(), MCP.TRANSLATOR.translateKey("task.setup.selectVersion"));
		}

		InputStream versionStream;
		try {
			versionStream = new URL(chosenVersionData.url).openStream();
		} catch (MalformedURLException ex) {
			versionStream = Files.newInputStream(MCPPaths.get(mcp, chosenVersionData.url));
		}
		JSONObject versionJsonObj = new JSONObject(new String(Util.readAllBytes(versionStream), StandardCharsets.UTF_8));
		Version versionJson = Version.from(versionJsonObj);
		FileUtil.createDirectories(MCPPaths.get(mcp, CONF));

		if (chosenVersionData.resources != null) {
			setProgress(getLocalizedStage("download", chosenVersionData.resources), 2);
			try {
				URL url = new URL(chosenVersionData.resources);
				FileUtil.extract(url.openStream(), MCPPaths.get(mcp, CONF));
			} catch (MalformedURLException e) {
				Path p = Paths.get(chosenVersionData.resources);
				if (Files.exists(p)) {
					FileUtil.extract(p, MCPPaths.get(mcp, CONF));
				}
			}
		}

		DownloadData dlData = new DownloadData(mcp, versionJson);
		dlData.performDownload((dl, totalSize) -> {
			libsSize += dl.size();
			int percent = (int) ((double) libsSize / totalSize * 97D);
			setProgress(getLocalizedStage("download", dl.name()), 3 + percent);
		});
		Path natives = MCPPaths.get(mcp, NATIVES);
		for (Path nativeArchive : DownloadData.getNatives(MCPPaths.get(mcp, LIB), versionJson)) {
			FileUtil.extract(nativeArchive, natives);
		}
		try (BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, VERSION))) {
			versionJsonObj.write(writer, 1, 0);
		}
		mcp.setCurrentVersion(versionJson);
	}
}
