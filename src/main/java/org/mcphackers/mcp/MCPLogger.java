package org.mcphackers.mcp;

import java.util.Collections;
import java.util.List;

import org.fusesource.jansi.Ansi;
import org.mcphackers.mcp.tools.ProgressInfo;

public class MCPLogger {
	
	public MCPLogger() {
	}
	
	public void newLine() {
		System.out.println();
	}
	
	public void print(Object msg) {
		System.out.print(msg);
	}
	
	public void println(Object msg) {
		System.out.println(msg);
	}
	
	public void printProgressBars(List<SideThread> threads) {
		StringBuilder s = new StringBuilder(new Ansi().cursorUp(threads.size() + 1).a('\n').toString());
        for (int i = 0; i < threads.size(); i++) {
            ProgressInfo dinfo = threads.get(i).getInfo();
            String side = threads.get(i).getSideName();
            s.append(progressString(dinfo.getTotal(), dinfo.getCurrent(), dinfo.getMessage(), side + ":"));
        }
        s.append(new Ansi().restoreCursorPosition().toString());
        print(s.toString());
	}

    private static String progressString(long total, long current, String progressMsg, String prefix) {
        Ansi string = new Ansi(100);
        if (total != 0) {
            int percent = (int) (current * 100 / total);
            string
                    .a(" ")
                    .a(String.format("%-7s", prefix))
                    .a(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                    .a(String.format(" %d%% [", percent))
                    .fgGreen()
                    .a(String.join("", Collections.nCopies(percent / 10, "=")))
                    .fgDefault()
                    .a(String.join("", Collections.nCopies(10 - percent / 10, "-")))
                    .a("] ")
                    .a(progressMsg)
                    .a(String.join("", Collections.nCopies(100, " ")));
        }

        return string.a("\n").toString();
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
	
	public void info(String msg, boolean newLine) {
		//TODO: Add some kind of logging to a file
		if(newLine) {
			System.out.println(msg);
		} else {
			System.out.print(msg);
		}
	}
	
	public void warning(String msg) {
		System.err.println(new Ansi().fgBrightYellow().a(msg).fgDefault());
		//TODO: Add some kind of logging to a file
	}
	
	public void error(String msg) {
		//TODO: Add some kind of logging to a file
		System.err.println(new Ansi().fgBrightRed().a(msg).fgDefault());
	}
}
