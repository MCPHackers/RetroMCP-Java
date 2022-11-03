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
import org.mcphackers.mcp.tools.versions.json.Artifact;
import org.mcphackers.mcp.tools.versions.json.AssetIndex;
import org.mcphackers.mcp.tools.versions.json.AssetIndex.Asset;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Download;
import org.mcphackers.mcp.tools.versions.json.Rule;
import org.mcphackers.mcp.tools.versions.json.Version;

public class DownloadData {

	public static class DownloadEntry {
		public final Download dlObject;
		public final Path path;
		public final boolean verifySHA1;

		public DownloadEntry(Download dl, Path filePath, boolean verify) {
			path = filePath;
			dlObject = dl;
			verifySHA1 = verify;
		}
	}

	protected List<DownloadEntry> downloadQueue = new ArrayList<>();
	protected AssetIndex assets;
	protected Path assetsPath;
	public int totalSize;

	public DownloadData(MCP mcp, Version version) {
		if(version.downloads.client != null) {
			add(version.downloads.client, MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT));
		}
		if(version.downloads.server != null) {
			add(version.downloads.client, MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER));
		}
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				if(dependencyDownload.downloads.artifact != null) {
					add(dependencyDownload.downloads.artifact, MCPPaths.get(mcp, MCPPaths.LIB + dependencyDownload.downloads.artifact.path));
				}

				if(dependencyDownload.downloads.classifiers != null) {
					Artifact artifact = dependencyDownload.downloads.classifiers.getNatives();
					if(artifact != null) {
						add(artifact, MCPPaths.get(mcp, MCPPaths.LIB + artifact.path));
					}
				}
			}
		}
		try {
			Path assetIndex = MCPPaths.get(mcp, MCPPaths.JARS + "assets/indexes/" + version.assets + ".json");
			String assetIndexString;
			if (!Files.exists(assetIndex) || !version.assetIndex.sha1.equals(Util.getSHA1(assetIndex))) {
				assetIndexString = new String(Util.readAllBytes(new URL(version.assetIndex.url).openStream()));
				Files.write(assetIndex, assetIndexString.getBytes());
			}
			else {
				assetIndexString = new String(Files.readAllBytes(assetIndex));
			}
			addAssets(AssetIndex.from(new JSONObject(assetIndexString)), MCPPaths.get(mcp, MCPPaths.JARS));
		}
		catch (IOException e) {};
	}

	public void addAssets(AssetIndex assets, Path path) {
		this.assets = assets;
		assetsPath = path;
		for(Entry<String, Asset> entry : assets.objects.entrySet()) {
			totalSize += entry.getValue().size();
		}
	}

	public void add(Download dl, Path path) {
		add(dl, path, true);
	}

	public void add(Download dl, Path path, boolean verify) {
		if(dl == null) {
			return;
		}
		totalSize += dl.size();
		downloadQueue.add(new DownloadEntry(dl, path, verify));
	}

	public void performDownload(DownloadListener listener) throws IOException {
		for(DownloadEntry dl : downloadQueue) {
			Path file = dl.path;
			Download dlObj = dl.dlObject;
			listener.notify(dlObj, totalSize);
			if(!Files.exists(file) || (dl.verifySHA1 && !dlObj.sha1.equals(Util.getSHA1(file)))) {
				Path parent = file.getParent();
				if(parent != null) Files.createDirectories(parent);
				FileUtil.downloadFile(dlObj.url, file);
			}
		}
		if(assets != null) {
			for(Entry<String, Asset> entry : assets.objects.entrySet()) {
				Asset asset = entry.getValue();
				String hash = asset.hash.substring(0, 2) + "/" + asset.hash;
				String filename = assets.map_to_resources ? "resources/" + entry.getKey() : "assets/objects/" + hash;
				Path file = assetsPath.resolve(filename);
				listener.notify(asset, totalSize);
				if(!Files.exists(file)) {
					Path parent = file.getParent();
					if(parent != null) Files.createDirectories(parent);
					FileUtil.downloadFile("http://resources.download.minecraft.net/" + hash, file);
				}
			}
		}
	}

	public static List<String> getLibraries(Version version) {
		List<String> retList = new ArrayList<>();
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				String[] path = dependencyDownload.name.split(":");
				String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2];
				retList.add(lib);
			}
		}
		return retList;
	}

	public static List<Path> getLibraries(MCP mcp, Version version) {
		List<Path> retList = new ArrayList<>();
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				String[] path = dependencyDownload.name.split(":");
				String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2] + ".jar";
				retList.add(MCPPaths.get(mcp, MCPPaths.LIB + lib));
			}
		}
		return retList;
	}

	public static List<Path> getNatives(MCP mcp, Version version) {
		List<Path> retList = new ArrayList<>();
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				if(dependencyDownload.downloads.classifiers != null) {
					Artifact artifact = dependencyDownload.downloads.classifiers.getNatives();
					if(artifact != null) {
						retList.add(MCPPaths.get(mcp, MCPPaths.LIB + artifact.path));
					}
				}
			}
		}
		return retList;
	}
}
