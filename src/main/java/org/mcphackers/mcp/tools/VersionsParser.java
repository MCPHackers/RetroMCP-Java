package org.mcphackers.mcp.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class VersionsParser {
	
	private static final String jsonURL = "https://mcphackers.github.io/versions/versions.json";
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

	public static int getProxyPort(String chosenVersion) throws Exception {
		checkJson();
		return json.getJSONObject(chosenVersion).getInt("proxy_port");
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

	public static URL downloadVersion(String chosenVersion) throws Exception {
		checkJson();
		if(json.getJSONObject(chosenVersion).has("resources")) {
			return new URI("https://mcphackers.github.io/versions/" + json.getJSONObject(chosenVersion).getString("resources")).toURL();
		}
		return null;
	}
}
