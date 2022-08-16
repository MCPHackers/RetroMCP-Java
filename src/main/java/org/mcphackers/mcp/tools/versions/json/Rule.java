package org.mcphackers.mcp.tools.versions.json;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.mcphackers.mcp.tools.OS;

public class Rule {
	public Action action;
	public OSName os;
	//public Map<String, Boolean> features;
	
	public static Rule from(JSONObject obj) {
		if(obj == null) { 
			return null;
		}
		return new Rule() {
			{
				action = Action.valueOf(obj.getString("action"));
				os = OSName.from(obj.getJSONObject("os"));
			}
		};
	}
	
	public static class OSName {
		public OS name;
		public String version;
		
		public static OSName from(JSONObject obj) {
			if(obj == null) { 
				return null;
			}
			return new OSName() {
				{
					name = OS.valueOf(obj.getString("name"));
					version = obj.optString("version");
				}
			};
		}

		public boolean equalsOS(OS os) {
			if(this.name != null && this.name != os) {
				return false;
			} else {
				if(this.version != null) {
					try {
						Pattern pattern = Pattern.compile(this.version);
						Matcher matcher = pattern.matcher(System.getProperty("os.version"));
						if(!matcher.matches()) {
							return false;
						}
					} catch (Throwable var3) {
					}
				}

				return true;
			}
		}
	}

	public Rule.Action getAppliedAction() {
		return this.os != null && !this.os.equalsOS(OS.getOs()) ? null : this.action;
	}

	public static boolean apply(List<Rule> rules) {
		if(rules != null && !rules.isEmpty()) {
			Rule.Action lastAction = Rule.Action.disallow;

			for(Rule compatibilityRule : rules) {
				Rule.Action action = compatibilityRule.getAppliedAction();
				if(action != null) {
					lastAction = action;
				}
			}
			return lastAction == Rule.Action.allow;
		} else {
			return true;
		}
	}


	public static enum Action {
		allow,
		disallow;
	}
}
