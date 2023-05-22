package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONObject;

public class AssetIndexMeta {
	public String id;
	public String sha1;
	public long size;
	public long totalSize;
	public String url;

	public static AssetIndexMeta from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new AssetIndexMeta() {
			{
				id = obj.getString("id");
				sha1 = obj.getString("sha1");
				size = obj.getLong("size");
				totalSize = obj.getLong("totalSize");
				url = obj.getString("url");
			}
		};
	}
}
