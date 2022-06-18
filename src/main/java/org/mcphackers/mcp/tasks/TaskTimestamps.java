package org.mcphackers.mcp.tasks;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskTimestamps extends Task {

	public TaskTimestamps(MCP instance) {
		super(instance);
	}

	@Override
	public void doTask() throws Exception {
		List<String> verList = new ArrayList<>();
		Iterator<String> iterator = VersionsParser.json.keys();
		iterator.forEachRemaining(verList::add);
		Side[] sides = {Side.CLIENT, Side.SERVER};
		FileUtil.createDirectories(MCPPaths.get(mcp, "timestamps"));
		for (int i = 0; i < verList.size(); i++) {
			String ver = verList.get(i);
			setProgress(ver, (int)((float)i / (float)verList.size() * 100));
			for(Side side : sides) {
				Path jar = MCPPaths.get(mcp, "timestamps/version.jar");
				if(side == Side.SERVER && VersionsParser.hasServer(ver) || side == Side.CLIENT) {
					FileUtil.downloadFile(new URL(VersionsParser.getDownloadURL(ver, side)), jar);
				}
				else break;
				String time = side == Side.CLIENT ? "client_timestamp" : "server_timestamp";
				String value = null;
				try (FileSystem fs = FileSystems.newFileSystem(jar, (ClassLoader)null)) {
					Path meta = fs.getPath("/").resolve("META-INF/MANIFEST.MF");
					if(Files.exists(meta)) {
						value = Files.getLastModifiedTime(meta).toInstant().toString();
					}
				}
				if(value != null) {
					VersionsParser.json.getJSONObject(ver).put(time, value);
				}
			}
		}
		try (BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, "timestamps/versions.json"))) {
			VersionsParser.json.write(writer, 1, 1);
		}
	}
}