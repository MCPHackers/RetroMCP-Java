package org.mcphackers.mcp.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCPConfig;

public class VersionsParser {
	
	private static final String jsonURL = "https://raw.githubusercontent.com/MCPHackers/Vault/main/versions.json";
	private static final String contentsURL = "https://api.github.com/repos/MCPHackers/Vault/contents";
	private static Exception cause = null;
	
	public static final JSONObject json = getJson();
	
	public static List<String> getVersionList() throws Exception {
		checkJson();
		List<String> verList = new ArrayList<String>();
		Iterator<String> iterator = json.keys();
		iterator.forEachRemaining(verList::add);
		return verList;
	}
	
	private static void checkJson() throws Exception {
		if(cause != null) {
			throw new Exception("Could not receive version list", cause);
		}
	}
	
	private static JSONObject getJson() {
		try {
			InputStream in = new URL(jsonURL).openStream();
			return Util.parseJSONFile(in);
		} catch (JSONException | IOException e) {
			cause = e;
		}
		return null;
	}

	public static int getWorkspace(String chosenVersion) throws Exception {
		checkJson();
		return json.getJSONObject(chosenVersion).getInt("workspace_version");
	}

	public static String getServerVersion(String chosenVersion) throws Exception {
		checkJson();
		if(json.getJSONObject(chosenVersion).has("server")) {
			return json.getJSONObject(chosenVersion).getString("server");
		}
		return null;
	}

	public static boolean hasServer(String chosenVersion) throws Exception {
		checkJson();
		return json.getJSONObject(chosenVersion).has("server");
	}

	public static String getDownloadURL(String chosenVersion, int side) throws Exception {
		checkJson();
		String url = side == 0 ? "client_url" : side == 1 ? "server_url" : null;
		if(json.getJSONObject(chosenVersion).has(url)) {
			return json.getJSONObject(chosenVersion).getString(url);
		}
		return null;
	}

	public static void downloadVersion(String chosenVersion) throws Exception {
		InputStream in = new URL(contentsURL).openStream();
		JSONArray json = Util.parseJSONArray(in);
		for(Object object : json) {
			if(object instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject)object;
				if(jsonObject.getString("type").equals("dir") && jsonObject.getString("name").equals(chosenVersion)) {
					FileUtil.downloadGitDir(new URI(jsonObject.getString("url")).toURL(), Paths.get(MCPConfig.CONF));
					break;
				}
			}
		}
	}
}
