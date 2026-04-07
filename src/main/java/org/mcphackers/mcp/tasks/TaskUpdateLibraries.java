package org.mcphackers.mcp.tasks;

import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.json.Version;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mcphackers.mcp.MCPPaths.*;

public class TaskUpdateLibraries extends TaskStaged {
	private long libsSize = 0;

	public TaskUpdateLibraries(MCP instance) {
		super(Side.CLIENT, instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[] {
				stage(getLocalizedStage("delete_libraries"), 0, () -> {
					Path libraries = MCPPaths.get(mcp, MCPPaths.LIB);
					Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES);
					FileUtil.cleanDirectory(natives);
					FileUtil.cleanDirectory(libraries);
				}),
				stage(getLocalizedStage("update_libraries"), () -> {
					VersionParser versionParser = mcp.getVersionParser();
					Version version = this.mcp.getCurrentVersion();
					if (version == null) {
						RuntimeException t = new RuntimeException("Current version is null!");
						Util.throwExceptionInIDE(t);
						throw t;
					}
					VersionParser.VersionData data = versionParser.getVersion(version.id);
					InputStream versionStream;
					try {
						versionStream = new URL(data.url).openStream();
					} catch (MalformedURLException ex) {
						versionStream = Files.newInputStream(MCPPaths.get(mcp, data.url));
					}
					JSONObject versionJsonObj = new JSONObject(new String(Util.readAllBytes(versionStream), StandardCharsets.UTF_8));
					Version versionJson = Version.from(versionJsonObj);
					FileUtil.createDirectories(MCPPaths.get(mcp, CONF));

					if (data.resources != null) {
						setProgress(getLocalizedStage("download", data.resources), 2);
						try {
							URL url = new URL(data.resources);
							FileUtil.extract(url.openStream(), MCPPaths.get(mcp, CONF));
						} catch (MalformedURLException e) {
							Path p = Paths.get(data.resources);
							if (Files.exists(p)) {
								FileUtil.extract(p, MCPPaths.get(mcp, CONF));
							}
						}
					}

					DownloadData dlData = new DownloadData(mcp, versionJson);
					dlData.performDownload((dl, totalSize) -> {
						libsSize += dl.downloadSize();
						int percent = (int) ((double) libsSize / totalSize * 97D);
						setProgress(getLocalizedStage("download", dl.downloadURL()), 3 + percent);
					});
					Path natives = MCPPaths.get(mcp, NATIVES);
					Files.createDirectories(natives);
					for (Path nativeArchive : DownloadData.getNatives(MCPPaths.get(mcp, LIB), versionJson)) {
						// Create dir inside natives dir if dir does not exist
						if (!nativeArchive.getParent().equals(natives)) {
							Files.createDirectories(nativeArchive.getParent());
						}
						FileUtil.extract(nativeArchive, natives);
					}
					mcp.setCurrentVersion(version);
				})
		};
	}
}
