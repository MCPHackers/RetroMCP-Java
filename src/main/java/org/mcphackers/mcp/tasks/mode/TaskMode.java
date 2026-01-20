package org.mcphackers.mcp.tasks.mode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskApplyPatch;
import org.mcphackers.mcp.tasks.TaskBuild;
import org.mcphackers.mcp.tasks.TaskCleanup;
import org.mcphackers.mcp.tasks.TaskCreatePatch;
import org.mcphackers.mcp.tasks.TaskDecompile;
import org.mcphackers.mcp.tasks.TaskDownloadUpdate;
import org.mcphackers.mcp.tasks.TaskMergeMappings;
import org.mcphackers.mcp.tasks.TaskRecompile;
import org.mcphackers.mcp.tasks.TaskReobfuscate;
import org.mcphackers.mcp.tasks.TaskRun;
import org.mcphackers.mcp.tasks.TaskSetup;
import org.mcphackers.mcp.tasks.TaskSourceBackup;
import org.mcphackers.mcp.tasks.TaskUpdateMD5;

/**
 * General info about task
 */
public class TaskMode {
	/**
	 * All registered tasks
	 */
	public static final List<TaskMode> registeredTasks = new ArrayList<>();

	public static TaskMode HELP = new TaskModeBuilder()
			.setName("help")
			.setProgressBars(false)
			.build();
	public static TaskMode DECOMPILE = new TaskModeBuilder()
			.setName("decompile")
			.setTaskClass(TaskDecompile.class)
			.addRequirement((mcp, side) -> {
				if (side == Side.MERGED) {
					return Files.isReadable(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT))
							&& Files.isReadable(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER));
				}
				return Files.isReadable(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
			})
			.setParameters(new TaskParameter[]{
					TaskParameter.SOURCE_VERSION,
					TaskParameter.TARGET_VERSION,
					TaskParameter.JAVA_HOME,
					TaskParameter.JAVAC_ARGS,
					TaskParameter.IGNORED_PACKAGES,
					TaskParameter.FERNFLOWER_OPTIONS,
					TaskParameter.PATCHES,
					TaskParameter.SIDE,
					TaskParameter.STRIP_GENERICS
			})
			.build();
	public static TaskMode RECOMPILE = new TaskModeBuilder()
			.setName("recompile")
			.setTaskClass(TaskRecompile.class)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side)))
			.setParameters(new TaskParameter[]{
					TaskParameter.SOURCE_VERSION,
					TaskParameter.TARGET_VERSION,
					TaskParameter.JAVA_HOME,
					TaskParameter.JAVAC_ARGS,
					TaskParameter.SIDE
			})
			.build();
	public static TaskMode REOBFUSCATE = new TaskModeBuilder()
			.setName("reobfuscate")
			.setTaskClass(TaskReobfuscate.class)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side)))
			.setParameters(new TaskParameter[]{
					TaskParameter.SOURCE_VERSION,
					TaskParameter.TARGET_VERSION,
					TaskParameter.JAVA_HOME,
					TaskParameter.JAVAC_ARGS,
					TaskParameter.SIDE,
					TaskParameter.EXCLUDED_CLASSES
			})
			.build();
	public static TaskMode UPDATE_MD5 = new TaskModeBuilder()
			.setName("updatemd5")
			.setTaskClass(TaskUpdateMD5.class)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.BIN, side)))
			.setParameters(new TaskParameter[]{
					TaskParameter.SOURCE_VERSION,
					TaskParameter.TARGET_VERSION,
					TaskParameter.JAVA_HOME,
					TaskParameter.JAVAC_ARGS,
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
			.build();
	public static TaskMode CLEANUP = new TaskModeBuilder()
			.setName("cleanup")
			.setTaskClass(TaskCleanup.class)
			.setProgressBars(false)
			.build();
	public static TaskMode START = new TaskModeBuilder()
			.setName("start")
			.setTaskClass(TaskRun.class)
			.setProgressBars(false)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.BIN, side)) || Files.isReadable(MCPPaths.get(mcp, MCPPaths.BIN, Side.MERGED)))
			.setParameters(new TaskParameter[]{
					TaskParameter.RUN_BUILD
			})
			.build();
	public static TaskMode BUILD = new TaskModeBuilder()
			.setName("build")
			.setTaskClass(TaskBuild.class)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side)))
			.setParameters(new TaskParameter[]{
					TaskParameter.SOURCE_VERSION,
					TaskParameter.TARGET_VERSION,
					TaskParameter.JAVA_HOME,
					TaskParameter.JAVAC_ARGS,
					TaskParameter.FULL_BUILD,
					TaskParameter.SIDE
			})
			.build();
	public static TaskMode CREATE_PATCH = new TaskModeBuilder()
			.setName("createpatch")
			.setTaskClass(TaskCreatePatch.class)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side))
					&& Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE_UNPATCHED, side)))
			.setParameters(new TaskParameter[]{
					TaskParameter.SIDE
			})
			.build();
	public static TaskMode APPLY_PATCH = new TaskModeBuilder()
			.setName("applypatch")
			.setTaskClass(TaskApplyPatch.class)
			.setProgressBars(false)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.PATCHES, side))
					&& Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side)))
			.setParameters(new TaskParameter[]{
					TaskParameter.SIDE
			})
			.build();
	public static TaskMode BACKUP_SRC = new TaskModeBuilder()
			.setName("backupsrc")
			.setTaskClass(TaskSourceBackup.class)
			.setProgressBars(true)
			.addRequirement((mcp, side) -> Files.isReadable(MCPPaths.get(mcp, MCPPaths.SOURCE, side)))
			.build();
	public static TaskMode MERGE_MAPPINGS = new TaskModeBuilder()
			.setName("mergemappings")
			.setTaskClass(TaskMergeMappings.class)
			.setProgressBars(false)
			.build();
	public static TaskMode EXIT = new TaskModeBuilder()
			.setName("exit")
			.setProgressBars(false)
			.build();

	public final String name;
	public final boolean usesProgressBars;
	public final Class<? extends Task> taskClass;
	public final TaskParameter[] params;
	public final Requirement requirement;

	/**
	 * Create a new TaskMode instance
	 *
	 * @param name         internal name
	 * @param taskClass    class of Task
	 * @param params       used parameters
	 * @param useBars      if task should display progress bars
	 * @param requirements
	 */
	public TaskMode(String name, Class<? extends Task> taskClass, TaskParameter[] params, boolean useBars, Requirement requirements) {
		this.name = name;
		this.taskClass = taskClass;
		this.params = params;
		this.usesProgressBars = useBars;
		this.requirement = requirements;
		registeredTasks.add(this);
	}

	/**
	 * @return Internal name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Translated name
	 */
	public String getFullName() {
		String s = "task." + name;
		if (MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return name;
	}

	/**
	 * @return Translated description
	 */
	public String getDesc() {
		String s = "task." + name + ".desc";
		if (MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return MCP.TRANSLATOR.translateKey("task.noDesc");
	}


	private List<Side> allowedSides() {
		List<Side> sides = new ArrayList<>();
		for (Side side : Side.VALUES) {
			if (side != Side.ANY) {
				sides.add(side);
			}
		}
		return sides;
	}

	/**
	 * Create new instances of executable Tasks based on current TaskMode
	 *
	 * @param mcp
	 * @return List of Tasks
	 */
	public List<Task> getTasks(MCP mcp) {
		List<Task> tasks = new ArrayList<>();
		if (taskClass != null) {
			Constructor<? extends Task> constructor;
			try {
				constructor = taskClass.getConstructor(Side.class, MCP.class);
				try {
					for (Side side : allowedSides()) {
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
				} catch (NoSuchMethodException ignored2) {
				}
			}
		}
		return tasks;
	}

	/**
	 * Checks if all requirements are met for specified side and the task can be executed
	 *
	 * @param mcp
	 * @param side
	 * @return availability
	 */
	public boolean isAvailable(MCP mcp, Side side) {
		if (requirement == null) return true;
		if (side == Side.ANY) {
			return requirement.get(mcp, Side.CLIENT) || requirement.get(mcp, Side.SERVER);
		} else {
			return requirement.get(mcp, side);
		}
	}

	@FunctionalInterface
	public interface Requirement {
		/**
		 * Determines if the specified side can be executed in specified mcp instance
		 *
		 * @param mcp
		 * @param side
		 */
		boolean get(MCP mcp, Side side);
	}
}
