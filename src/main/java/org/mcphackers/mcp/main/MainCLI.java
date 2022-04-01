package org.mcphackers.mcp.main;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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
import org.mcphackers.mcp.MCPLogger;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tasks.Task;
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
	private MCPLogger logger;
	private TaskMode mode;
	private TaskMode helpCommand;
	private Console console = System.console();
	private Options options = new Options();

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

		options.resetDefaults();
		attemptToDeleteUpdateJar();
		logger = new MCPLogger();
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
				if(mode == TaskMode.startclient || mode == TaskMode.startserver) {
					options.setParameter(TaskParameter.RUN_ARGS, args);
				}
				start();
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
						log(new Ansi().a(" ").fgCyan().a(String.format("%-10s", param)).a(" - ").fgBrightYellow().a(param.desc).fgDefault().toString());
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
						//setParameter(name, true);
					break;
				}
				if(mode == TaskMode.help) {
					try {
						helpCommand = TaskMode.valueOf(name);
					}
					catch (IllegalArgumentException ignored) {}
				}
				if(mode == TaskMode.setup) {
					//setParameter("setupversion", name);
				}
			}
			else if(value instanceof Integer) {
				//setParameter(name, (Integer)value);
			}
			else if(value instanceof Boolean) {
				//setParameter(name, (Boolean)value);
			}
			else if(value instanceof String) {
				//setParameter(name, (String)value);
			}
			else if(value instanceof String[]) {
				//setParameter(name, (String[])value);
			}
		}
	}

	private void shutdown() {
		logger.close();
	}

	private boolean setMode(String name) {
		try {
			mode = TaskMode.valueOf(name);
			//return mode.task != null;
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
	

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public Options getOptions() {
		return options;
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
	public boolean yesNoInput(String title, String msg) {
		log(msg);
		return console.readLine().toLowerCase().equals("yes");
	}

	@Override
	public String inputString(String title, String msg) {
		log(msg);
		return console.readLine().toLowerCase();
	}

	public void showPopup(String title, String msg, int type) {
		String typeName = "INFO";
		switch (type) {
		case Task.WARNING:
			typeName = "WARNING";
			break;
		case Task.ERROR:
			typeName = "ERROR";
			break;
		}
		log("[" + typeName + "]: " + msg);
	}

}
