package org.mcphackers.mcp.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.fusesource.jansi.Ansi;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;

public class TaskDownloadUpdate extends Task {

	private static final String API = "https://api.github.com/repos/MCPHackers/RetroMCP-Java/releases/latest";
	
	public TaskDownloadUpdate(MCP instance) {
		super(Side.NONE , instance);
	}

	@Override
	public void doTask() throws Exception {
		URL updateURL = new URL(API);
		InputStream in = updateURL.openStream();
		JSONObject releaseJson = Util.parseJSONFile(in);
		String latestVersion = releaseJson.getString("tag_name");
		String notes = releaseJson.getString("body");
		if(!latestVersion.equals(MCP.VERSION)) {
			//TODO no Ansi plz
			log(new Ansi().a("New version found: ").fgBrightYellow().a(latestVersion).fgDefault().toString());
			log(new Ansi().fgRgb(255,255,255).a("=========================").newline().a(notes).newline().a("=========================").fgDefault().toString());
			for(Object obj : releaseJson.getJSONArray("assets")) {
				if(obj instanceof JSONObject) {
					JSONObject assetObj = (JSONObject)obj;
					if(!assetObj.getString("name").endsWith(".jar")) {
						continue;
					}
					FileUtil.downloadFile(new URL(assetObj.getString("browser_download_url")), Paths.get(MCPPaths.UPDATE_JAR));
					break;
				}
			}
			log("Press ENTER key to continue");
			// FIXME
			//MCP.input.nextLine();
			Path jarPath = Paths.get(MCP.class
					  .getProtectionDomain()
					  .getCodeSource()
					  .getLocation()
					  .toURI());
			if(!Files.isDirectory(jarPath)) {
				String[] cmd = new String[] {
					Util.getJava(),
					"-cp",
					MCPPaths.UPDATE_JAR,
					"org.mcphackers.mcp.Update",
					jarPath.toString()
				};
				Util.runCommand(cmd);
			}
			else {
				throw new IOException("Running from a folder! Aborting");
			}
		}
	}

	@Override
	public String getName() {
		return "Update";
	}
}
