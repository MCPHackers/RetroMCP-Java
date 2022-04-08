package org.mcphackers.mcp;

import java.util.HashMap;
import java.util.Map;

public class Options {
	
	public Options() {
		resetDefaults();
	}
	
	private Map<TaskParameter, Object> options = new HashMap<>();

	public void setDefault(TaskParameter param) {
		Object value = null;
		switch (param) {
		case DEBUG:
			value = false;
			break;
		case SRC_CLEANUP:
			value = false;
			break;
		case PATCHES:
			value = true;
			break;
		case IGNORED_PACKAGES:
			value = new String[] {"paulscode", "com/jcraft", "isom"};
			break;
		case INDENTION_STRING:
			value = "\t";
			break;
		case FULL_BUILD:
			value = false;
			break;
		case RUN_BUILD:
			value = false;
			break;
		case RUN_ARGS:
			value = new String[0];
			break;
		case SETUP_VERSION:
			value = null;
			break;
		case SOURCE_VERSION:
			value = -1;
			break;
		case TARGET_VERSION:
			value = -1;
			break;
		case BOOT_CLASS_PATH:
			value = null;
			break;
		default:
			break;
		}
		options.put(param, value);
	}
	
	public void setParameter(TaskParameter param, Object value) throws IllegalArgumentException {
		if(value == null || param.type.isInstance(value)) {
			options.put(param, value);
		}
		else {
			throw new IllegalArgumentException("Type mismatch");
		}
	}

	public boolean getBooleanParameter(TaskParameter param) throws IllegalArgumentException {
		Object value = options.get(param);
		if(value instanceof Boolean) {
			return (Boolean)value;
		}
		throw new IllegalArgumentException("Type mismatch");
	}

	public String[] getStringArrayParameter(TaskParameter param) throws IllegalArgumentException {
		Object value = options.get(param);
		if(value == null || value instanceof String[]) {
			return (String[])value;
		}
		throw new IllegalArgumentException("Type mismatch");
	}

	public String getStringParameter(TaskParameter param) throws IllegalArgumentException {
		Object value = options.get(param);
		if(value == null || value instanceof String) {
			return (String)value;
		}
		throw new IllegalArgumentException("Type mismatch");
	}

	public int getIntParameter(TaskParameter param) throws IllegalArgumentException {
		Object value = options.get(param);
		if(value instanceof Integer) {
			return (Integer)value;
		}
		throw new IllegalArgumentException("Type mismatch");
	}

	public void resetDefaults() {
		for(TaskParameter param : TaskParameter.values()) {
			setDefault(param);
		}
	}
}
