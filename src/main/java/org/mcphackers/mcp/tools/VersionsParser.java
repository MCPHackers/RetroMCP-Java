package org.mcphackers.mcp.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task.Side;

public abstract class VersionsParser {

	private static String versionsURL = "https://mcphackers.github.io/versions/";
	private static Exception cause = null;
	
	public static final JSONObject json = getJson();
	
	public static class VersionSorter implements Comparator<String> {

		public int compare(String t1, String t2) {
			try {
				if(!json.getJSONObject(t1).has("client_timestamp")) {
					return 1;
				}
				if(!json.getJSONObject(t2).has("client_timestamp")) {
					return -1;
				}
				Instant i1 = Instant.parse(json.getJSONObject(t1).getString("client_timestamp"));
				Instant i2 = Instant.parse(json.getJSONObject(t2).getString("client_timestamp"));
				return i2.compareTo(i1);
			}
			catch (Exception e) {
				return -1;
			}
		}
	}
	
	public static List<String> getVersionList() throws Exception {
		checkJson();
		List<String> verList = new ArrayList<>();
		Iterator<String> iterator = json.keys();
		iterator.forEachRemaining(verList::add);
		verList.sort(new VersionSorter());
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
			String jsonURL = versionsURL + "versions.json";
			if(Files.exists(jsonPath) && !Files.isDirectory(jsonPath)) {
				JSONObject jsonSource = Util.parseJSONFile(jsonPath);
				if(jsonSource.has("source")) {
					String src = jsonSource.getString("source");
					if(!src.endsWith("/") && !src.endsWith("\\")) {
						src += "/";
					}
					if(!src.startsWith("https:") && !src.startsWith("http:") && !src.startsWith("file:")) {
						src = "file:" + src;
					}
					versionsURL = src;
					jsonURL = src + "versions.json";
				}
			}
			URLConnection connect = new URL(jsonURL).openConnection();
			connect.setConnectTimeout(30000);
			InputStream in = connect.getInputStream();
			return Util.parseJSONFile(in);
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
		return json.getJSONObject(version).has("server_url");
	}

	public static String getDownloadURL(String version, Side side) throws Exception {
		checkJson();
		String url = side == Side.CLIENT ? "client_url" : side == Side.SERVER ? "server_url" : null;
		if(json.getJSONObject(version).has(url)) {
			return json.getJSONObject(version).getString(url);
		}
		throw new JSONException("Could not get download link for " + side.name.toLowerCase());
	}

	public static URL downloadVersion(String version) throws Exception {
		checkJson();
		if(json.getJSONObject(version).has("resources")) {
			return new URL(versionsURL + json.getJSONObject(version).getString("resources"));
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
