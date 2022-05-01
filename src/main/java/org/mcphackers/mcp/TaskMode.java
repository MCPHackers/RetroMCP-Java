package org.mcphackers.mcp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.mcp.tasks.*;
import org.mcphackers.mcp.tasks.Task.Side;

public class TaskMode {
	public static final List<TaskMode> registeredTasks = new ArrayList<>();
	
	public static final Map<String, TaskParameter> nameToParamMap = new HashMap<>();
	
	public static TaskMode HELP = new TaskMode("help", "Help", "Displays command usage", null);
	public static TaskMode DECOMPILE = new TaskMode("decompile", "Decompile", "Start decompiling Minecraft", TaskDecompile.class, new TaskParameter[]{
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.JAVA_HOME,
			TaskParameter.IGNORED_PACKAGES,
			TaskParameter.INDENTION_STRING,
			TaskParameter.PATCHES,
			TaskParameter.SIDE
			});
	public static TaskMode RECOMPILE = new TaskMode("recompile", "Recompile", "Recompile Minecraft sources", TaskRecompile.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.JAVA_HOME,
			TaskParameter.SIDE
			});
	public static TaskMode REOBFUSCATE = new TaskMode("reobfuscate", "Reobfuscate", "Reobfuscate Minecraft classes", TaskReobfuscate.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.JAVA_HOME,
			TaskParameter.SIDE
			});
	public static TaskMode UPDATE_MD5 = new TaskMode("updatemd5", "Update MD5 Hashes", "Update md5 hash tables used for reobfuscation", TaskUpdateMD5.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.JAVA_HOME,
			TaskParameter.SIDE
			});
	public static TaskMode UPDATE_MCP = new TaskMode("updatemcp", "Update", "Download an update if available", TaskDownloadUpdate.class);

	public static TaskMode SETUP = new TaskMode("setup", "Setup", "Choose a version to setup", TaskSetup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			});
	public static TaskMode CLEANUP = new TaskMode("cleanup", "Cleanup", "Delete all source and class folders", TaskCleanup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SRC_CLEANUP
			});
	public static TaskMode START = new TaskMode("start", "Start", "Runs the client or the server from compiled classes", TaskRun.class, new TaskParameter[] {
			TaskParameter.RUN_BUILD
			});
	public static TaskMode BUILD = new TaskMode("build", "Build", "Builds the final jar or zip", TaskBuild.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.JAVA_HOME,
			TaskParameter.FULL_BUILD,
			TaskParameter.SIDE
			});
	public static TaskMode CREATE_PATCH = new TaskMode("createpatch", "Create patch", "Creates patch", TaskCreatePatch.class);
	public static TaskMode EXIT = new TaskMode("exit", "Exit", "Exit the program", null);
	
	private final String name;
	private final String fullName;
	private final String desc;
	public final Class<? extends Task> taskClass;
	public TaskParameter[] params = new TaskParameter[] {};
	
	public TaskMode(String name, String fullName, String desc, Class<? extends Task> taskClass) {
		this.name = name;
		this.fullName = fullName;
		this.desc = desc;
		this.taskClass = taskClass;
		registeredTasks.add(this);
	}
	
	public TaskMode(String name, String fullName, String desc, Class<? extends Task> taskClass, TaskParameter[] params) {
		this(name, fullName, desc, taskClass);
		this.params = params;
	}
	
	public TaskMode(String name, String fullName, Class<? extends Task> taskClass, TaskParameter[] params) {
		this(name, fullName, "No description provided", taskClass, params);
	}
	
	public TaskMode(String name, String fullName, Class<? extends Task> taskClass) {
		this(name, fullName, "No description provided", taskClass);
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public List<Side> allowedSides() {
		List<Side> sides = new ArrayList<>();
		sides.add(Side.CLIENT);
		sides.add(Side.SERVER);
		return sides;
	}
	
	public List<Task> getTasks(MCP mcp) {
		List<Task> tasks = new ArrayList<>();
		if(taskClass != null) {
			Constructor<? extends Task> constructor;
			try {
				constructor = taskClass.getConstructor(Side.class, MCP.class);
				try {
					for(Side side : allowedSides()) {
						tasks.add(constructor.newInstance(side, mcp));
					}
				} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
					e.printStackTrace();
				}
			} catch (NoSuchMethodException ignored) {
				try {
					constructor = taskClass.getConstructor(MCP.class);
					try {
						tasks.add(constructor.newInstance(mcp));
					} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
						e.printStackTrace();
					}
				} catch (NoSuchMethodException ignored2) {}
			}
		}
		return tasks;
	}
}
