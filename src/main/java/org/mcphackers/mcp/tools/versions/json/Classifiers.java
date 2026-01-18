package org.mcphackers.mcp.tools.versions.json;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class Classifiers {
	public Map<String, Artifact> artifacts = new HashMap<>();

	public static Classifiers from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new Classifiers() {
			{
				for (String key : obj.keySet()) {
					artifacts.put(key, getArtifact(obj, key));
				}
			}
		};
	}

	private static Artifact getArtifact(JSONObject root, String name) {
		return Artifact.from(root.optJSONObject(name), name);
	}
}
