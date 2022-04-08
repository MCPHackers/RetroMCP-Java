package org.mcphackers.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.VersionsParser;

public interface MCP {
	
	String VERSION = "v0.4";

	default void attemptToDeleteUpdateJar() {
		long startTime = System.currentTimeMillis();
		boolean keepTrying = true;
		while(keepTrying) {
			try {
				Files.deleteIfExists(Paths.get(MCPPaths.UPDATE_JAR));
				keepTrying = false;
			} catch (IOException e) {
				keepTrying = System.currentTimeMillis() - startTime < 10000;
			}
		}
	}

	default void performTask(TaskMode mode, Side side) {
		performTask(mode, side, true, true);
	}
	
	default void performTask(TaskMode mode, Side side, boolean enableProgressBars, boolean enableCompletionMessage) {
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
				if (task.side == Side.SERVER && hasServer || task.side != Side.SERVER) {
					performedTasks.add(task);
				}
			} else if (task.side == side) {
				performedTasks.add(task);
			}
		}
		setProgressBars(performedTasks);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		setActive(false);

		AtomicInteger result1 = new AtomicInteger(Task.INFO);
		
		for(int i = 0; i < performedTasks.size(); i++) {
			Task task = performedTasks.get(i);
			final int barIndex = i;
			if(enableProgressBars) {
				task.setProgressBarIndex(barIndex);
			}
			pool.execute(() -> {
				String name = mode.name;
				if(task.side == Side.CLIENT || task.side == Side.SERVER) {
					name = task.side.name;
				}
				if(enableProgressBars) {
					setProgressBarName(barIndex, name);
					setProgressBarActive(barIndex, true);
				}
				try {
					task.doTask();
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
		if(msgs.size() > 0) log("");
		for(String msg : msgs) {
			log(msg);
		}
		if(enableCompletionMessage) {
			String[] msgs2 = {"Finished successfully!", "Finished with warnings!", "Finished with errors!"};
			showMessage(mode.name, msgs2[result], result);
		}

		for(int i = 0; i < performedTasks.size(); i++) {
			setProgressBarActive(i, false);
			
			setProgress(i, "Idle", 0);
		}
		setActive(true);
		clearProgressBars();
	}
	
	void setProgressBars(List<Task> tasks);
	
	void clearProgressBars();

	void log(String msg);
	
	Options getOptions();
	
	String getCurrentVersion();
	
	void setCurrentVersion(String version);
	
	void setProgressBarActive(int barIndex, boolean active);
	
	void setProgressBarName(int side, String name);
	
	void setProgress(int barIndex, String progressMessage);
	
	void setProgress(int barIndex, int progress);
	
	void setActive(boolean active);

	boolean yesNoInput(String title, String msg);
	
	String inputString(String title, String msg);
	
	void showMessage(String title, String msg, int type);
	
	default void setProgress(int barIndex, String progressMessage, int progress) {
		setProgress(barIndex, progress);
		setProgress(barIndex, progressMessage);
	}
}
