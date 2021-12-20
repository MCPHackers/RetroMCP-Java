package org.mcphackers.mcp;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.tasks.info.*;
import org.mcphackers.mcp.tools.ProgressInfo;

import java.util.*;

public class MCP {

    public static EnumMode mode;
    public static MCPLogger logger;
    public static Scanner input;
    private static Ansi logo = 
    		new Ansi()
            .fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____  ").a('\n')
            .fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\ ").a('\n')
            .fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n')
            .fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/ ").a('\n')
            .fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |     ").a('\n')
            .fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|     ").a('\n')
            .fgDefault();

    static {
    	AnsiConsole.systemInstall();
    }

    public static void main(String[] args) {
        logger = new MCPLogger();
        input = new Scanner(System.in);

        boolean startedWithNoParams = false;
        boolean exit = false;

        if (args.length <= 0) {
            startedWithNoParams = true;
            logger.println(logo);
            logger.println("Enter a command to execute:");
        }
        int executeTimes = 0;
        while (startedWithNoParams && !exit || !startedWithNoParams && executeTimes < 1) {
            while (args.length < 1) {
                logger.print(new Ansi().fgBrightCyan().a("> ").fgRgb(255,255,255));
                String str = input.nextLine();
                logger.print(new Ansi().fgDefault());
                args = str.split(" ");
            }
            if (setMode(args[0])) {
                HashMap parsedArgs = new HashMap();
                for (int index = 1; index < args.length; index++) {
                    parseArg(args[index], parsedArgs);
                }
                Iterator<Map.Entry> iterator = parsedArgs.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry mapElement = iterator.next();
                    String[] e = (String[]) mapElement.getValue();
                    if (e.length == 0) {
                        setParameter((String) mapElement.getKey(), true);
                        continue;
                    }
                    if (e.length == 1) {
                        try {
                            setParameter((String) mapElement.getKey(), Integer.parseInt(e[0]));
                            continue;
                        } catch (NumberFormatException ex) {
                            setParameter((String) mapElement.getKey(), e[0]);
                            continue;
                        }
                    }
                    if (e.length > 1) {
                        setParameter((String) mapElement.getKey(), e);
                        continue;
                    }
                }
                start();
            } else if (mode == EnumMode.help) {
                List<String[]> commands = new ArrayList();
                for (EnumMode mode : EnumMode.values()) {
                	commands.add(new String[]{mode.name(), mode.desc});
            	}

                for (int i = 0; i < commands.size(); i++) {
                    for (int i2 = 0; i2 < commands.get(i).length; i2++) {
                        if (i2 == 0)
                            logger.info(new Ansi().fgBrightMagenta().a(" - " + String.format("%-12s", commands.get(i)[i2])).fgDefault(), false);
                        else
                            logger.info(new Ansi().fgGreen().a(" ").a(commands.get(i)[i2]).fgDefault(), false);
                    }

                    logger.newLine();
                }
            } else if (mode != EnumMode.exit) {
                logger.println("Unknown command. Type 'help' for list of available commands");
            }
            args = new String[]{};
            Conf.resetConfig();
            if (!startedWithNoParams || mode == EnumMode.exit)
                exit = true;
            mode = null;
            executeTimes++;
        }
        shutdown();
    }

    private static void shutdown() {
        input.close();
        //TODO: Close logger (unimplemented)
	}

	private static void start() {
        TaskInfo task = getTaskInfo(mode);
        try {
            logger.info(new Ansi().fgMagenta().a("====== ").fgDefault().a(task.title()).fgMagenta().a(" ======").fgDefault());
            processTask(task);
            logger.info(new Ansi().a('\n').fgBrightGreen().a(task.successMsg()).fgDefault());
        } catch (Exception e) {
        	Exception ex = e;
        	String msg = e.getMessage();
        	try {
        		logger.info(new Ansi().a('\n').fgBrightRed().a(task.failMsg()).fgDefault());
        	}
        	catch (NullPointerException nullException) {
        		ex = nullException;
        		msg = "Invalid task detected!";
        	}
            if (Conf.debug) ex.printStackTrace();
            else {
            	if(msg != null) {
            		logger.info(msg);
            	}
                logger.info("Use -debug for more info");
            }
        }
    }

    public static TaskInfo getTaskInfo(EnumMode enumMode) {
        return enumMode.task;
    }

    private static void processTask(TaskInfo task) throws Exception {
    	if(task.isMultiThreaded()) {
    		processMultitasks(task);
    	}
    	else {
    		task.newTask(0).doTask();
    	}
    }
    
    private static void processMultitasks(TaskInfo task) throws Exception {
        List<SideThread> threads = new ArrayList<SideThread>();
        int threadsAmount = 2;
        logger.newLine();
        for (int i = 0; i < threadsAmount; i++) {
            logger.newLine();
            threads.add(new SideThread(i, task.newTask(i)));
            threads.get(i).start();
        }
        boolean working = true;
        while (working) {
            Thread.sleep(10);
            logger.printProgressBars(threads);
            working = false;
            Exception ex = null;
            for(SideThread thread : threads) {
                if (thread.exception != null) {
                	ex = thread.exception;
                }
            	working = !working ? thread.isAlive() : true;
            }
            if(ex != null) {
            	for(SideThread thread : threads) {
            		thread.stopThread();
            	}
            	throw ex; 
            }
        }
    }

    private static void parseArg(String arg, HashMap<String, String[]> map) {
        int equalSign = arg.indexOf('=');
        if (arg.startsWith("-") && equalSign > 0) {
            String name = arg.substring(1, equalSign);
            String[] values = arg.substring(equalSign + 1).split(",");
            for (int i = 0; i < values.length; i++) {
            	values[i] = values[i].replace("\\n", "\n").replace("\\t", "\t");
            }
            map.put(name, values);
        } else if (arg.startsWith("-")) {
            String name = arg.substring(1);
            String[] value = new String[]{};
            map.put(name, value);
        }
    }

    private static void setParameter(String name, int value) {
        switch (name) {
        	case "side":
        		Conf.onlySide = value;
        		break;
        }
    }

    private static void setParameter(String name, String value) {
        switch (name) {
	        case "ind":
	        case "indention":
	        	Conf.indentionString = value;
	            break;
        }
    }

    private static void setParameter(String name, String[] value) {
        switch (name) {
	        case "ignore":
	            Conf.ignorePackages = value;
	            break;
        }
    }

    private static void setParameter(String name, boolean value) {
        switch (name) {
            case "debug":
                Conf.debug = value;
                break;
            case "patch":
                Conf.patch = value;
                break;
        }
    }

    private static boolean setMode(String name) {
    	try {
            mode = EnumMode.valueOf(name);
            return mode.task != null;
    	}
    	catch (IllegalArgumentException ex) {}
        return false;
    }
}