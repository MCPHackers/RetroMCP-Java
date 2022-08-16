package org.mcphackers.mcp.tools.versions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.json.Version;

public class VersionParser {

	//public static final String MAPPINGS_JSON = "https://mcphackers.github.io/versions/versions.json";
	public static final String MAPPINGS_JSON = "file:C:\\Users\\User\\Desktop\\versions.json";
	public static final VersionParser INSTANCE = new VersionParser();
	
	public List<VersionData> versions = new ArrayList<>();
	
	public VersionParser() {
		JSONArray json;
		try {
			json = getJson();
		}
		catch (Exception e) {
			e.printStackTrace();
			return; // Couldn't init json
		}
		for(Object o : json) {
			if(!(o instanceof JSONObject)) {
				continue;
			}
			try {
				JSONObject j = (JSONObject)o;
				Version ver = Version.from(Util.parseJSON(new URL(j.getString("version")).openStream()));
				versions.add(new VersionData(
						ver.id,
						ver.releaseTime,
						ver.type,
						j.getString("version"),
						j.getString("resources"),
						ver.downloads.server != null
						));
			}
			catch (Exception e) {
				// Catching exception will skip to the next version
				e.printStackTrace();
			}
		}
		versions.sort(new VersionSorter());
		System.gc();
	}
	
	/**
	 * Returns version data from version id/name
	 * @param id
	 * @return Cached VersionData
	 */
	public VersionData getVersion(String id) {
		for(VersionData data : versions) {
			if(data.id.equals(id)) {
				return data;
			}
		}
		return null;
	}
	/**
	 * @return All cached VersionData
	 */
	public List<VersionData> getVersions() {
		return versions;
	}
	
	public static class VersionData {
		public String id;
		public String type;
		public String releaseTime;
		public String versionJson;
		public String resources;
		public boolean hasServer;
		
		public VersionData(String name, String time, String type1, String ver, String res, boolean server) {
			id = name;
			releaseTime = time;
			type = type1;
			versionJson = ver;
			resources = res;
			hasServer = server;
		}
		
		public boolean hasServer() {
			return hasServer;
		}
		
		public String toString() {
			String typ;
			String ver;
			if(id.startsWith("rd") && "old_alpha".equals(type)) {
				typ = "Pre-Classic";
				ver = id;
			}
			else if(id.startsWith("c") && "old_alpha".equals(type)) {
				typ = "Classic";
				ver = id.substring(1);
			}
			else if(id.startsWith("inf-")) {
				typ = "Infdev";
				ver = id.substring(4);
			}
			else if(id.startsWith("in-")) {
				typ = "Indev";
				ver = id.substring(3);
			}
			else if(id.startsWith("a") && "old_alpha".equals(type)) {
				typ = "Alpha";
				ver = id.substring(1);
			}
			else if(id.startsWith("b")) {
				typ = "Beta";
				ver = id.substring(1);
			}
			else {
				typ = type.substring(0,1).toUpperCase() + type.substring(1);
				ver = id;
			}
			return typ + " " + ver;
		}
	}
	
	/**
	 * Sorts versions by date
	 */
	public static class VersionSorter implements Comparator<VersionData> {

		public int compare(VersionData t1, VersionData t2) {
			try {
				Instant i1 = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(t1.releaseTime));
				Instant i2 = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(t2.releaseTime));
				return i2.compareTo(i1);
			}
			catch (Exception e) {
				return -1;
			}
		}
	}
	
	private static JSONArray getJson() throws IOException {
		URLConnection connect = new URL(MAPPINGS_JSON).openConnection();
		connect.setConnectTimeout(30000);
		InputStream in = connect.getInputStream();
		return Util.parseJSONArray(in);
	}
	
}
