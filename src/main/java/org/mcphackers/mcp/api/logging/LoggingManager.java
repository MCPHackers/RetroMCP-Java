package org.mcphackers.mcp.api.logging;

import org.fusesource.jansi.Ansi;

public class LoggingManager {
	public static final String INFO = new Ansi().fgDefault().a("[INFO] ").toString();
	public static final String WARNING = new Ansi().fgYellow().a("[WARNING] ").toString();
	public static final String ERROR = new Ansi().fgRed().a("[ERROR] ").toString();

	public void log(String message) {
		System.out.println(message);
	}

	public void info(String message) {
		System.out.println(INFO + message);
	}

	public void warning(String message) {
		System.out.println(WARNING + message);
	}

	public void error(String message) {
		System.out.println(ERROR + message);
	}

	static {
		System.setErr(System.out);
	}
}
