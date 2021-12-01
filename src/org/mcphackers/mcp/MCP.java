package org.mcphackers.mcp;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.mcphackers.mcp.tools.Decompile;
import org.mcphackers.mcp.tools.DecompileInfo;

public class MCP {

	public static EnumMode mode;
	public static String cd;
	public static PrintStream logger;
	
	public static void main(String args[])
	{
		AnsiConsole.systemInstall();
		logger = System.out;
	    cd = System.getProperty("user.dir");
	    if (args.length < 1) {
	    	logger.println("No arguments given");
	    	return;
	    }
	    HashMap parsedArgs = new HashMap();
	    for(int index = 0; index < args.length; index++)
	    {
	    	parseArg(args[index], parsedArgs);
	    }
	    Iterator<Map.Entry> iterator = parsedArgs.entrySet().iterator();
	    while(iterator.hasNext())
	    {
	    	Map.Entry mapElement = iterator.next();
	    	String[] e = (String[])mapElement.getValue();
	    	if (e.length == 0)
	    	{
	    		setParameter((String)mapElement.getKey(), true);
	    		continue;
	    	}
	    	if (e.length == 1)
	    	{
	    		setParameter((String)mapElement.getKey(), e[0]);
	    		continue;
	    	}
	    }
	    //logger.println(new Ansi().eraseScreen().fgRed().a("Hello").fg(Ansi.Color.GREEN).a(" World").reset());
    	start();
	}
	
	private static void start()
	{
		if(mode == EnumMode.decompile)
		{
			logger.println("Decompiling...");
			logger.println();
			SideThread clientThread = new SideThread(0);
			SideThread serverThread = new SideThread(1);
			clientThread.start();
			serverThread.start();
			boolean alive1 = true;
			boolean alive2 = true;
			while(alive1 || alive2)
			{
				alive1 = clientThread.isAlive();
				alive2 = serverThread.isAlive();
				DecompileInfo dinfo = clientThread.getInfo();
				if(!alive1)
				{
					dinfo = new DecompileInfo("Done!", 1, 1);
				}
				logger.print(new Ansi().eraseLine());
				logger.print(new Ansi().cursorUp(1).a('\r'));
				printProgress(dinfo.counters[1], dinfo.counters[0], dinfo.msg, "Client:");
				dinfo = serverThread.getInfo();
				if(!alive2)
				{
					dinfo = new DecompileInfo("Done!", 1, 1);
				}
				logger.print(new Ansi().eraseLine());
				logger.print(new Ansi().cursorDown(1).a('\r'));
				printProgress(dinfo.counters[1], dinfo.counters[0], dinfo.msg, "Server:");
			}
			logger.print(new Ansi().eraseLine());
			logger.println();
		}
	}
	
	private static void printProgress(long total, long current, String progressMsg, String prefix) {
		if(total == 0) return;
		Ansi string = new Ansi(140);   
	    int percent = (int) (current * 100 / total);
	    string
			.a(" ")
    		.a(String.format("%-8s", prefix))
	        .a(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
	        .a(String.format(" %d%% [", percent))
	        .fgGreen()
	        .a(String.join("", Collections.nCopies(percent/10, "=")))
	        .fgDefault()
	        .a(String.join("", Collections.nCopies(10 - percent/10, "-")))
	        .a("] ")
	        .a(progressMsg);
        	//.a(String.format(" %d/%d, ETA: %s", current, total, etaHms));

	    logger.print(string);
	}
	
	private static void parseArg(String arg, HashMap<String,String[]> map)
	{
		int equalSign = arg.indexOf('=');
		if(arg.startsWith("-") && equalSign > 0)
		{
			String name = arg.substring(1, equalSign);
			String[] values = arg.substring(equalSign + 1).split(",");
			map.put(name, values);
		}
		else
		{
			int i = 0;
			if(arg.startsWith("-")) i = 1;
			String name = arg.substring(i);
			String[] values = new String[] {};
			map.put(name, values);
		}
	}
	
	private static void setParameter(String name, int value)
	{
		switch(name) {
			case "":
				//TODO
		}
	}
	
	private static void setParameter(String name, String value)
	{
		switch(name) {
			case "":
				//TODO
		}
	}
	
	private static void setParameter(String name, String[] value)
	{
		switch(name) {
			case "":
				//TODO
		}
	}
	
	private static void setParameter(String name, boolean value)
	{
		switch(name) {
			case "decompile":
				mode = EnumMode.decompile;
				break;
			case "recompile":
				mode = EnumMode.recompile;
				break;
			case "reobfuscate":
				mode = EnumMode.reobfuscate;
				break;
			case "updatemd5":
				mode = EnumMode.updatemd5;
				break;
			case "updatemcp":
				mode = EnumMode.updatemcp;
				break;
			case "setup":
				mode = EnumMode.setup;
				break;
			case "nopatch":
				Decompile.noPatch = value;
				break;
		}
	}
}