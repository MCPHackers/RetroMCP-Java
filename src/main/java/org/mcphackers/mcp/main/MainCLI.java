package org.mcphackers.mcp.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.api.task.Task;
import org.mcphackers.mcp.api.task.TaskManager;

public class MainCLI extends MCP {
	private static final Ansi LOGO = new Ansi().fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____").a('\n').fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\").a('\n').fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n').fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/").a('\n').fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |").a('\n').fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|").a('\n').fgDefault();

	public static void main(String[] args) {
		AnsiConsole.systemInstall();
		System.out.println(LOGO.toString());

		MainCLI mcp = new MainCLI();
		mcp.initializeMCP();
		TaskManager taskManager = mcp.getTaskManager();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String prompt = new Ansi().fgCyan().a("> ").toString();
		while (true) {
			try {
				System.out.print(prompt);
				String line = reader.readLine();
				if ("exit".equalsIgnoreCase(line)) {
					System.exit(0);
				}

				for (Task task : taskManager.getTasks()) {
					String[] splitLine = line.split(" ");
					if (task.getName().equalsIgnoreCase(splitLine[0])) {
						taskManager.executeTask(mcp, task, Arrays.copyOfRange(splitLine, 1, splitLine.length));
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
