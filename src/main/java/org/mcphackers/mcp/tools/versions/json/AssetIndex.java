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
		if(obj == null) {
			return null;
		}
		return new AssetIndex() {
			{
				virtual = obj.optBoolean("virtual");
				map_to_resources = obj.optBoolean("map_to_resources");
				JSONObject obj2 = obj.optJSONObject("objects");
				if(obj2 != null) {
					for(String s : obj2.keySet()) {
						objects.put(s, assetFrom(obj2.getJSONObject(s)));
					}
				}
			}
		};
	}
	
	public Asset assetFrom(JSONObject obj) {
		if(obj == null) {
			return null;
		}
		return new Asset() {
			{
				hash = obj.getString("hash");
				size = obj.getLong("size");
				reconstruct = obj.getBoolean("reconstruct");
				compressedHash = obj.optString("compressedHash", null);
				compressedSize = obj.optLong("compressedSize");
			}
		};
	}

	public class Asset implements IDownload {
		public String hash;
		public long size;
		public boolean reconstruct;
		public String compressedHash;
		public long compressedSize;

		@Override
		public String name() {
			return "";
		}

		@Override
		public long size() {
			return compressedSize;
		}
	}
}
