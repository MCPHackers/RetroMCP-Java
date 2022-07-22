package org.mcphackers.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.Util;

public class Options {

	private final Map<TaskParameter, Object> options = new HashMap<>();
	public Path saveFile;
	public Side side = Side.ANY;
	public Language lang;
	
	public Options() {
		for(TaskParameter param : TaskParameter.values()) {
			setDefault(param);
		}
	}
	
	public Options(Path file) {
		this();
		saveFile = file;
		if(Files.exists(saveFile)) {
			load(saveFile);
			save();
		}
	}
	
	private void load(Path file) {
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				int sep = line.indexOf("=");
				if(sep >= 0) {
					String key = line.substring(0, sep);
					String value = sep == line.length() ? "" : line.substring(sep + 1);
					if(key.equals(TaskParameter.SIDE.name)) {
						try {
							side = Side.valueOf(value);
						} catch (IllegalArgumentException e) {}
					}
					else if(key.equals("lang")) {
						try {
							lang = Language.valueOf(value);
						} catch (IllegalArgumentException e) {}
					}
					else {
						safeSetParameter(TaskMode.nameToParamMap.get(key), value);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void save() {
		if(saveFile != null) {
			try (BufferedWriter writer = Files.newBufferedWriter(saveFile)) {
				writer.append(TaskParameter.SIDE.name).append('=').append(side.name()).append('\n');
				writer.append("lang").append('=').append(MCP.TRANSLATOR.currentLang.name()).append('\n');
				for(Entry<TaskParameter, Object> entry : options.entrySet()) {
					if(entry.getValue() != null) {
						writer.append(entry.getKey().name).append('=').append(getParameter(entry.getKey()).toString()).append(String.valueOf('\n'));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void resetDefaults() {
		side = Side.ANY;
		for(TaskParameter param : TaskParameter.values()) {
			setDefault(param);
		}
		save();
	}

	public void setDefault(TaskParameter param) {
		Object value = null;
		switch (param) {
		case DEBUG:
		case SRC_CLEANUP:
		case FULL_BUILD:
		case RUN_BUILD:
		case OBFUSCATION:
		case DECOMPILE_OVERRIDE:
			value = false;
			break;
		case PATCHES:
			value = true;
			break;
		case IGNORED_PACKAGES:
			value = new String[] {"paulscode", "com/jcraft", "de/jarnbjo", "isom"};
			break;
		case INDENTATION_STRING:
			value = "\t";
			break;
		case RUN_ARGS:
			value = new String[] {"-Xms1024M", "-Xmx1024M", "-Djava.util.Arrays.useLegacyMergeSort=true"};
			break;
		case SOURCE_VERSION:
		case TARGET_VERSION:
			value = -1;
			break;
		case JAVA_HOME:
			value = "";
			break;
		case SIDE:
			side = Side.ANY;
			value = side.index;
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
		save();
	}

	public Object getParameter(TaskParameter param) {
		Object value = options.get(param);
		if(value instanceof String[]) {
			return String.join(", ", (String[])value);
		}
		return value;
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

	public boolean safeSetParameter(TaskParameter param, String value) {
		if(param == null) {
			return false;
		}
		if(param.type == Integer.class) {
			try {
				int valueInt = Integer.parseInt(value);
				setParameter(param, valueInt);
				return true;
			}
			catch (NumberFormatException ignored) {}
			catch (IllegalArgumentException e) {}
		}
		else if(param.type == Boolean.class) {
			if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
				try {
					boolean valueBoolean = Boolean.parseBoolean(value);
					setParameter(param, valueBoolean);
					return true;
				}
				catch (IllegalArgumentException e) {}
			}
		}
		else if(param.type == String[].class) {
			try {
				String[] values = value.split(",");
				for(int i2 = 0 ; i2 < values.length; i2++) {
					values[i2] = Util.convertFromEscapedString(values[i2]).trim();
				}
				setParameter(param, values);
				return true;
			}
			catch (IllegalArgumentException e) {}
		}
		else if(param.type == String.class) {
			try {
				value = Util.convertFromEscapedString(value);
				setParameter(param, value);
				return true;
			}
			catch (IllegalArgumentException e) {}
		}
		return false;
	}
}
