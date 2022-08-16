package org.mcphackers.mcp.tools.versions.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class Version {
	
	public AssetIndex assetIndex;
	public String assets;
	public VersionDownloads downloads;
	public String id;
	public String time;
	public String releaseTime;
	public String type;
	public List<DependDownload> libraries;
	//public Logging logging;
	public String mainClass;
	public String minecraftArguments;
	public Arguments arguments;
	
	public static Version from(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new Version() {
			{
				assetIndex = AssetIndex.from(obj.getJSONObject("assetIndex"));
				assets = obj.getString("assets");
				downloads = VersionDownloads.from(obj.getJSONObject("downloads"));
				id = obj.getString("id");
				time = obj.getString("time");
				releaseTime = obj.getString("releaseTime");
				type = obj.getString("type");
				libraries = new ArrayList<>();
				for(Object o : obj.getJSONArray("libraries")) {
					if(o instanceof JSONObject) {
						libraries.add(DependDownload.from((JSONObject)o));
					}
				}
			}
		};
	}
	
	public static class VersionDownloads {

		public Download client;
		public Download server;
		public Download windows_server;
		public Download client_mappings;
		public Download server_mappings;
		
		public static VersionDownloads from(JSONObject obj) {
			if(obj == null) {
				return null;
			}
			return new VersionDownloads() {
				{
					client = Download.from(obj.optJSONObject("client"));
					server = Download.from(obj.optJSONObject("server"));
					windows_server = Download.from(obj.optJSONObject("windows_server"));
					client_mappings = Download.from(obj.optJSONObject("client_mappings"));
					server_mappings = Download.from(obj.optJSONObject("server_mappings"));
				}
			};
		}
	}
	
	public static class Arguments {
		public Object[] game;
		public Object[] jvm;
		
		public static Arguments from(JSONObject obj) {
			if(obj == null) {
				return null;
			}
			return new Arguments() {
				{
					game = JSONUtil.getArray(obj.getJSONArray("game"));
					jvm = JSONUtil.getArray(obj.getJSONArray("jvm"));
				}
			};
		}
	}

}
