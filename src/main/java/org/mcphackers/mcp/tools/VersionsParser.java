package org.mcphackers.mcp.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

public abstract class VersionsParser {
	
	private static final String jsonURL = "https://mcphackers.github.io/versions/versions.json";
	private static Exception cause = null;
	
	public static final JSONObject json = getJson();
	
	public static List<String> getVersionList() throws Exception {
		checkJson();
		List<String> verList = new ArrayList<>();
		Iterator<String> iterator = json.keys();
		iterator.forEachRemaining(verList::add);
		// TODO sort by date instead
		// Add date entry to json. Make a version to be the last one if there is no date entry
		verList.sort(Comparator.naturalOrder());
		return verList;
	}
	
	private static void checkJson() throws Exception {
		if(cause != null) {
			throw new Exception("Could not receive version list", cause);
		}
	}
	
	private static JSONObject getJson() {
		try {
			Path jsonPath = Paths.get("versions.json");
			if(Files.exists(jsonPath) && !Files.isDirectory(jsonPath)) {
				return Util.parseJSONFile(jsonPath);
			}
			else {
				InputStream in = new URL(jsonURL).openStream();
				return Util.parseJSONFile(in);
			}
		} catch (JSONException | IOException e) {
			cause = e;
		}
		return null;
	}

	public static int getProxyPort(String version) throws Exception {
		checkJson();
		return json.getJSONObject(version).getInt("proxy_port");
	}

	public static String getServerVersion(String version) throws Exception {
		checkJson();
		if(json.getJSONObject(version).has("server")) {
			return json.getJSONObject(version).getString("server");
		}
		return null;
	}

	public static boolean hasServer(String version) throws Exception {
		checkJson();
		//TODO Better null pointer handling
		return version == null || json.getJSONObject(version).has("server_url");
	}

	public static String getDownloadURL(String version, int side) throws Exception {
		checkJson();
		String url = side == 0 ? "client_url" : side == 1 ? "server_url" : null;
		if(json.getJSONObject(version).has(url)) {
			return json.getJSONObject(version).getString(url);
		}
		throw new JSONException("Could not get download link for " + (side == 0 ? "client" : "server"));
	}

	public static URL downloadVersion(String version) throws Exception {
		checkJson();
		if(json.getJSONObject(version).has("resources")) {
			return new URL("https://mcphackers.github.io/versions/" + json.getJSONObject(version).getString("resources"));
		}
		throw new JSONException("Could not get download link for mappings");
	}

	public static String setCurrentVersion(MCP mcp, String version) throws Exception {
		checkJson();
		if(!json.has(version)) {
			throw new Exception("Invalid version detected!");
		}
		try(BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, MCPPaths.VERSION))) {
			writer.write(version);
		}
		return version;
	}
}
