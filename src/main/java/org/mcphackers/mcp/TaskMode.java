package org.mcphackers.mcp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.tasks.*;
import org.mcphackers.mcp.tasks.Task.Side;

public enum TaskMode {
	help("Help", "Displays command usage", null),
	decompile("Decompile", "Start decompiling Minecraft", TaskDecompile.class, new TaskParameter[]{
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.IGNORED_PACKAGES,
			TaskParameter.INDENTION_STRING,
			TaskParameter.PATCHES,
			TaskParameter.SIDE
			}),
	recompile("Recompile", "Recompile Minecraft sources", TaskRecompile.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			}),
	reobfuscate("Reobfuscate", "Reobfuscate Minecraft classes", TaskReobfuscate.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			}),
	updatemd5("Update MD5 Hashes", "Update md5 hash tables used for reobfuscation", TaskUpdateMD5.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			}),
	updatemcp("Update", "Download an update if available", TaskDownloadUpdate.class),

	setup("Setup", "Choose a version to setup", TaskSetup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			}),
	cleanup("Cleanup", "Delete all source and class folders", TaskCleanup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SRC_CLEANUP
			}),
	start("Start", "Runs the client or the server from compiled classes", TaskRun.class, new TaskParameter[] {
			TaskParameter.RUN_BUILD
			}),
	build("Build", "Builds the final jar or zip", TaskBuild.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.FULL_BUILD,
			TaskParameter.SIDE
			}),
	createpatch("Create patch", "Creates patch", TaskTest.class),
	exit("Exit", "Exit the program", null);
	
	public final String name;
	public final String desc;
	public final Class<? extends Task> taskClass;
	public TaskParameter[] params = new TaskParameter[] {};
	
	TaskMode(String name, String desc, Class<? extends Task> taskClass) {
		this.name = name;
		this.desc = desc;
		this.taskClass = taskClass;
	}
	
	TaskMode(String name, String desc, Class<? extends Task> taskClass, TaskParameter[] params) {
		this(name, desc, taskClass);
		this.params = params;
	}
	
	public List<Task> getTasks(MCP mcp) {
		List<Task> tasks = new ArrayList<>();
		if(taskClass != null) {
			Constructor<? extends Task> constructor;
			try {
				constructor = taskClass.getConstructor(Side.class, MCP.class);
				try {
					tasks.add(constructor.newInstance(Side.CLIENT, mcp));
					tasks.add(constructor.newInstance(Side.SERVER, mcp));
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
