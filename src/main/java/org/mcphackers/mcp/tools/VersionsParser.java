package org.mcphackers.mcp.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
import org.mcphackers.mcp.MCPPaths;

public class VersionsParser {
	
	private static final String jsonURL = "https://mcphackers.github.io/versions/versions.json";
	private static String currentVersion = "unknown";
	private static Exception cause = null;
	
	public static final JSONObject json = getJson();
	
	public static List<String> getVersionList() throws Exception {
		checkJson();
		List<String> verList = new ArrayList<>();
		Iterator<String> iterator = json.keys();
		iterator.forEachRemaining(verList::add);
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

	public static int getProxyPort() throws Exception {
		checkJson();
		return json.getJSONObject(currentVersion).getInt("proxy_port");
	}

	public static String getServerVersion() throws Exception {
		checkJson();
		if(json.getJSONObject(currentVersion).has("server")) {
			return json.getJSONObject(currentVersion).getString("server");
		}
		return null;
	}

	public static boolean hasServer() throws Exception {
		checkJson();
		return json.getJSONObject(currentVersion).has("server_url");
	}

	public static String getDownloadURL(int side) throws Exception {
		checkJson();
		String url = side == 0 ? "client_url" : side == 1 ? "server_url" : null;
		if(json.getJSONObject(currentVersion).has(url)) {
			return json.getJSONObject(currentVersion).getString(url);
		}
		throw new JSONException("Could not get download link for " + (side == 0 ? "client" : "server"));
	}

	public static URL downloadVersion() throws Exception {
		checkJson();
		if(json.getJSONObject(currentVersion).has("resources")) {
			return new URL("https://mcphackers.github.io/versions/" + json.getJSONObject(currentVersion).getString("resources"));
		}
		throw new JSONException("Could not get download link for mappings");
	}

	public static void setCurrentVersion(String version) throws Exception {
		if(!json.has(version)) {
			throw new Exception("Invalid version detected!");
		}
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(MCPPaths.VERSION))) {
			writer.write(version);
		}
		currentVersion = version;
	}

	public static String getCurrentVersion() {
		return currentVersion;
	}
}
