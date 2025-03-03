package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;
import org.mcphackers.mcp.tools.versions.IDownload;

public class Artifact implements IDownload {
	public String path;
	public String name;
	public String sha1;
	public String url;
	public long size;

	public static Artifact from(JSONObject obj, String nameStr) {
		if (obj == null) {
			return null;
		}
		return new Artifact() {
			{
				name = nameStr;
				path = obj.optString("path", null);
				sha1 = obj.optString("sha1", null);
				url = obj.optString("url", null);
				size = obj.optLong("size");
			}
		};
	}

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
		return size;
	}

	@Override
	public String downloadHash() {
		return sha1;
	}

	@Override
	public boolean verify() {
		return true;
	}
}
