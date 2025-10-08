package org.mcphackers.mcp.main;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONObject;
import org.mcphackers.mcp.Language;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tasks.mode.TaskParameterMap;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;
import org.mcphackers.mcp.tools.versions.json.Version;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * CLI implementation of MCP
 */
public class MainCLI extends MCP {
	private static final Ansi LOGO =
			new Ansi()
					.fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____").a('\n')
					.fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\").a('\n')
					.fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n')
					.fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/").a('\n')
					.fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |").a('\n')
					.fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|").a('\n')
					.fgDefault();
	private final Scanner consoleInput = new Scanner(System.in);
	private TaskMode mode;
	private Side side = Side.ANY;
	private TaskMode helpCommand;
	private Version currentVersion;

	private int[] progresses;
	private String[] progressStrings;
	private String[] progressBarNames;

	public MainCLI(String[] args) {
		isGUI = false;
		options.resetDefaults();
		changeLanguage(Language.ENGLISH); // Some CLI text is hardcoded in English
		log("RetroMCP " + MCP.VERSION);

		boolean startedWithNoParams = false;
		boolean exit = false;
		String version = null;
		Path versionPath = Paths.get(MCPPaths.VERSION);
		VersionParser versionParser = VersionParser.getInstance();
		List<VersionData> versions = versionParser.getVersions();
		if (Files.exists(versionPath)) {
			try {
				currentVersion = Version.from(new JSONObject(new String(Files.readAllBytes(versionPath))));
				VersionData data = versionParser.getVersion(currentVersion.id);
				if (data != null) {
					version = new Ansi().a("Current version: ").fgBrightCyan().a(data.toString()).fgDefault().toString();
				}
			} catch (Exception e) {
				version = new Ansi().fgBrightRed().a("Unable to get current version!").fgDefault().toString();
			}
		}
		if (args.length == 0) {
			startedWithNoParams = true;
			log(LOGO.toString());
			JavaCompiler c = ToolProvider.getSystemJavaCompiler();
			if (c == null) {
				// Likely a JRE
				log(new Ansi().fgBrightRed().a("Error: Java Development Kit is required to recompile!").toString());
				log("Using Java from " + Paths.get(Util.getJava()).toAbsolutePath());
			}
			if (Util.getJavaVersion() > 8) {
				warning("WARNING: JDK " + Util.getJavaVersion() + " is being used! Java 8 is recommended.");
			}
			if (version != null) log(version);
			log(new Ansi().fgDefault().a("Enter a command to execute:").toString());
		}
		int executeTimes = 0;
		while (startedWithNoParams && !exit || !startedWithNoParams && executeTimes < 1) {
			while (args.length < 1) {
				System.out.print(new Ansi().fgBrightCyan().a("> ").fgDefault());
				String str = null;
				try {
					if (consoleInput.hasNextLine()) {
						str = consoleInput.nextLine();
					} else {
						// Only seems to occur during EOF, might occur in other cases?
						System.exit(0);
					}
				} catch (NoSuchElementException ignored) {
				}
				if (str == null) {
					mode = TaskMode.EXIT;
				} else {
					str = str.trim();
					if (str.isEmpty()) {
						continue;
					}
					System.out.print(new Ansi().fgDefault());
					args = str.split(" ");
				}
			}
			boolean taskMode = setMode(args[0]);
			Map<String, Object> parsedArgs = new HashMap<>();
			for (int index = 1; index < args.length; index++) {
				parseArg(args[index], parsedArgs);
			}
			setParams(parsedArgs, mode);
			if (mode == TaskMode.SETUP && versions != null) {
				if (versionParser.getVersion(getOptions().getStringParameter(TaskParameter.SETUP_VERSION)) == null) {
					log(new Ansi().fgMagenta().a("================ ").fgDefault().a("Current versions").fgMagenta().a(" ================").fgDefault().toString());
					log(getTable(versions));
					log(new Ansi().fgMagenta().a("==================================================").fgDefault().toString());
				}
			}
			if (taskMode) {
				performTask(mode, side);
			} else if (mode == TaskMode.HELP) {
				if (helpCommand == null) {
					for (TaskMode mode : TaskMode.registeredTasks) {
						log(new Ansi()
								.fgBrightMagenta().a(" - " + String.format("%-12s", mode.getName())).fgDefault()
								.fgGreen().a(" ").a(mode.getDesc()).fgDefault().toString());
					}
				} else {
					log(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", helpCommand.getName())).fgDefault().fgGreen().a(" ").a(helpCommand.getDesc()).fgDefault().toString());
					if (helpCommand.params.length > 0) log("Optional parameters:");
					for (TaskParameter param : helpCommand.params) {
						log(new Ansi().a(" ").fgCyan().a(String.format("%-14s", param.name)).a(" - ").fgBrightYellow().a(param.getDesc()).fgDefault().toString());
					}
				}
			} else if (mode != TaskMode.EXIT) {
				log("Unknown command. Type 'help' for list of available commands");
			}
			args = new String[]{};
			options.resetDefaults();
			if (!startedWithNoParams || mode == TaskMode.EXIT)
				exit = true;
			mode = null;
			helpCommand = null;
			executeTimes++;
		}
	}

	public static void main(String[] args) {
		AnsiConsole.systemInstall();
		new MainCLI(args);
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
			if (values.length == 1) {
				try {
					value = Integer.parseInt(values[0]);
				} catch (NumberFormatException e) {
					if (values[0].equals("false") || values[0].equals("true")) {
						value = Boolean.parseBoolean(values[0]);
					} else {
						value = values[0];
					}
				}
			} else {
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

	@SuppressWarnings("unchecked")
	private static String getTable(List<VersionData> versions) {
		int rows = (int) Math.ceil(versions.size() / 3D);
		List<String>[] tableList = new List[rows];
		for (int i = 0; i < tableList.length; i++) {
			tableList[i] = new ArrayList<>();
		}
		StringBuilder table = new StringBuilder();
		int index = 0;
		for (VersionData ver : versions) {
			tableList[index % rows].add(new Ansi().fgBrightCyan().a(" - ").fgDefault().fgCyan().a(String.format("%-16s", ver.id)).fgDefault().toString());
			index++;
		}
		for (int i = 0; i < tableList.length; i++) {
			for (String ver : tableList[i]) {
				table.append(ver);
			}
			if (i < tableList.length - 1) table.append("\n");
		}
		return table.toString();
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

	private void setParams(Map<String, Object> parsedArgs, TaskMode mode) {
		side = Side.ANY;
		for (Map.Entry<String, Object> arg : parsedArgs.entrySet()) {
			Object value = arg.getValue();
			String name = arg.getKey();
			if (value == null) {
				switch (name) {
					case "client":
						side = Side.CLIENT;
						break;
					case "server":
						side = Side.SERVER;
						break;
					case "merged":
						side = Side.MERGED;
						break;
				}
				if (mode == TaskMode.HELP) {
					for (TaskMode taskMode : TaskMode.registeredTasks) {
						if (taskMode.getName().equals(name)) {
							helpCommand = taskMode;
							break;
						}
					}
				}
				if (mode == TaskMode.SETUP) {
					setParameter(TaskParameter.SETUP_VERSION, name);
				}
			} else {
				TaskParameter param = TaskParameterMap.get(name);
				if (param != null) {
					safeSetParameter(param, value.toString());
				} else {
					log("Unrecognized option: " + name);
				}
			}
		}
	}

	private boolean setMode(String name) {
		for (TaskMode taskMode : TaskMode.registeredTasks) {
			if (taskMode.getName().equals(name)) {
				mode = taskMode;
				return mode.taskClass != null;
			}
		}
		return false;
	}

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public void warning(String msg) {
		System.out.println(Ansi.ansi().fgYellow().a(msg).fgDefault());
	}

	@Override
	public void error(String msg) {
		System.out.println(Ansi.ansi().fgRed().a(msg).fgDefault());
	}

	@Override
	public boolean yesNoInput(String title, String msg) {
		log(msg);
		String line = consoleInput.nextLine();
		return line != null && line.equalsIgnoreCase("yes");
	}

	@Override
	public String inputString(String title, String msg) {
		log(msg);
		String line = consoleInput.nextLine();
		return line == null ? "" : line.toLowerCase();
	}

	@Override
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
	public void showMessage(String title, String msg, Throwable e) {
		Ansi typeName = new Ansi().fgRed().a("ERROR").fgDefault();
		if (msg != null) {
			log("[" + typeName + "]: " + msg);
		}
		e.printStackTrace();
	}

	@Override
	public void setActive(boolean active) {
	}

	@Override
	public Version getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public void setCurrentVersion(Version version) {
		currentVersion = version;
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		//TODO logging messages while progress bar is active still breaks;
		synchronized (this) {
			progressStrings[side] = progressMessage;
			System.out.print(new Ansi().cursorUpLine(progresses.length));
			for (int i = 0; i < progresses.length; i++) {
				System.out.println(progressString(progresses[i], progressStrings[i], progressBarNames[i]));
			}
		}
	}

	@Override
	public void setProgress(int side, int progress) {
		//TODO logging messages while progress bar is active still breaks;
		synchronized (this) {
			progresses[side] = progress;
			System.out.print(new Ansi().cursorUpLine(progresses.length));
			for (int i = 0; i < progresses.length; i++) {
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
			String name = mode.getFullName();
			if (tasks.get(i).side == Side.CLIENT || tasks.get(i).side == Side.SERVER) {
				name = tasks.get(i).side.getName();
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

	@Override
	public boolean updateDialogue(String changelog, String version) {
		return yesNoInput("New version found: " + version, changelog + "\n\nAre you sure you want to update?");
	}

}
