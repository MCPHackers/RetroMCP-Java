package org.mcphackers.mcp.tools.versions.json;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mcphackers.mcp.tools.versions.IDownload;

public class AssetIndex {
	public Map<String, Asset> objects = new LinkedHashMap<>();
	public boolean virtual;
	public boolean map_to_resources;

	public static AssetIndex from(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new AssetIndex() {
			{
				virtual = obj.optBoolean("virtual");
				map_to_resources = obj.optBoolean("map_to_resources");
				JSONObject obj2 = obj.optJSONObject("objects");
				if (obj2 != null) {
					for (String s : obj2.keySet()) {
						objects.put(s, assetFrom(obj2.getJSONObject(s)));
					}
				}
			}
		};
	}

	public Asset assetFrom(JSONObject obj) {
		if (obj == null) {
			return null;
		}
		return new Asset() {
			{
				hash = obj.getString("hash");
				size = obj.getLong("size");
				url = obj.optString("url", null);
				// reconstruct = obj.optBoolean("reconstruct");
				// compressedHash = obj.optString("compressedHash", null);
				// compressedSize = obj.optLong("compressedSize");
			}
		};
	}

	public static class Asset implements IDownload {
		public String hash;
		public String url;
		public long size;
		// public boolean reconstruct;
		// public String compressedHash;
		// public long compressedSize;

		@Override
		public String downloadURL() {
			return url != null ? url : "https://resources.download.minecraft.net/" + downloadPath();
		}

		@Override
		public long downloadSize() {
			return size;
		}

		@Override
		public String downloadPath() {
			return hash.substring(0, 2) + "/" + hash;
		}

		@Override
		public String downloadHash() {
			return hash;
		}

		@Override
		public boolean verify() {
			return true;
		}
	}
}
