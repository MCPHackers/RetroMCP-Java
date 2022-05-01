package org.mcphackers.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.mcphackers.mcp.plugin.MCPPlugin;
import org.mcphackers.mcp.plugin.MCPPlugin.MCPEvent;
import org.mcphackers.mcp.plugin.MCPPlugin.TaskEvent;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public abstract class MCP {

	public static final String VERSION = "v1.0-pre2";
	private static final Map<String, MCPPlugin> plugins = new HashMap<>();

	static {
		Update.attemptToDeleteUpdateJar();
		loadPlugins();
	}

	protected MCP() {
		triggerEvent(MCPEvent.ENV_STARTUP);
	}

	public void performTask(TaskMode mode, Side side) {
		performTask(mode, side, true, true);
	}

	public abstract Path getWorkingDir();

	public final void performTask(TaskMode mode, Side side, boolean enableProgressBars, boolean enableCompletionMessage) {
		List<Task> tasks = mode.getTasks(this);
		if(tasks.size() == 0) {
			System.err.println("Performing 0 tasks");
			return;
		}

		boolean hasServer = true;
		try {
			hasServer = VersionsParser.hasServer(getCurrentVersion());
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<Task> performedTasks = new ArrayList<>();
		for (Task task : tasks) {
			if (side == Side.ANY) {
				// TODO also check if client/server exists locally
				if (task.side != Side.SERVER || hasServer) {
					performedTasks.add(task);
				}
			} else if (task.side == side || task.side == Side.ANY) {
				performedTasks.add(task);
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
				} catch (Exception e) {
					result1.set(Task.ERROR);
					e.printStackTrace();
				}
				if(enableProgressBars) {
					setProgress(barIndex, "Finished!", 100);
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
		//TODO display this info in the pop up message
		if(msgs.size() > 0) log("");
		for(String msg : msgs) {
			log(msg);
		}
		triggerEvent(MCPEvent.FINISHED_TASKS);
		if(enableCompletionMessage) {
			String[] msgs2 = {"Finished successfully!", "Finished with warnings!", "Finished with errors!"};
			showMessage(mode.getFullName(), msgs2[result], result);
		}
		setActive(true);
		if(enableProgressBars) clearProgressBars();
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

	public void setProgress(int barIndex, String progressMessage, int progress) {
		setProgress(barIndex, progress);
		setProgress(barIndex, progressMessage);
	}

	public void setParameter(TaskParameter param, Object value) throws IllegalArgumentException {
		getOptions().setParameter(param, value);
	}

	public void safeSetParameter(TaskParameter param, String value) {
		if(value != null) {
			if(param.type == Integer.class) {
				try {
					int valueInt = Integer.parseInt(value);
					setParameter(param, valueInt);
					return;
				}
				catch (NumberFormatException ignored) {}
				catch (IllegalArgumentException e) {}
			}
			else if(param.type == Boolean.class) {
				if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
					try {
						boolean valueBoolean = Boolean.parseBoolean(value);
						setParameter(param, valueBoolean);
						return;
					}
					catch (IllegalArgumentException e) {}
				}
			}
			else if(param.type == String[].class) {
				try {
					String[] values = value.split(",");
					for(int i2 = 0 ; i2 < values.length; i2++) {
						values[i2] = Util.convertFromEscapedString(values[i2]).trim();
					}
					setParameter(param, values);
					return;
				}
				catch (IllegalArgumentException e) {}
			}
			else if(param.type == String.class) {
				try {
					value = Util.convertFromEscapedString(value);
					setParameter(param, value);
					return;
				}
				catch (IllegalArgumentException e) {}
			}
			showMessage(param.desc, "Invalid value!", Task.ERROR);
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
						MCPPlugin plugin = cls.newInstance();
						plugin.init();
						plugins.put(plugin.pluginId(), plugin);
					}
	    		}
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }

	public final void setPluginOverrides(Task task) {
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
}
