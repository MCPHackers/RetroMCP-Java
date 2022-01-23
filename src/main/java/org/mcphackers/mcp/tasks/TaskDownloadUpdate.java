package org.mcphackers.mcp.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;

import jredfox.selfcmd.SelfCommandPrompt;

public class TaskDownloadUpdate extends Task {

	private static final String API = "https://api.github.com/repos/MCPHackers/RetroMCP-Java/releases/latest";
	
    public TaskDownloadUpdate(TaskInfo info) {
		super(-1 , info);
	}

    @Override
    public void doTask() throws Exception {
		URL updateURL = new URL(API);
		InputStream in = updateURL.openStream();
		JSONObject releaseJson = Util.parseJSONFile(in);
		String latestVersion = releaseJson.getString("tag_name");
		String notes = releaseJson.getString("body");
		if(!latestVersion.equals(MCP.VERSION)) {
			MCP.logger.info("New version found: " + latestVersion);
			MCP.logger.info(notes);
			for(Object obj : releaseJson.getJSONArray("assets")) {
				if(obj instanceof JSONObject) {
					JSONObject assetObj = (JSONObject)obj;
					if(!assetObj.getString("name").endsWith(".jar")) {
						continue;
					}
					FileUtil.downloadFile(new URL(assetObj.getString("browser_download_url")), Paths.get(MCPConfig.UPDATE_JAR));
				}
			}
			MCP.logger.info("Press ENTER key to continue");
			MCP.input.nextLine();
			Path jarPath = Paths.get(MCP.class
			          .getProtectionDomain()
			          .getCodeSource()
			          .getLocation()
			          .toURI());
			if(!Files.isDirectory(jarPath)) {
				String[] cmd = new String[] {
					Util.getJava(),
					"-cp",
					MCPConfig.UPDATE_JAR,
					"org.mcphackers.mcp.Update",
					jarPath.toString()
				};
				Util.runCommand(cmd);
				SelfCommandPrompt.shutdown();
			}
			else {
				throw new IOException("Running from a folder! Aborting");
			}
		}
    }
}
