package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class Downloads {
	public Artifact artifact;
	public Classifiers classifiers;

	public static Downloads from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new Downloads() {
			{
				artifact = Artifact.from(obj.optJSONObject("artifact"), null);
				classifiers = Classifiers.from(obj.optJSONObject("classifiers"));
			}
		};
	}
}
