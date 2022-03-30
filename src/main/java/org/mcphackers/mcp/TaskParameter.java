package org.mcphackers.mcp;

public enum TaskParameter {

	//TODO
	DEBUG("Display additional info", true),
	SIDE("Perform operation for side"),
	PATCHES("Apply patches", true),
	IGNORED_PACKAGES("Ignored packages"),
	INDENTION_STRING("Indention string"),
	FULL_BUILD("Full build", true),
	RUN_BUILD("Run build", true),
	RUN_ARGS("Run arguments"),
	SETUP_VERSION("Setup version"),
	SOURCE_VERSION("Set a specific source version"),
	TARGET_VERSION("Set a specific target version"),
	BOOT_CLASS_PATH("Set a specific bootstrap class path");
	
	public final String name;
	public boolean isToggle;
	
	TaskParameter(String name) {
		this.name = name;
		isToggle = false;
	}
	
	TaskParameter(String name, boolean toggle) {
		this(name);
		isToggle = toggle;
	}
}
