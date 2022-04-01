package org.mcphackers.mcp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.tasks.*;
import org.mcphackers.mcp.tasks.Task.Side;

public enum TaskMode {
	help("Displays command usage", null),
	decompile("Start decompiling Minecraft", TaskDecompile.class, new TaskParameter[]{
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.IGNORED_PACKAGES,
			TaskParameter.INDENTION_STRING,
			TaskParameter.PATCHES,
			TaskParameter.SIDE
			}),
	recompile("Recompile Minecraft sources", TaskRecompile.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			}),
	reobfuscate("Reobfuscate Minecraft classes", TaskReobfuscate.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			}),
	updatemd5("Update md5 hash tables used for reobfuscation", TaskUpdateMD5.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.SIDE
			}),
	updatemcp("Download an update if available", TaskDownloadUpdate.class),

	setup("Choose a version to setup", TaskSetup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			}),
	cleanup("Delete all source and class folders", TaskCleanup.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SRC
			}),
	startclient("Runs the client from compiled classes", TaskRun.class, new TaskParameter[] {
			TaskParameter.RUN_BUILD
			}),
	startserver("Runs the server from compiled classes", TaskRun.class, new TaskParameter[] {
			TaskParameter.RUN_BUILD
			}),
	build("Builds the final jar or zip", TaskBuild.class, new TaskParameter[] {
			TaskParameter.DEBUG,
			TaskParameter.SOURCE_VERSION,
			TaskParameter.TARGET_VERSION,
			TaskParameter.BOOT_CLASS_PATH,
			TaskParameter.FULL_BUILD,
			TaskParameter.SIDE
			}),
	createpatch("Creates patch", TaskCreatePatch.class),
	exit("Exit the program", null);
	
	public final String desc;
	public final Class<? extends Task> taskClass;
	public TaskParameter[] params = new TaskParameter[] {};
	
	TaskMode(String desc, Class<? extends Task> taskClass) {
		this.desc = desc;
		this.taskClass = taskClass;
	}
	
	TaskMode(String desc, Class<? extends Task> taskClass, TaskParameter[] params) {
		this(desc, taskClass);
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
