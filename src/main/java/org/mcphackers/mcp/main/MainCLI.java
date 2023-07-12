package org.mcphackers.mcp.main;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.MCP;

import java.util.Scanner;

public class MainCLI extends MCP {
	private static final Ansi LOGO = new Ansi().fgCyan().a("  _____      _             ").fgYellow().a("__  __  _____ _____").a('\n').fgCyan().a(" |  __ \\    | |           ").fgYellow().a("|  \\/  |/ ____|  __ \\").a('\n').fgCyan().a(" | |__) |___| |_ _ __ ___ ").fgYellow().a("| \\  / | |    | |__) |").a('\n').fgCyan().a(" |  _  // _ \\ __| '__/ _ \\").fgYellow().a("| |\\/| | |    |  ___/").a('\n').fgCyan().a(" | | \\ \\  __/ |_| | | (_) ").fgYellow().a("| |  | | |____| |").a('\n').fgCyan().a(" |_|  \\_\\___|\\__|_|  \\___/").fgYellow().a("|_|  |_|\\_____|_|").a('\n').fgDefault();

	public static void main(String[] args) {
		AnsiConsole.systemInstall();
		System.out.println(LOGO.toString());

		Scanner scanner = new Scanner(System.in);
		System.out.print(new Ansi().fgCyan().a("> ").toString());
		while (scanner.hasNextLine()) {
			System.out.print(new Ansi().fgCyan().a("> ").toString());
			String line = scanner.nextLine();
			if (line.equalsIgnoreCase("exit")) {
				System.exit(0);
			}
		}
	}
}
