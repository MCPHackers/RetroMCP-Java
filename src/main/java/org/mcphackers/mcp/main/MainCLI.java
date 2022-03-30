package org.mcphackers.mcp.main;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.MCPLogger;
import org.mcphackers.mcp.TaskParameter;
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
	private static MCPLogger logger;
	private static Scanner input;
	private static TaskMode mode;
	private static TaskMode helpCommand;

	public static void main(String[] args) throws Exception {
		if(System.console() != null) {
			AnsiConsole.systemInstall();
		}
		else {
			System.err.println("Error: Could not find console. Launching GUI instead");
			MainGUI.main(args);
			return;
		}
		new MainCLI(args);
	}
	
	public MainCLI(String[] args) {

		resetConfig();
		attemptToDeleteUpdateJar();
		logger = new MCPLogger();
		input = new Scanner(System.in);
		logger.log("Operating system: " + System.getProperty("os.name"));
		logger.log("RetroMCP " + MCP.VERSION);

		boolean startedWithNoParams = false;
		boolean exit = false;
		String version = null;
		if(Files.exists(Paths.get(MCPPaths.VERSION))) {
			try {
				VersionsParser.setCurrentVersion(new String(Files.readAllBytes(Paths.get(MCPPaths.VERSION))));
				version = new Ansi().a("Current version: ").fgBrightCyan().a(VersionsParser.getCurrentVersion()).fgDefault().toString();
			} catch (Exception e) {
				version = new Ansi().fgBrightRed().a("Unable to get current version!").fgDefault().toString();
			}
		}
		if (args.length <= 0) {
			startedWithNoParams = true;
			logger.println(logo);
			JavaCompiler c = ToolProvider.getSystemJavaCompiler();
			if (c == null) {
				// Likely a JRE
				logger.println(new Ansi().fgBrightRed().a("Error: Java Development Kit not detected! Compilation will fail!").toString());
				logger.println("Using Java from " + Paths.get(Util.getJava()).toAbsolutePath());
			}
			if(version != null) logger.info(version);
			logger.println(new Ansi().fgDefault().a("Enter a command to execute:").toString());
		}
		int executeTimes = 0;
		while (startedWithNoParams && !exit || !startedWithNoParams && executeTimes < 1) {
			while (args.length < 1) {
				logger.print(new Ansi().fgBrightCyan().a("> ").fgRgb(255,255,255));
				String str = "";
				try {
					str = input.nextLine();
				} catch (NoSuchElementException ignored) {}
				logger.print(new Ansi().fgDefault());
				args = str.split(" ");
			}
			boolean taskMode = setMode(args[0]);
			Map<String, Object> parsedArgs = new HashMap<>();
			for (int index = 1; index < args.length; index++) {
				parseArg(args[index], parsedArgs);
			}
			setParams(parsedArgs, mode);
			if (taskMode) {
				if(mode == TaskMode.startclient || mode == TaskMode.startserver) {
					runArgs = args;
				}
				start();
			} else if (mode == TaskMode.help) {
				if(helpCommand == null) {
					for (TaskMode mode : TaskMode.values()) {
						logger.println(new Ansi()
								.fgBrightMagenta().a(" - " + String.format("%-12s", mode.name())).fgDefault()
								.fgGreen().a(" ").a(mode.desc).fgDefault());
					}
				}
				else {
					logger.println(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", helpCommand.name())).fgDefault().fgGreen().a(" ").a(helpCommand.desc).fgDefault());
					if(helpCommand.params.length > 0) logger.println("Optional parameters:");
					for(String param : helpCommand.params) {
						logger.println(new Ansi().a(" ").fgCyan().a(String.format("%-10s", param)).a(" - ").fgBrightYellow().a(TaskMode.getParamDesc(param)).fgDefault());
					}
				}
			} else if (mode != TaskMode.exit) {
				logger.println("Unknown command. Type 'help' for list of available commands");
			}
			args = new String[]{};
			resetConfig();
			if (!startedWithNoParams || mode == TaskMode.exit)
				exit = true;
			mode = null;
			helpCommand = null;
			executeTimes++;
		}
		shutdown();
	}

	private void setParams(Map<String, Object> parsedArgs, TaskMode mode) {
		for (Map.Entry<String, Object> arg : parsedArgs.entrySet()) {
			Object value = arg.getValue();
			String name = arg.getKey();
			if(value == null) {
				switch (name) {
					case "client":
					case "server":
						setParameter(name, true);
					break;
				}
				if(mode == TaskMode.help) {
					try {
						helpCommand = TaskMode.valueOf(name);
					}
					catch (IllegalArgumentException ignored) {}
				}
				if(mode == TaskMode.setup) {
					setParameter("setupversion", name);
				}
			}
			else if(value instanceof Integer) {
				setParameter(name, (Integer)value);
			}
			else if(value instanceof Boolean) {
				setParameter(name, (Boolean)value);
			}
			else if(value instanceof String) {
				setParameter(name, (String)value);
			}
			else if(value instanceof String[]) {
				setParameter(name, (String[])value);
			}
		}
	}

	private static void shutdown() {
		input.close();
		logger.close();
	}

	private static boolean setMode(String name) {
//		try {
//			mode = EnumMode.valueOf(name);
//			return mode.task != null;
//		}
//		catch (IllegalArgumentException ignored) {}
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

	private void start() {
//		TaskInfo task = getTaskInfo(mode);
//		try {
//			logger.info(new Ansi().fgMagenta().a("====== ").fgDefault().a(task.title()).fgMagenta().a(" ======").fgDefault().toString());
//			processTask(task);
//			logger.resetProgressString();
//			String completemsg = task.successMsg();
//			if(completemsg != null) {
//				logger.info(new Ansi().a('\n').fgBrightGreen().a(completemsg).fgDefault().toString());
//				List<String> errors = task.getInfoList();
//				for(String error : errors) {
//					logger.info(" " + error.replace("\n", "\n "));
//				}
//			}
//		} catch (Exception e) {
//			logger.info(new Ansi().a('\n').fgBrightRed().a(task.failMsg()).fgDefault().toString());
//			List<String> errors = task.getInfoList();
//			for(String error : errors) {
//				logger.info(" " + error.replace("\n", "\n "));
//			}
//			if (debug) e.printStackTrace();
//			else {
//				String msg = e.getMessage();
//				if(msg != null) {
//					logger.info(msg);
//				}
//				logger.info("Use -debug for more info");
//			}
//		}
//		task.clearInfoList();
	}
	
	public boolean debug;
	public boolean patch;
	public boolean srcCleanup;
	public String[] ignorePackages;
	public int onlySide;
	public String indentionString;
	public boolean fullBuild;
	public boolean runBuild;
	public String setupVersion;
	public String[] runArgs;

	public void resetConfig() {
		debug = false;
		patch = true;
		srcCleanup = false;
		onlySide = -1;
		ignorePackages = new String[]{"paulscode", "com/jcraft", "isom"};
		indentionString = "\t";
		fullBuild = false;
		runBuild = false;
		setupVersion = null;
		runArgs = null;
	}

	public void setParameter(String name, int value) {
		switch (name) {
			case "side":
				onlySide = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	public void setParameter(String name, String value) {
		switch (name) {
			case "ind":
			case "indention":
				indentionString = value;
				break;
			case "ignore":
				ignorePackages = new String[] {value};
				break;
			case "setupversion":
				setupVersion = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	public void setParameter(String name, String[] value) {
		switch (name) {
			case "ignore":
				ignorePackages = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	public void setParameter(String name, boolean value) {
		switch (name) {
			case "debug":
				debug = value;
				break;
			case "patch":
				patch = value;
				break;
			case "client":
				onlySide = value ? 0 : onlySide;
				break;
			case "server":
				onlySide = value ? 1 : onlySide;
				break;
			case "src":
				srcCleanup = value;
				break;
			case "fullbuild":
				fullBuild = value;
				break;
			case "runbuild":
				runBuild = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	@Override
	public void log(String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getBooleanParam(TaskParameter param) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] getStringArrayParam(TaskParameter param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStringParam(TaskParameter param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIntParam(TaskParameter param) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setProgressBarActive(int side, boolean active) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProgress(int side, String progressMessage, int progress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProgress(int side, int progress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int askForInput(String title, String msg) {
		log(msg);
		return input.nextLine().toLowerCase().equals("yes") ? 0 : 1;
	}

}
