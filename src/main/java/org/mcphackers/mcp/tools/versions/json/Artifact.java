package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class Artifact extends Download {
	public String path;
	
	public static Artifact from(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new Artifact() {
			{
				path = obj.getString("path");
				sha1 = obj.getString("sha1");
				url = obj.getString("url");
				size = obj.getLong("size");
			}
		};
	}
	
	@Override
	public String name() {
		return path;
	}
}
