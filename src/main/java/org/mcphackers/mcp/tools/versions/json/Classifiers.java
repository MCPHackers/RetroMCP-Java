package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;
import org.mcphackers.mcp.tools.OS;

public class Classifiers {
	public Artifact javadoc;
	public Artifact natives_linux;
	public Artifact natives_macos;
	public Artifact natives_osx;
	public Artifact natives_windows;
	public Artifact sources;

	public static Classifiers from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new Classifiers() {
			{
				javadoc = Artifact.from(obj.optJSONObject("javadoc"));
				natives_linux = Artifact.from(obj.optJSONObject("natives-linux"));
				natives_macos = Artifact.from(obj.optJSONObject("natives-macos"));
				natives_osx = Artifact.from(obj.optJSONObject("natives-osx"));
				natives_windows = Artifact.from(obj.optJSONObject("natives-windows"));
				sources = Artifact.from(obj.optJSONObject("sources"));
			}
		};
	}

	public Artifact getNatives() {
		switch (OS.getOs()) {
			case windows:
				return natives_windows;
			case linux:
				return natives_linux;
			case osx:
				if (natives_osx != null) {
					return natives_osx;
				}
				return natives_macos;
			default:
				return null;
		}
	}
}
