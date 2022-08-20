package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class DownloadLibrary {
	public Artifact artifact;
	public Classifiers classifiers;
	
	public static DownloadLibrary from(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new DownloadLibrary() {
			{
				artifact = Artifact.from(obj.optJSONObject("artifact"));
				classifiers = Classifiers.from(obj.optJSONObject("classifiers"));
			}
		};
	}
}
