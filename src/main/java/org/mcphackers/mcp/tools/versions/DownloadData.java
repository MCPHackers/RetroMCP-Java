package org.mcphackers.mcp.tools.versions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.DownloadListener;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.json.Artifact;
import org.mcphackers.mcp.tools.versions.json.AssetIndex;
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
	public int totalSize;
	public List<DownloadEntry> natives = new ArrayList<>();
	
	public DownloadData(MCP mcp, Version version) {
		this(MCPPaths.get(mcp, MCPPaths.LIB), MCPPaths.get(mcp, MCPPaths.JARS), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER), version);
	}

	public DownloadData(Path libraries, Path gameDir, Path client, Path server, Version version) {
		if(version.downloads.client != null && client != null) {
			queueDownload(version.downloads.client, client, true); // TODO may want to make verify flag togglable
		}
		if(version.downloads.server != null && server != null) {
			queueDownload(version.downloads.server, server, true);
		}
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				if(dependencyDownload.downloads != null && dependencyDownload.downloads.artifact != null) {
					queueDownload(dependencyDownload.downloads.artifact, libraries.resolve(dependencyDownload.downloads.artifact.path), true);
				}

				if(dependencyDownload.downloads != null && dependencyDownload.downloads.classifiers != null) {
					Artifact artifact = dependencyDownload.downloads.classifiers.getNatives();
					if(artifact != null) {
						natives.add(queueDownload(artifact, libraries.resolve(artifact.path), true));
					}
					artifact = dependencyDownload.downloads.classifiers.sources;
					if(artifact != null) {
						queueDownload(artifact, libraries.resolve(artifact.path), true);
					}
				}
			}
		}
	}

	public DownloadEntry queueDownload(Download dl, Path path, boolean verify) {
		if(dl == null) {
			return null;
		}
		DownloadEntry entry = new DownloadEntry(dl, path, verify);
		totalSize += dl.size();
		downloadQueue.add(entry);
		return entry;
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

	public static List<Path> getLibraries(Path libDir, Version version) {
		List<Path> retList = new ArrayList<>();
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				String[] path = dependencyDownload.name.split(":");
				String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2] + ".jar";
				retList.add((libDir.resolve(lib)));
			}
		}
		return retList;
	}

	public static List<Path> getNatives(Path libDir, Version version) {
		List<Path> retList = new ArrayList<>();
		for(DependDownload dependencyDownload : version.libraries) {
			if(Rule.apply(dependencyDownload.rules)) {
				if(dependencyDownload.downloads != null && dependencyDownload.downloads.classifiers != null) {
					Artifact artifact = dependencyDownload.downloads.classifiers.getNatives();
					if(artifact != null) {
						retList.add(libDir.resolve(artifact.path));
					}
				}
			}
		}
		return retList;
	}
}
