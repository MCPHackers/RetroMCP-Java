package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;
import org.mcphackers.mcp.tools.versions.IDownload;

public class Download implements IDownload {

	public String sha1;
	public String url;
	public long size;

	public static Download from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new Download() {
			{
				sha1 = obj.getString("sha1");
				url = obj.getString("url");
				size = obj.getLong("size");
			}
		};
	}

	@Override
	public String name() {
		return url;
	}

	@Override
	public long size() {
		return size;
	}
}
