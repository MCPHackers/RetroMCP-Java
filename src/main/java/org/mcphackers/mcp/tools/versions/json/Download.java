package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class Download {
	
	public String sha1;
	public String url;
	public long size;
	
	public static Download from(JSONObject obj) {
		if(obj == null) { 
			return null;
		}
		return new Download() {
			{
				sha1 = obj.getString("sha1");
				url = obj.getString("url");
				size = obj.getLong("size");
			}
		};
	}
}
