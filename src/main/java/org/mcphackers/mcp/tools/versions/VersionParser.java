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
import org.mcphackers.mcp.tools.versions.json.VersionMetadata;

public class VersionParser {

	//public static final String MAPPINGS_JSON = "https://mcphackers.github.io/versions/versions.json";
	public static final String MAPPINGS_JSON = "file:C:\\Users\\User\\Desktop\\versions.json";
	public static final VersionParser INSTANCE = new VersionParser();
	
	private List<VersionData> versions = new ArrayList<>();
	
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
				VersionData data = VersionData.from(j);
				if(data.resources != null) {
					versions.add(data);
				}
			}
			catch (Exception e) {
				// Catching exception will skip to the next version
				e.printStackTrace();
			}
		}
		versions.sort(new VersionSorter());
	}
	
	/**
	 * Returns version data from version id/name
	 * @param id
	 * @return VersionData
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
	
	public static class VersionData extends VersionMetadata {
		public String resources;
		
		public static VersionData from(JSONObject obj) {
			if(obj == null) {
				return null;
			}
			return new VersionData() {
				{
					id = obj.getString("id");
					time = obj.getString("time");
					releaseTime = obj.getString("releaseTime");
					type = obj.getString("type");
					url = obj.getString("url");
					resources = obj.optString("resources", null);
				}
			};
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
