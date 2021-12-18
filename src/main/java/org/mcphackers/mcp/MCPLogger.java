package org.mcphackers.mcp;

import org.fusesource.jansi.Ansi;

public class MCPLogger {
	/**
	 * Prints a new line
	 */
	public void newLine() {
		System.out.println();
	}
	
	/**
	 * Prints text without logging it to a file
	 * @param msg
	 */
	public void print(Object msg) {
		System.out.print(msg);
	}
	public void println(Object msg) {
		System.out.println(msg);
	}
	
	public void info(Ansi msg) {
		info(msg.toString());
	}
	
	public void info(Ansi msg, boolean newLine) {
		info(msg.toString(), newLine);
	}
	
	public void info(String msg) {
		info(msg, true);
	}
	
	/**
	 * Logs and prints some message
	 * @param msg
	 * @param newLine
	 */
	public void info(String msg, boolean newLine) {
		// Add some kind of logging to a file
		if(newLine) {
			System.out.println(msg);
		} else {
			System.out.print(msg);
		}
	}
	
	/**
	 * Logs an error message and prints it in red
	 * @param msg
	 */
	public void error(String msg) {
		// Add some kind of logging to a file
		System.err.println(new Ansi().fgBrightRed().a(msg).fgDefault());
	}
}
