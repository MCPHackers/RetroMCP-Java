package org.mcphackers.mcp.api.util;

public class Config {
	class Minecraft {
		private boolean fullBuild = false;
		private String ignoredPackages = "paulscode, com/jcraft, de/jarnbjo, isom";
		private boolean enablePatches = true;
		private boolean isObfuscationEnabled = false;
		private boolean enableOverrides = false;

		public boolean isFullBuildEnabled() {
			return this.fullBuild;
		}

		public String getIgnoredPackages() {
			return this.ignoredPackages;
		}

		public boolean isPatchesEnabled() {
			return this.enablePatches;
		}

		public boolean isObfuscationEnabled() {
			return this.isObfuscationEnabled;
		}

		public boolean isOverridesEnabled() {
			return this.enableOverrides;
		}
	}
}
