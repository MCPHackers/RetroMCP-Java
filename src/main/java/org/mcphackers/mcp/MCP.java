package org.mcphackers.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.mcphackers.mcp.plugin.MCPPlugin;
import org.mcphackers.mcp.plugin.MCPPlugin.MCPEvent;
import org.mcphackers.mcp.plugin.MCPPlugin.TaskEvent;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskStaged;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.FileUtil;

public abstract class MCP {

	public static final String VERSION = "v1.0";
	public static final String githubURL = "https://github.com/MCPHackers/RetroMCP-Java";

	private static final Map<String, MCPPlugin> plugins = new HashMap<>();

	public static final TranslatorUtil TRANSLATOR = new TranslatorUtil();

	static {
		loadPlugins();
	}

	protected MCP() {
		triggerEvent(MCPEvent.ENV_STARTUP);
		Update.attemptToDeleteUpdateJar();
		changeLanguage(Language.get(Locale.getDefault()));
		System.gc();
	}

	public abstract Path getWorkingDir();


	/**
	 * Creates instances of TaskMode and executes them
	 * @param mode task to execute
	 * @param side side to execute
	 * @return <tt>true</tt> if task was successfully executed
	 */
	public final boolean performTask(TaskMode mode, Side side) {
		return performTask(mode, side, true);
	}

	/**
	 * Creates instances of TaskMode and executes them
	 * @param mode task to execute
	 * @param side side to execute
	 * @param completionMsg display completion message when finished
	 * @return <tt>true</tt> if task was successfully executed
	 */
	public final boolean performTask(TaskMode mode, Side side, boolean completionMsg) {
		List<Task> tasks = mode.getTasks(this);
		if(tasks.size() == 0) {
			System.err.println("Performing 0 tasks");
			return false;
		}
		
		boolean enableProgressBars = mode.usesProgressBars;
		
		List<Task> performedTasks = new ArrayList<>();
		for (Task task : tasks) {
			if (task.side == side || task.side == Side.ANY) {
				performedTasks.add(task);
			}
			else if (side == Side.ANY) {
				if (task.side == Side.SERVER || task.side == Side.CLIENT) {
					if(mode.requirement.get(this, task.side)) {
						performedTasks.add(task);
					}
				}
			}
		}
		if(enableProgressBars) setProgressBars(performedTasks, mode);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		setActive(false);
		triggerEvent(MCPEvent.STARTED_TASKS);

		AtomicInteger result1 = new AtomicInteger(Task.INFO);

		for(int i = 0; i < performedTasks.size(); i++) {
			Task task = performedTasks.get(i);
			final int barIndex = i;
			if(enableProgressBars) {
				task.setProgressBarIndex(barIndex);
			}
			pool.execute(() -> {
				try {
					task.performTask();
				} catch (Throwable e) {
					result1.set(Task.ERROR);
					e.printStackTrace();
				}
				if(enableProgressBars) {
					setProgress(barIndex, TRANSLATOR.translateKey("task.stage.finished"), 100);
				}
			});
		}
		
		pool.shutdown();
		while (!pool.isTerminated()) {}

		byte result = result1.byteValue();
		
		List<String> msgs = new ArrayList<>();
		for(Task task : performedTasks) {
			msgs.addAll(task.getMessageList());
			byte retresult = task.getResult();
			if(retresult > result) {
				result = retresult;
			}
		}
		//TODO display this info in the pop up message (Maybe)
		if(msgs.size() > 0) log("");
		for(String msg : msgs) {
			log(msg);
		}
		triggerEvent(MCPEvent.FINISHED_TASKS);
		if(completionMsg) {
			String[] msgs2 = {
					TRANSLATOR.translateKey("tasks.success"),
					TRANSLATOR.translateKey("tasks.warning"),
					TRANSLATOR.translateKey("tasks.error")};
			showMessage(mode.getFullName(), msgs2[result], result);
		}
		setActive(true);
		if(enableProgressBars) clearProgressBars();
		System.gc();
		return result != Task.ERROR;
	}

	public abstract void setProgressBars(List<Task> tasks, TaskMode mode);

	public abstract void clearProgressBars();

	public abstract void log(String msg);

	public abstract Options getOptions();

	public abstract String getCurrentVersion();

	public abstract void setCurrentVersion(String version);

	public abstract void setProgress(int barIndex, String progressMessage);

	public abstract void setProgress(int barIndex, int progress);

	public abstract void setActive(boolean active);

	public abstract boolean yesNoInput(String title, String msg);

	public abstract String inputString(String title, String msg);

	public abstract void showMessage(String title, String msg, int type);

	public abstract boolean updateDialogue(String changelog, String version);

	public void setProgress(int barIndex, String progressMessage, int progress) {
		setProgress(barIndex, progress);
		setProgress(barIndex, progressMessage);
	}

	public void setParameter(TaskParameter param, Object value) throws IllegalArgumentException {
		getOptions().setParameter(param, value);
		getOptions().save();
	}

	public void safeSetParameter(TaskParameter param, String value) {
		if(value != null) {
			if(getOptions().safeSetParameter(param, value)) return;
			showMessage(param.getDesc(), TRANSLATOR.translateKey("options.invalidValue"), Task.ERROR);
		}
	}

	private final static void loadPlugins() {
		Path pluginsDir = Paths.get("plugins");
		if(Files.exists(pluginsDir)) {
			List<Path> jars = new ArrayList<>();
			try {
				FileUtil.collectJars(pluginsDir, jars);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				for(Path p : jars) {
					List<Class<MCPPlugin>> classes = ClassUtils.getClasses(p, MCPPlugin.class);
					for(Class<MCPPlugin> cls : classes) {
						if(!ClassUtils.isClassAbstract(cls)) {
							MCPPlugin plugin = cls.newInstance();
							plugin.init();
							plugins.put(plugin.pluginId() + plugin.hashCode(), plugin);
						}
						else {
							System.err.println(TRANSLATOR.translateKey("mcp.incompatiblePlugin") + cls.getName());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public final void setPluginOverrides(TaskStaged task) {
		for(Map.Entry<String, MCPPlugin> entry : plugins.entrySet()) {
			entry.getValue().setTaskOverrides(task);
		}
	}

	public final void triggerEvent(MCPEvent event) {
		for(Map.Entry<String, MCPPlugin> entry : plugins.entrySet()) {
			entry.getValue().onMCPEvent(event, this);
		}
	}

	public final void triggerTaskEvent(TaskEvent event, Task task) {
		for(Map.Entry<String, MCPPlugin> entry : plugins.entrySet()) {
			entry.getValue().onTaskEvent(event, task);
		}
	}

	public final void changeLanguage(Language lang) {
		TRANSLATOR.changeLang(lang);
		for(Map.Entry<String, MCPPlugin> entry : plugins.entrySet()) {
			TRANSLATOR.readTranslation(entry.getValue().getClass());
		}
	}
}
