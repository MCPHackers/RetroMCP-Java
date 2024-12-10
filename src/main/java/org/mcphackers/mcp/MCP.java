package org.mcphackers.mcp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.mcphackers.mcp.plugin.MCPPlugin;
import org.mcphackers.mcp.plugin.MCPPlugin.MCPEvent;
import org.mcphackers.mcp.plugin.MCPPlugin.TaskEvent;
import org.mcphackers.mcp.plugin.PluginManager;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskStaged;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.source.Source;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.mcp.tools.versions.json.Version;

public abstract class MCP {
	public static final String VERSION = "v1.0.1";
	public static final String GITHUB_URL = "https://github.com/MCPHackers/RetroMCP-Java";
	public static final TranslatorUtil TRANSLATOR = new TranslatorUtil();
	private static final PluginManager pluginManager = new PluginManager();

	public Options options = new Options(this, Paths.get("options.cfg"));
	protected boolean isGUI = false;

	public static final List<? extends Source> SOURCE_ADAPTERS = new ArrayList<>();

	protected MCP() {
		Update.attemptToDeleteUpdateJar();
		changeLanguage(Language.get(Locale.getDefault()));
		pluginManager.discoverPlugins(this);
		triggerEvent(MCPEvent.ENV_STARTUP);
		System.gc();
	}

	public boolean isGUI() {
		return isGUI;
	}

	/**
	 * @return The working directory
	 */
	public abstract Path getWorkingDir();

	/**
	 * Creates instances of TaskMode and executes them
	 *
	 * @param mode task to execute
	 * @param side side to execute
	 * @return <code>true</code> if task was successfully executed
	 */
	public final boolean performTask(TaskMode mode, Side side) {
		return performTask(mode, side, true);
	}

	/**
	 * Creates instances of TaskMode and executes them
	 *
	 * @param mode          task to execute
	 * @param side          side to execute
	 * @param completionMsg display completion message when finished
	 * @return <code>true</code> if task was successfully executed
	 */
	public final boolean performTask(TaskMode mode, Side side, boolean completionMsg) {
		List<Task> tasks = mode.getTasks(this);
		if (tasks.isEmpty()) {
			System.err.println("Performing 0 tasks");
			return false;
		}

		boolean enableProgressBars = mode.usesProgressBars;

		List<Task> performedTasks = new ArrayList<>();
		for (Task task : tasks) {
			if (task.side == side || task.side == Side.ANY) {
				performedTasks.add(task);
			} else if (side == Side.ANY) {
				if (task.side == Side.SERVER || task.side == Side.CLIENT) {
					if (mode.requirement == null || mode.requirement.get(this, task.side)) {
						performedTasks.add(task);
					}
				}
			}
		}
		if (enableProgressBars) setProgressBars(performedTasks, mode);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		setActive(false);
		triggerEvent(MCPEvent.STARTED_TASKS);

		AtomicInteger result1 = new AtomicInteger(Task.INFO);
		AtomicReference<Throwable> e = new AtomicReference<>();

		for (int i = 0; i < performedTasks.size(); i++) {
			Task task = performedTasks.get(i);
			final int barIndex = i;
			if (enableProgressBars) {
				task.setProgressBarIndex(barIndex);
			}
			pool.execute(() -> {
				try {
					task.performTask();
				} catch (Throwable e1) {
					result1.set(Task.ERROR);
					e.set(e1);
				}
				if (enableProgressBars) {
					setProgress(barIndex, TRANSLATOR.translateKey("task.stage.finished"), 100);
				}
			});
		}

		pool.shutdown();
		while (true) {
			try {
				if (pool.awaitTermination(500, TimeUnit.MILLISECONDS)) break;
			} catch (InterruptedException err) {
				throw new RuntimeException("Main thread was interrupted while performTask was waiting for tasks to finish", err );
			}
		};
		triggerEvent(MCPEvent.FINISHED_TASKS);

		byte result = result1.byteValue();

		List<String> msgs = new ArrayList<>();
		for (Task task : performedTasks) {
			msgs.addAll(task.getMessageList());
			byte retresult = task.getResult();
			if (retresult > result) {
				result = retresult;
			}
		}
		if (!msgs.isEmpty()) log("");
		for (String msg : msgs) {
			log(msg);
		}
		if (completionMsg) {
			String[] msgs2 = {
					TRANSLATOR.translateKey("tasks.success"),
					TRANSLATOR.translateKey("tasks.warning"),
					TRANSLATOR.translateKey("tasks.error")};
			if (e.get() != null) {
				showMessage(mode.getFullName(), msgs2[result], e.get());
			} else {
				showMessage(mode.getFullName(), msgs2[result], result);
			}
		}
		setActive(true);
		if (enableProgressBars) clearProgressBars();
		System.gc();
		return result != Task.ERROR;
	}

	public Side getSide() {
		return getOptions().side;
	}

	/**
	 * Sets progress bars based on list of running tasks and task mode
	 *
	 * @param tasks
	 * @param mode
	 */
	public abstract void setProgressBars(List<Task> tasks, TaskMode mode);

	/**
	 * Resets progress bars to inactive state
	 */
	public abstract void clearProgressBars();

	/**
	 * Logs a message to console
	 *
	 * @param msg
	 */
	public abstract void log(String msg);

	/**
	 * @return Instance of options
	 */
	public Options getOptions() {
		return this.options;
	}

	/**
	 * @return Current version
	 */
	public abstract Version getCurrentVersion();

	/**
	 * Sets current version from parsed JSON data
	 */
	public abstract void setCurrentVersion(Version version);

	/**
	 * Sets display string for progress bar at specified barIndex
	 *
	 * @param barIndex
	 * @param progressMessage
	 */
	public abstract void setProgress(int barIndex, String progressMessage);

	/**
	 * Sets progress value for progress bar at specified barIndex (Must be in range from 0-100)
	 *
	 * @param barIndex
	 * @param progress
	 */
	public abstract void setProgress(int barIndex, int progress);

	/**
	 * Marks MCP instance as busy on a task
	 *
	 * @param active
	 */
	public abstract void setActive(boolean active);

	public abstract boolean yesNoInput(String title, String msg);

	/**
	 * Implementation of string input
	 */
	public abstract String inputString(String title, String msg);

	/**
	 * Implementation of any important messages
	 */
	public abstract void showMessage(String title, String msg, int type);

	public abstract void showMessage(String title, String msg, Throwable e);

	/**
	 * Displayed by TaskUpdateMCP
	 *
	 * @param changelog
	 * @param version
	 * @return <code>true</code> if the user chose to install update
	 */
	public abstract boolean updateDialogue(String changelog, String version);

	/**
	 * Sets display string and progress value for progress bar at specified barIndex (Must be in range from 0-100)
	 *
	 * @param barIndex
	 * @param progressMessage
	 * @param progress
	 */
	public void setProgress(int barIndex, String progressMessage, int progress) {
		setProgress(barIndex, progress);
		setProgress(barIndex, progressMessage);
	}

	/**
	 * Changes a parameter in current options and saves changes to disk
	 *
	 * @param param
	 * @param value
	 * @throws IllegalArgumentException
	 */
	public void setParameter(TaskParameter param, Object value) throws IllegalArgumentException {
		getOptions().setParameter(param, value);
		getOptions().save();
	}

	/**
	 * Changes a parameter in current options and saves changes to disk, shows an error message if fails
	 *
	 * @param param
	 * @param value
	 * @throws IllegalArgumentException
	 */
	public void safeSetParameter(TaskParameter param, String value) {
		if (param != null && value != null) {
			if (getOptions().safeSetParameter(param, value)) return;
			showMessage(param.getDesc(), TRANSLATOR.translateKey("options.invalidValue"), Task.ERROR);
		}
	}

	/**
	 * @param task
	 * @see MCPPlugin#setTaskOverrides(TaskStaged)
	 */
	public final void setPluginOverrides(TaskStaged task) {
		for (Map.Entry<String, MCPPlugin> entry : pluginManager.getLoadedPlugins().entrySet()) {
			entry.getValue().setTaskOverrides(task);
		}
	}

	/**
	 * Triggers an MCPEvent for every plugin
	 *
	 * @param event
	 */
	public final void triggerEvent(MCPEvent event) {
		for (Map.Entry<String, MCPPlugin> entry : pluginManager.getLoadedPlugins().entrySet()) {
			entry.getValue().onMCPEvent(event, this);
		}
	}

	/**
	 * Triggers a TaskEvent for every plugin
	 *
	 * @param event
	 */
	public final void triggerTaskEvent(TaskEvent event, Task task) {
		for (Map.Entry<String, MCPPlugin> entry : pluginManager.getLoadedPlugins().entrySet()) {
			entry.getValue().onTaskEvent(event, task);
		}
	}

	/**
	 * Notifies language change
	 *
	 * @param lang
	 */
	public final void changeLanguage(Language lang) {
		System.out.println("Changing language to " + lang.locale.toString());
		TRANSLATOR.changeLang(lang);
		for (Map.Entry<String, MCPPlugin> entry : pluginManager.getLoadedPlugins().entrySet()) {
			TRANSLATOR.readTranslation(entry.getValue().getClass());
		}
	}

	public List<Path> getLibraries() {
		try {
			// Omit source JARs and only add JARs with classes
			return FileUtil.walkDirectory(MCPPaths.get(this, MCPPaths.LIB), (path) -> !path.getFileName().toString().endsWith("-sources.jar") && path.getFileName().toString().endsWith(".jar"));
		} catch (IOException e) {
			// Default to version libraries
			return DownloadData.getLibraries(MCPPaths.get(this, MCPPaths.LIB), getCurrentVersion());
		}
	}
}
