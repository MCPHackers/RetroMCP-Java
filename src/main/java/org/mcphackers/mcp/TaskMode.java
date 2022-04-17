package org.mcphackers.mcp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.tasks.*;
import org.mcphackers.mcp.tasks.Task.Side;

public class TaskMode {
	public static final List<TaskMode> registeredTasks = new ArrayList<>();
	
	public static final TaskMode HELP = new TaskMode("help", "Help", "Displays command usage", null);
	public static final TaskMode DECOMPILE = new TaskMode("decompile", "Decompile", "Start decompiling Minecraft", TaskDecompile.class, new TaskParameter[]{
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.IGNORED_PACKAGES,
			TaskParameter.INDENTION_STRING,
			TaskParameter.PATCHES,
			TaskParameter.SIDE
			});
	public static final TaskMode RECOMPILE = new TaskMode("recompile", "Recompile", "Recompile Minecraft sources", TaskRecompile.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			});
	public static final TaskMode REOBFUSCATE = new TaskMode("reobfuscate", "Reobfuscate", "Reobfuscate Minecraft classes", TaskReobfuscate.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			});
	public static final TaskMode UPDATE_MD5 = new TaskMode("updatemd5", "Update MD5 Hashes", "Update md5 hash tables used for reobfuscation", TaskUpdateMD5.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			});
	public static final TaskMode UPDATE_MCP = new TaskMode("updatemcp", "Update", "Download an update if available", TaskDownloadUpdate.class);

	public static final TaskMode SETUP = new TaskMode("setup", "Setup", "Choose a version to setup", TaskSetup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			});
	public static final TaskMode CLEANUP = new TaskMode("cleanup", "Cleanup", "Delete all source and class folders", TaskCleanup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SRC_CLEANUP
			});
	public static final TaskMode START = new TaskMode("start", "Start", "Runs the client or the server from compiled classes", TaskRun.class, new TaskParameter[] {
			TaskParameter.RUN_BUILD
			});
	public static final TaskMode BUILD = new TaskMode("build", "Build", "Builds the final jar or zip", TaskBuild.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.FULL_BUILD,
			TaskParameter.SIDE
			});
	public static final TaskMode CREATE_PATCH = new TaskMode("createpatch", "Create patch", "Creates patch", TaskCreatePatch.class);
	public static final TaskMode EXIT = new TaskMode("exit", "Exit", "Exit the program", null);
	
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
	
	public TaskMode(String name, String fullName, Class<? extends Task> taskClass, TaskParameter[] params) {
		this(name, fullName, "???", taskClass, params);
	}
	
	public TaskMode(String name, String fullName, String desc, Class<? extends Task> taskClass, TaskParameter[] params) {
		this(name, fullName, desc, taskClass);
		this.params = params;
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
