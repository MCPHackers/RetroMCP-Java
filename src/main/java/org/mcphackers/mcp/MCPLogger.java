package org.mcphackers.mcp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import org.fusesource.jansi.Ansi;
import org.mcphackers.mcp.tools.FileUtil;

public class MCPLogger {
	
	private BufferedWriter writer;
	
	public MCPLogger() {
		try {
			FileUtil.createDirectories(Paths.get("logs"));
			writer = new BufferedWriter(new FileWriter("logs/mcp.log"));
		} catch (IOException ignored) {}
	}
	
//	public void printProgressBars(List<SideThread> threads) {
//		StringBuilder s = new StringBuilder(new Ansi().cursorUp(threads.size() + 1).a('\n').toString());
//		for (SideThread thread : threads) {
//			ProgressInfo dinfo = thread.getInfo();
//			String side = thread.getSideName();
//			s.append(progressString(dinfo.getTotal(), dinfo.getCurrent(), dinfo.getMessage(), side + ":"));
//		}
//		s.append(new Ansi().reset().toString());
//		String progressBar = s.toString();
//		if(!progressBar.equals(cachedProgressBar)) print(progressBar);
//		cachedProgressBar = progressBar;
//	}

	private static String progressString(long total, long current, String progressMsg, String prefix) {
		Ansi string = new Ansi(100);
		if (total != 0) {
			int percent = (int) (current * 100 / total);
			string
					.eraseLine()
					.a(" ")
					.a(String.format("%-7s", prefix))
					.a(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
					.a(String.format(" %d%% [", percent))
					.fgGreen()
					.a(String.join("", Collections.nCopies(percent / 10, "=")))
					.fgDefault()
					.a(String.join("", Collections.nCopies(10 - percent / 10, "-")))
					.a("] ")
					.a(progressMsg);
		}

		return string.a("\n").toString();
	}

	public void log(String msg) {
		String msgNoAnsi =  msg.replaceAll("\u001B\\[[;\\d]*m", "");
		try {
			writer.write(msgNoAnsi + "\n");
			writer.flush();
		} catch (IOException ignored) {}
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException ignored) {}
	}
}
