package org.mcphackers.mcp.tools.versions.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.OS;
import org.mcphackers.mcp.tools.versions.IDownload;

public class DependDownload {
	public Downloads downloads;
	public String name;
	public String url;
	public List<Rule> rules = new ArrayList<>();

	public static DependDownload from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new DependDownload() {
			{
				name = obj.getString("name");
				url = obj.optString("url");
				downloads = Downloads.from(obj.optJSONObject("downloads"));
				JSONArray a = obj.optJSONArray("rules");
				if (a != null) {
					for (Object o : a) {
						rules.add(Rule.from((JSONObject) o));
					}
				}
			}
		};
	}

	private static String getLibPath(String name, String classifierSuffix) {
		String[] artifact = name.split(":");
		return artifact[0].replace('.', '/') + "/" + artifact[1] + "/" + artifact[2] + "/" + artifact[1] + "-" + artifact[2] + (classifierSuffix == null ? "" : ("-" + classifierSuffix)) + ".jar";
	}
	
	public String getArtifactURL(String artifactName) {
		Artifact artifact = getArtifact(artifactName);
		if(artifact == null) {
			return null;
		}
		if(artifact.url != null) {
			return artifact.url;
		}
		String urlBase = "https://libraries.minecraft.net/";
		if(url != null) {
			urlBase = url.codePointAt(url.length() - 1) == '/' ? url : url + "/";
		}
		return urlBase + getLibPath(name, artifact.name);
	}

	public IDownload getDownload(String artifactName) {
		final Artifact artifact = getArtifact(artifactName);
		if(artifact == null) {
			return null;
		}
		final String url = getArtifactURL(artifactName);
		final String path = getArtifactPath(artifactName);
		return new IDownload() {

			@Override
			public String downloadPath() {
				return path;
			}

			@Override
			public String downloadURL() {
				return url;
			}

			@Override
			public long downloadSize() {
				return artifact.size;
			}

			@Override
			public String downloadHash() {
				return artifact.sha1;
			}

			@Override
			public boolean verify() {
				return true;
			}
			
		};
	}
	
	public String getArtifactPath(String artifactName) {
		Artifact artifact = getArtifact(artifactName);
		if(artifact == null) {
			return null;
		}
		if(artifact.path != null) {
			return artifact.path;
		}
		return getLibPath(name, artifact.name);
	}

	private Artifact getArtifact(String name) {
		if(name == null) {
			return downloads == null ? null : downloads.artifact;
		}
		return (downloads == null || downloads.classifiers == null) ? null : downloads.classifiers.artifacts.get(name);
	}

	public String getNatives() {
		switch (OS.getOs()) {
			case windows:
				if (getArtifact("natives_windows") != null) {
					return "natives_windows";
				}
				break;
			case linux:
				if (getArtifact("natives_linux") != null) {
					return "natives_linux";
				}
				break;
			case osx:
				if (getArtifact("natives_osx") != null) {
					return "natives_osx";
				}
				if (getArtifact("natives_macos") != null) {
					return "natives_macos";
				}
				break;
			default:
				break;
		}
		return null;
	}
}
