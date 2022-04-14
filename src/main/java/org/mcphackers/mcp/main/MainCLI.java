package org.mcphackers.mcp.main;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.Update;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class MainCLI implements MCP {
	private static final Ansi logo =
			new Ansi()
			.fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____  ").a('\n')
			.fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\ ").a('\n')
			.fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n')
			.fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/ ").a('\n')
			.fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |     ").a('\n')
			.fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|     ").a('\n')
			.fgDefault();
	private TaskMode mode;
	private Side side = Side.ANY;
	private TaskMode helpCommand;
	private Console console = System.console();
	private Options options = new Options();
	private String currentVersion;
	
	private int[] progresses;
	private String[] progressStrings;
	private String[] progressBarNames;

	public static void main(String[] args) throws Exception {
		if(System.console() != null) {
			AnsiConsole.systemInstall();
		}
		else {
//			System.err.println("Error: Could not find console. Launching GUI instead");
//			MainGUI.main(args);
			String filename = MCP.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
			Runtime.getRuntime().exec(new String[]{"cmd","/c","start","cmd","/k","java -cp \"" + filename + ";D:/Stuff/git/RetroMCP-Java/build/libs/RetroMCP-Java-all.jar" + "\" org.mcphackers.mcp.main.MainCLI"});
			return;
		}
		new MainCLI(args);
	}
	
	public MainCLI(String[] args) {

		options.resetDefaults();
		Update.attemptToDeleteUpdateJar();
		log("RetroMCP " + MCP.VERSION);

		boolean startedWithNoParams = false;
		boolean exit = false;
		String version = null;
		if(Files.exists(Paths.get(MCPPaths.VERSION))) {
			try {
				currentVersion = VersionsParser.setCurrentVersion(this, new String(Files.readAllBytes(Paths.get(MCPPaths.VERSION))));
				version = new Ansi().a("Current version: ").fgBrightCyan().a(currentVersion).fgDefault().toString();
			} catch (Exception e) {
				version = new Ansi().fgBrightRed().a("Unable to get current version!").fgDefault().toString();
			}
		}
		if (args.length <= 0) {
			startedWithNoParams = true;
			log(logo.toString());
			JavaCompiler c = ToolProvider.getSystemJavaCompiler();
			if (c == null) {
				// Likely a JRE
				log(new Ansi().fgBrightRed().a("Error: Java Development Kit not detected! Compilation will fail!").toString());
				log("Using Java from " + Paths.get(Util.getJava()).toAbsolutePath());
			}
			if(version != null) log(version);
			log(new Ansi().fgDefault().a("Enter a command to execute:").toString());
		}
		int executeTimes = 0;
		while (startedWithNoParams && !exit || !startedWithNoParams && executeTimes < 1) {
			while (args.length < 1) {
				System.out.print(new Ansi().fgBrightCyan().a("> ").fgRgb(255,255,255));
				String str = "";
				try {
					str = console.readLine().trim();
				} catch (NoSuchElementException ignored) {
					mode = TaskMode.exit;
				}
				System.out.print(new Ansi().fgDefault());
				args = str.split(" ");
			}
			boolean taskMode = setMode(args[0]);
			Map<String, Object> parsedArgs = new HashMap<>();
			for (int index = 1; index < args.length; index++) {
				parseArg(args[index], parsedArgs);
			}
			setParams(parsedArgs, mode);
			if (taskMode) {
				if(mode == TaskMode.start) {
					options.setParameter(TaskParameter.RUN_ARGS, args);
				}
				boolean progressBars = mode != TaskMode.cleanup && mode != TaskMode.setup;
				performTask(mode, side, progressBars, true);
			} else if (mode == TaskMode.help) {
				if(helpCommand == null) {
					for (TaskMode mode : TaskMode.values()) {
						log(new Ansi()
								.fgBrightMagenta().a(" - " + String.format("%-12s", mode.name())).fgDefault()
								.fgGreen().a(" ").a(mode.desc).fgDefault().toString());
					}
				}
				else {
					log(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", helpCommand.name())).fgDefault().fgGreen().a(" ").a(helpCommand.desc).fgDefault().toString());
					if(helpCommand.params.length > 0) log("Optional parameters:");
					for(TaskParameter param : helpCommand.params) {
						log(new Ansi().a(" ").fgCyan().a(String.format("%-14s", param.name)).a(" - ").fgBrightYellow().a(param.desc).fgDefault().toString());
					}
				}
			} else if (mode != TaskMode.exit) {
				log("Unknown command. Type 'help' for list of available commands");
			}
			args = new String[]{};
			options.resetDefaults();
			if (!startedWithNoParams || mode == TaskMode.exit)
				exit = true;
			mode = null;
			helpCommand = null;
			executeTimes++;
		}
	}

	private void setParams(Map<String, Object> parsedArgs, TaskMode mode) {
		side = Side.ANY;
		for (Map.Entry<String, Object> arg : parsedArgs.entrySet()) {
			Object value = arg.getValue();
			String name = arg.getKey();
			if(value == null) {
				switch (name) {
					case "client":
						side = Side.CLIENT;
						break;
					case "server":
						side = Side.SERVER;
						break;
				}
				if(mode == TaskMode.help) {
					try {
						helpCommand = TaskMode.valueOf(name);
					}
					catch (IllegalArgumentException ignored) {}
				}
				if(mode == TaskMode.setup) {
					options.setParameter(TaskParameter.SETUP_VERSION, name);
				}
			}
			else {
				options.setParameter(nameToParamMap.get(name), value);
			}
		}
	}

	private boolean setMode(String name) {
		try {
			mode = TaskMode.valueOf(name);
			return mode.taskClass != null;
		}
		catch (IllegalArgumentException ignored) {}
		return false;
	}

	private static void parseArg(String arg, Map<String, Object> map) {
		int equalSign = arg.indexOf('=');
		if (arg.startsWith("-") && equalSign > 0) {
			String name = arg.substring(1, equalSign);
			String[] values = arg.substring(equalSign + 1).split(",");
			Object value;
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].replace("\\n", "\n").replace("\\t", "\t");
			}
			if(values.length == 1) {
				try {
					value = Integer.parseInt(values[0]);
				}
				catch (NumberFormatException e) {
					if(values[0].equals("false") || values[0].equals("true")) {
						value = Boolean.parseBoolean(values[0]);
					}
					else {
						value = values[0];
					}
				}
			}
			else {
				value = values;
			}
			map.put(name, value);
		} else if (arg.startsWith("-")) {
			String name = arg.substring(1);
			map.put(name, true);
		} else {
			map.put(arg, null);
		}
	}
	

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public boolean yesNoInput(String title, String msg) {
		log(msg);
		return console.readLine().toLowerCase().equals("yes");
	}

	@Override
	public String inputString(String title, String msg) {
		log(msg);
		return console.readLine().toLowerCase();
	}

	public void showMessage(String title, String msg, int type) {
		Ansi typeName = new Ansi();
		switch (type) {
		case Task.INFO:
			typeName = typeName.fgBlue().a("INFO").fgDefault();
			break;
		case Task.WARNING:
			typeName = typeName.fgYellow().a("WARNING").fgDefault();
			break;
		case Task.ERROR:
			typeName = typeName.fgRed().a("ERROR").fgDefault();
			break;
		}
		log("[" + typeName + "]: " + msg);
	}

	@Override
	public void setActive(boolean active) {
	}

	@Override
	public String getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public void setCurrentVersion(String version) {
		currentVersion = version;
	}

	private static String progressString(int progress, String progressMsg, String prefix) {
		Ansi string = new Ansi(100);
		string
				.eraseLine()
				.a(" ")
				.a(String.format("%-7s", prefix))
				.a(String.join("", Collections.nCopies(progress == 0 ? 2 : 2 - (int) (Math.log10(progress)), " ")))
				.a(String.format(" %d%% [", progress))
				.fgGreen()
				.a(String.join("", Collections.nCopies(progress / 10, "=")))
				.fgDefault()
				.a(String.join("", Collections.nCopies(10 - progress / 10, "-")))
				.a("] ")
				.a(progressMsg);

		return string.toString();
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		synchronized (this) { 
			progressStrings[side] = progressMessage;
	        System.out.print(new Ansi().cursorUpLine(progresses.length));
			for(int i = 0; i < progresses.length; i++) {
				System.out.println(progressString(progresses[i], progressStrings[i], progressBarNames[i]));
			}
		}
	}

	@Override
	public void setProgress(int side, int progress) {
		synchronized (this) { 
			progresses[side] = progress;
	        System.out.print(new Ansi().cursorUpLine(progresses.length));
			for(int i = 0; i < progresses.length; i++) {
				System.out.println(progressString(progresses[i], progressStrings[i], progressBarNames[i]));
			}
		}
	}

	@Override
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
		progresses = new int[tasks.size()];
		progressStrings = new String[tasks.size()];
		progressBarNames = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
			String name = mode.name;
			if(tasks.get(i).side == Side.CLIENT || tasks.get(i).side == Side.SERVER) {
				name = tasks.get(i).side.name;
			}
			progresses[i] = 0;
			progressStrings[i] = "Idle";
			progressBarNames[i] = name;
			System.out.println();
        }
	}

	@Override
	public void clearProgressBars() {
		progresses = new int[0];
		progressStrings = new String[0];
		progressBarNames = new String[0];
	}

}
