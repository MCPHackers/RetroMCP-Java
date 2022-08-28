package org.mcphackers.mcp.tools.versions.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class DependDownload {
	public DownloadLibrary downloads;
	public String name;
	public List<Rule> rules = new ArrayList<>();
	
	public static DependDownload from(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new DependDownload() {
			{
				name = obj.getString("name");
				downloads = DownloadLibrary.from(obj.getJSONObject("downloads"));
				JSONArray a = obj.optJSONArray("rules");
				if(a != null) {
					for(Object o : a) {
						rules.add(Rule.from((JSONObject)o));
					}
				}
			}
		};
	}
}
