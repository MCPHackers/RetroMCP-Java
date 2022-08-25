package org.mcphackers.mcp.tools.versions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.mcphackers.mcp.DownloadListener;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.json.AssetIndex;
import org.mcphackers.mcp.tools.versions.json.AssetIndex.Asset;
import org.mcphackers.mcp.tools.versions.json.Download;

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
	
	public DownloadData() {
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
}
