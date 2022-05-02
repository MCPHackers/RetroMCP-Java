package org.mcphackers.mcp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskBuild;
import org.mcphackers.mcp.tasks.TaskCleanup;
import org.mcphackers.mcp.tasks.TaskCreatePatch;
import org.mcphackers.mcp.tasks.TaskDecompile;
import org.mcphackers.mcp.tasks.TaskDownloadUpdate;
import org.mcphackers.mcp.tasks.TaskRecompile;
import org.mcphackers.mcp.tasks.TaskReobfuscate;
import org.mcphackers.mcp.tasks.TaskRun;
import org.mcphackers.mcp.tasks.TaskSetup;
import org.mcphackers.mcp.tasks.TaskUpdateMD5;
import org.mcphackers.mcp.tools.MCPPaths;

public class TaskMode {
	public static final List<TaskMode> registeredTasks = new ArrayList<>();
	
	public static final Map<String, TaskParameter> nameToParamMap = new HashMap<>();
	
	public static TaskMode HELP = new TaskModeBuilder()
			.setCmdName("help")
			.setFullName("Help")
			.setDescription("Displays command usage")
			.build();
	public static TaskMode DECOMPILE = new TaskModeBuilder()
			.setCmdName("decompile")
			.setFullName("Decompile")
			.setDescription("Start decompiling Minecraft")
			.setTaskClass(TaskDecompile.class)
			.addRequirement((mcp, side) -> {
				if(side == Side.MERGED) {
					return Files.isReadable(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT))
						&& Files.isReadable(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER));
				}
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
			})
			.setParameters(new TaskParameter[]{
				TaskParameter.DEBUG,
				TaskParameter.SOURCE_VERSION,
				TaskParameter.TARGET_VERSION,
				TaskParameter.JAVA_HOME,
				TaskParameter.IGNORED_PACKAGES,
				TaskParameter.INDENTION_STRING,
				TaskParameter.PATCHES,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode RECOMPILE = new TaskModeBuilder()
			.setCmdName("recompile")
			.setFullName("Recompile")
			.setDescription("Recompile Minecraft sources")
			.setTaskClass(TaskRecompile.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCES, side));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SOURCE_VERSION,
				TaskParameter.TARGET_VERSION,
				TaskParameter.JAVA_HOME,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode REOBFUSCATE = new TaskModeBuilder()
			.setCmdName("reobfuscate")
			.setFullName("Reobfuscate")
			.setDescription("Reobfuscate Minecraft classes")
			.setTaskClass(TaskReobfuscate.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCES, side));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SOURCE_VERSION,
				TaskParameter.TARGET_VERSION,
				TaskParameter.JAVA_HOME,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode UPDATE_MD5 = new TaskModeBuilder()
			.setCmdName("updatemd5")
			.setFullName("Update MD5")
			.setDescription("Update MD5 hash tables used for reobfuscation")
			.setTaskClass(TaskUpdateMD5.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.BIN_SIDE, side));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SOURCE_VERSION,
				TaskParameter.TARGET_VERSION,
				TaskParameter.JAVA_HOME,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode UPDATE_MCP = new TaskModeBuilder()
			.setCmdName("updatemcp")
			.setFullName("Update")
			.setDescription("Download an update if available")
			.setTaskClass(TaskDownloadUpdate.class)
			.build();
	public static TaskMode SETUP = new TaskModeBuilder()
			.setCmdName("setup")
			.setFullName("Setup")
			.setDescription("Set initial workspace for a version")
			.setTaskClass(TaskSetup.class)
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG
				})
			.build();
	public static TaskMode CLEANUP = new TaskModeBuilder()
			.setCmdName("cleanup")
			.setFullName("Cleanup")
			.setDescription("Delete all source and class folders")
			.setTaskClass(TaskCleanup.class)
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SRC_CLEANUP
				})
			.build();
	public static TaskMode START = new TaskModeBuilder()
			.setCmdName("start")
			.setFullName("Start")
			.setDescription("Runs the client or the server from compiled classes")
			.setTaskClass(TaskRun.class)
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.RUN_BUILD
				})
			.build();
	public static TaskMode BUILD = new TaskModeBuilder()
			.setCmdName("build")
			.setFullName("Build")
			.setDescription("Builds the final jar or zip")
			.setTaskClass(TaskBuild.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCES, side));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SOURCE_VERSION,
				TaskParameter.TARGET_VERSION,
				TaskParameter.JAVA_HOME,
				TaskParameter.FULL_BUILD,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode CREATE_PATCH = new TaskModeBuilder()
			.setCmdName("createpatch")
			.setFullName("Create patch")
			.setDescription("Creates a patch based off your changes to source")
			.setTaskClass(TaskCreatePatch.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCES, side))
					&& Files.isReadable(MCPPaths.get(mcp, MCPPaths.TEMP_SOURCES, side));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode EXIT = new TaskModeBuilder()
			.setCmdName("exit")
			.setFullName("Exit")
			.setDescription("Exit the program")
			.build();
	
	private final String name;
	private final String fullName;
	private final String desc;
	public final Class<? extends Task> taskClass;
	public final TaskParameter[] params;
	public final Requirement requirement;
	
	public TaskMode(String name, String fullName, String desc, Class<? extends Task> taskClass, TaskParameter[] params, Requirement requirements) {
		this.name = name;
		this.fullName = fullName;
		this.desc = desc;
		this.taskClass = taskClass;
		this.params = params;
		this.requirement = requirements;
		registeredTasks.add(this); //TODO overriding any default tasks will register a duplicate
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
		for(Side side : Side.values()) {
			if(side != Side.ANY) {
				sides.add(side);
			}
		}
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

	public boolean isAvailable(MCP mcp, Side side) {
		if(side == Side.ANY) {
			return requirement.get(mcp, Side.CLIENT) || requirement.get(mcp, Side.SERVER);
		}
		else {
			if(!requirement.get(mcp, side)) {
				return false;
			}
		}
		return true;
	}
}
