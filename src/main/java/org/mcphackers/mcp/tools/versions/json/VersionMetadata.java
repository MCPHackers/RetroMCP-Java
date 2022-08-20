package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class VersionMetadata {
	public String id;
	public String type;
	public String time;
	public String releaseTime;
	public String url;

	
	public static VersionMetadata from(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new VersionMetadata() {
			{
				id = obj.getString("id");
				time = obj.getString("time");
				releaseTime = obj.getString("releaseTime");
				type = obj.getString("type");
				url = obj.getString("url");
			}
		};
	}
}
