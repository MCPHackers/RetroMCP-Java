package org.mcphackers.mcp.tasks.mode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.*;

public class TaskMode {
	public static final List<TaskMode> registeredTasks = new ArrayList<>();
	
	public static final Map<String, TaskParameter> nameToParamMap = new HashMap<>();
	
	public static TaskMode HELP = new TaskModeBuilder()
			.setName("help")
			.setProgressBars(false)
			.build();
	public static TaskMode DECOMPILE = new TaskModeBuilder()
			.setName("decompile")
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
				TaskParameter.INDENTATION_STRING,
				TaskParameter.PATCHES,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode RECOMPILE = new TaskModeBuilder()
			.setName("recompile")
			.setTaskClass(TaskRecompile.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side));
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
			.setName("reobfuscate")
			.setTaskClass(TaskReobfuscate.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side));
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
			.setName("updatemd5")
			.setTaskClass(TaskUpdateMD5.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.COMPILED, side));
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
			.setName("updatemcp")
			.setTaskClass(TaskDownloadUpdate.class)
			.setProgressBars(false)
			.build();
	public static TaskMode SETUP = new TaskModeBuilder()
			.setName("setup")
			.setTaskClass(TaskSetup.class)
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG
				})
			.build();
	public static TaskMode CLEANUP = new TaskModeBuilder()
			.setName("cleanup")
			.setTaskClass(TaskCleanup.class)
			.setProgressBars(false)
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG
				})
			.build();
	public static TaskMode START = new TaskModeBuilder()
			.setName("start")
			.setTaskClass(TaskRun.class)
			.setProgressBars(false)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.COMPILED, side)) ||  Files.isReadable(MCPPaths.get(mcp, MCPPaths.COMPILED, Side.MERGED));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.RUN_BUILD
				})
			.build();
	public static TaskMode BUILD = new TaskModeBuilder()
			.setName("build")
			.setTaskClass(TaskBuild.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side));
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
			.setName("createpatch")
			.setTaskClass(TaskCreatePatch.class)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side))
					&& Files.isReadable(MCPPaths.get(mcp, MCPPaths.TEMP_SRC, side));
			})
			.setParameters(new TaskParameter[] {
				TaskParameter.DEBUG,
				TaskParameter.SIDE
				})
			.build();
	public static TaskMode BACKUP_SRC = new TaskModeBuilder()
			.setName("backupsrc")
			.setTaskClass(TaskSourceBackup.class)
			.setProgressBars(true)
			.addRequirement((mcp, side) -> {
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side));
			})
			.build();
	public static TaskMode EXIT = new TaskModeBuilder()
			.setName("exit")
			.setProgressBars(false)
			.build();

	static {
//		new TaskModeBuilder()
//		.setName("timestamps")
//		.setTaskClass(TaskTimestamps.class)
//		.build();
	}
	public final String name;
	public final boolean usesProgressBars;
	public final Class<? extends Task> taskClass;
	public final TaskParameter[] params;
	public final Requirement requirement;
	
	public TaskMode(String name, Class<? extends Task> taskClass, TaskParameter[] params, boolean useBars, Requirement requirements) {
		this.name = name;
		this.taskClass = taskClass;
		this.params = params;
		this.usesProgressBars = useBars;
		this.requirement = requirements;
		registeredTasks.add(this);
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		String s = "task." + name;
		if(MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return name;
	}
	
	public String getDesc() {
		String s = "task." + name + ".desc";
		if(MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return MCP.TRANSLATOR.translateKey("task.noDesc");
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
			return requirement.get(mcp, side);
		}
	}
	
	@FunctionalInterface
	public interface Requirement {
		boolean get(MCP mcp, Side side);
	}
}
