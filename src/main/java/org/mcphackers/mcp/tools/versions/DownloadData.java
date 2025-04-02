package org.mcphackers.mcp.tools.versions;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.mcphackers.mcp.DownloadListener;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.json.AssetIndex;
import org.mcphackers.mcp.tools.versions.json.AssetIndex.Asset;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Rule;
import org.mcphackers.mcp.tools.versions.json.Version;

public class DownloadData {

	private final Path gameDir;
	public int totalSize;
	protected List<Download> downloadQueue = new ArrayList<>();
	protected AssetIndex assets;

	public DownloadData(MCP mcp, Version version) {
		this(MCPPaths.get(mcp, MCPPaths.LIB), MCPPaths.get(mcp, MCPPaths.JARS), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER), version);
	}

	public DownloadData(Path libraries, Path gameDir, Path client, Path server, Version version) {
		this.gameDir = gameDir;
		queueDownload(version.downloads.artifacts.get("client"), client);
		queueDownload(version.downloads.artifacts.get("server"), server);
		for (DependDownload dependencyDownload : version.libraries) {
			if (Rule.apply(dependencyDownload.rules)) {
				queueDownload(dependencyDownload.getDownload(null), libraries);
				queueDownload(dependencyDownload.getDownload(dependencyDownload.getNatives()), libraries);
				queueDownload(dependencyDownload.getDownload("sources"), libraries);
			}
		}
		try {
			Path assetIndex = gameDir.resolve("assets/indexes/" + version.assets + ".json");
			String assetIndexString;
			if (!Files.exists(assetIndex) || !version.assetIndex.sha1.equals(Util.getSHA1(assetIndex))) {
				assetIndexString = new String(Util.readAllBytes(FileUtil.openURLStream(new URL(version.assetIndex.url))));
				Files.write(assetIndex, assetIndexString.getBytes());
			} else {
				assetIndexString = new String(Files.readAllBytes(assetIndex));
			}
			setAssets(AssetIndex.from(new JSONObject(assetIndexString)));
		} catch (IOException ignored) {
		}
	}

	public static List<String> getLibraries(Version version) {
		List<String> retList = new ArrayList<>();
		for (DependDownload dependencyDownload : version.libraries) {
			if (Rule.apply(dependencyDownload.rules)) {
				String lib = dependencyDownload.getArtifactPath(null);
				retList.add(lib);
			}
		}
		return retList;
	}

	public static List<Path> getLibraries(Path libDir, Version version) {
		List<Path> retList = new ArrayList<>();
		for (DependDownload dependencyDownload : version.libraries) {
			if (Rule.apply(dependencyDownload.rules)) {
				String lib = dependencyDownload.getArtifactPath(null);
				if(lib == null) {
					continue;
				}
				retList.add((libDir.resolve(lib)));
			}
		}
		return retList;
	}

	public static List<Path> getNatives(Path libDir, Version version) {
		List<Path> retList = new ArrayList<>();
		for (DependDownload dependencyDownload : version.libraries) {
			if (Rule.apply(dependencyDownload.rules)) {
				String natives = dependencyDownload.getNatives();
				if (natives != null) {
					String lib = dependencyDownload.getArtifactPath(natives);
					if(lib == null) {
						continue;
					}
					retList.add(libDir.resolve(lib));
				}
			}
		}
		return retList;
	}

	public void setAssets(AssetIndex assets) {
		if (this.assets != null) {
			return;
		}
		this.assets = assets;
		Path dir = gameDir.resolve(assets.map_to_resources ? "resources/" : "assets/objects/");
		for (Entry<String, Asset> entry : assets.objects.entrySet()) {
			queueDownload(entry.getValue(), dir);
		}
	}

	public void queueDownload(IDownload dl, Path baseDir) {
		if (dl == null) {
			return;
		}
		totalSize += dl.downloadSize();
		downloadQueue.add(new Download(dl, baseDir));
	}

	public void performDownload(DownloadListener listener) throws IOException {
		for (Download dl : downloadQueue) {
			IDownload download = dl.download;
			Path baseDir = dl.dir;
			String path = download.downloadPath();
			// if downloadPath is null then baseDir is the location of downloaded file.
			Path file = path == null ? baseDir : baseDir.resolve(path);
			listener.notify(dl.download, totalSize);
			if (!Files.exists(file) || (download.verify() && !download.downloadHash().equals(Util.getSHA1(file)))) {
				Path parent = file.getParent();
				if (parent != null) Files.createDirectories(parent);
				FileUtil.downloadFile(download.downloadURL(), file);
			}
		}
	}

	private static class Download {
		IDownload download;
		Path dir;

		public Download(IDownload dl, Path path) {
			download = dl;
			dir = path;
		}
	}
}
