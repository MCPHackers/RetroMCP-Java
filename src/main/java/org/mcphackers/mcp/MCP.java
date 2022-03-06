package org.mcphackers.mcp;

import jredfox.selfcmd.SelfCommandPrompt;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MCP {
	
	public static final String VERSION = "v0.3";
	public static EnumMode mode = null;
	public static EnumMode helpCommand = null;
	public static MCPLogger logger;
	public static MCPConfig config;
	public static Scanner input;
	private static final Ansi logo =
			new Ansi()
			.fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____  ").a('\n')
			.fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\ ").a('\n')
			.fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n')
			.fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/ ").a('\n')
			.fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |     ").a('\n')
			.fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|     ").a('\n')
			.fgDefault();

	private static void attemptToDeleteUpdateJar() {
		long startTime = System.currentTimeMillis();
		boolean keepTrying = true;
		while(keepTrying) {
			try {
				Files.deleteIfExists(Paths.get(MCPConfig.UPDATE_JAR));
				keepTrying = false;
			} catch (IOException e) {
				keepTrying = System.currentTimeMillis() - startTime < 10000;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		SelfCommandPrompt.runWithCMD(SelfCommandPrompt.suggestAppId(), "RetroMCP " + VERSION, args, false, false);
		attemptToDeleteUpdateJar();
		AnsiConsole.systemInstall();
		logger = new MCPLogger();
		config = new MCPConfig();
		input = new Scanner(System.in);
		logger.log("Operating system: " + System.getProperty("os.name"));
		logger.log("RetroMCP " + VERSION);

		boolean startedWithNoParams = false;
		boolean exit = false;
		String version = null;
		if(Files.exists(Paths.get(MCPConfig.VERSION))) {
			try {
				VersionsParser.setCurrentVersion(new String(Files.readAllBytes(Paths.get(MCPConfig.VERSION))));
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
				if(mode == EnumMode.startclient || mode == EnumMode.startserver) {
					config.runArgs = args;
				}
				start();
			} else if (mode == EnumMode.help) {
				if(helpCommand == null) {
					for (EnumMode mode : EnumMode.values()) {
						logger.println(new Ansi()
								.fgBrightMagenta().a(" - " + String.format("%-12s", mode.name())).fgDefault()
								.fgGreen().a(" ").a(mode.desc).fgDefault());
					}
				}
				else {
					logger.println(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", helpCommand.name())).fgDefault().fgGreen().a(" ").a(helpCommand.desc).fgDefault());
					if(helpCommand.params.length > 0) logger.println("Optional parameters:");
					for(String param : helpCommand.params) {
						logger.println(new Ansi().a(" ").fgCyan().a(String.format("%-10s", param)).a(" - ").fgBrightYellow().a(EnumMode.getParamDesc(param)).fgDefault());
					}
				}
			} else if (mode != EnumMode.exit) {
				logger.println("Unknown command. Type 'help' for list of available commands");
			}
			args = new String[]{};
			config.resetConfig();
			if (!startedWithNoParams || mode == EnumMode.exit)
				exit = true;
			mode = null;
			helpCommand = null;
			executeTimes++;
		}
		shutdown();
	}

	private static void setParams(Map<String, Object> parsedArgs, EnumMode mode) {
		for (Map.Entry<String, Object> arg : parsedArgs.entrySet()) {
			Object value = arg.getValue();
			String name = arg.getKey();
			if(value == null) {
				switch (name) {
					case "client":
					case "server":
						config.setParameter(name, true);
					break;
				}
				if(mode == EnumMode.help) {
					try {
						helpCommand = EnumMode.valueOf(name);
					}
					catch (IllegalArgumentException ignored) {}
				}
				if(mode == EnumMode.setup || mode == EnumMode.test) {
					config.setParameter("setupversion", name);
				}
			}
			else if(value instanceof Integer) {
				config.setParameter(name, (Integer)value);
			}
			else if(value instanceof Boolean) {
				config.setParameter(name, (Boolean)value);
			}
			else if(value instanceof String) {
				config.setParameter(name, (String)value);
			}
			else if(value instanceof String[]) {
				config.setParameter(name, (String[])value);
			}
		}
	}

	private static void shutdown() {
		input.close();
		logger.close();
	}

	private static void start() {
		TaskInfo task = getTaskInfo(mode);
		try {
			logger.info(new Ansi().fgMagenta().a("====== ").fgDefault().a(task.title()).fgMagenta().a(" ======").fgDefault().toString());
			processTask(task);
			logger.resetProgressString();
			String completemsg = task.successMsg();
			if(completemsg != null) {
				logger.info(new Ansi().a('\n').fgBrightGreen().a(completemsg).fgDefault().toString());
				List<String> errors = task.getInfoList();
				for(String error : errors) {
					logger.info(" " + error.replace("\n", "\n "));
				}
			}
		} catch (Exception e) {
			logger.info(new Ansi().a('\n').fgBrightRed().a(task.failMsg()).fgDefault().toString());
			List<String> errors = task.getInfoList();
			for(String error : errors) {
				logger.info(" " + error.replace("\n", "\n "));
			}
			if (config.debug) e.printStackTrace();
			else {
				String msg = e.getMessage();
				if(msg != null) {
					logger.info(msg);
				}
				logger.info("Use -debug for more info");
			}
		}
		task.clearInfoList();
	}

	public static TaskInfo getTaskInfo(EnumMode enumMode) {
		return enumMode.task;
	}

	private static void processTask(TaskInfo task) throws Exception {
		if(task.isMultiThreaded()) {
			processMultitasks(task);
		}
		else {
			task.newTask(-1).doTask();
		}
	}
	
	private static void processMultitasks(TaskInfo task) throws Exception {
		List<SideThread> threads = new ArrayList<>();
		if(config.onlySide < 0 || config.onlySide == SideThread.CLIENT) {
			threads.add(new SideThread(SideThread.CLIENT, task.newTask(SideThread.CLIENT)));
		}

		if(config.onlySide < 0 || config.onlySide == SideThread.SERVER) {
			if(VersionsParser.hasServer()) {
				threads.add(new SideThread(SideThread.SERVER, task.newTask(SideThread.SERVER)));
			}
		}
		logger.newLine();
		for (SideThread thread : threads) {
			logger.newLine();
			thread.start();
		}
		boolean working = true;
		Exception ex = null;
		
		while (working) {
			Thread.sleep(10);
			logger.printProgressBars(threads);
			working = false;
			for(SideThread thread : threads) {
				working = working || thread.isAlive();
			}
		}
		logger.printProgressBars(threads);
		for(SideThread thread : threads) {
			if (thread.exception != null) {
				ex = thread.exception;
			}
		}
		if(ex != null) throw ex;
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

	private static boolean setMode(String name) {
		try {
			mode = EnumMode.valueOf(name);
			return mode.task != null;
		}
		catch (IllegalArgumentException ignored) {}
		return false;
	}
}