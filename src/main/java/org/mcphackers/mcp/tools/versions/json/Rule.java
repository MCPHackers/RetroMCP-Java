package org.mcphackers.mcp.tools.versions.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.mcphackers.mcp.tools.OS;

public class Rule {
	public Action action;
	public OSInfo os;
	public Map<String, Boolean> features = new HashMap<>();
	
	public static Rule from(JSONObject obj) {
		if(obj == null) { 
			return null;
		}
		return new Rule() {
			{
				action = Action.valueOf(obj.getString("action"));
				os = OSInfo.from(obj.optJSONObject("os"));
				JSONObject obj2 = obj.optJSONObject("features");
				if(obj2 != null) {
					for(String s : obj2.keySet()) {
						features.put(s, obj2.getBoolean(s));
					}
				}
			}
		};
	}
	
	public static class OSInfo {
		public OS name;
		public String version;
		
		public static OSInfo from(JSONObject obj) {
			if(obj == null) { 
				return null;
			}
			return new OSInfo() {
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

	public Rule.Action getAppliedAction(List<String> featuresList) {
		boolean featuresMatch = true;
		for(Entry<String, Boolean> entry : features.entrySet()) {
			featuresMatch = featuresList.contains(entry.getKey()) == entry.getValue();
			if(!featuresMatch) return Rule.Action.disallow;
		}
		return this.os != null && !this.os.equalsOS(OS.getOs()) ? null : this.action;
	}
	
	public static boolean apply(List<Rule> rules) {
		return apply(rules, Collections.emptyList());
	}

	public static boolean apply(List<Rule> rules, List<String> features) {
		if(rules != null && !rules.isEmpty()) {
			Rule.Action lastAction = Rule.Action.disallow;

			for(Rule compatibilityRule : rules) {
				Rule.Action action = compatibilityRule.getAppliedAction(features);
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
