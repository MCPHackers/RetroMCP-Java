package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class DependDownload {
	public DownloadLibrary downloads;
	public String name;
	//public List<Rule> rules;
	
	public static DependDownload from(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new DependDownload() {
			{
				name = obj.getString("name");
				downloads = DownloadLibrary.from(obj.getJSONObject("downloads"));
			}
		};
	}
}
