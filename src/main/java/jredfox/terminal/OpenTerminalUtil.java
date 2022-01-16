package jredfox.terminal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jredfox.common.exe.ExeBuilder;
import jredfox.common.io.IOUtils;
import jredfox.common.os.OSUtil;
import jredfox.common.utils.JREUtil;
import jredfox.common.utils.JavaUtil;

public class OpenTerminalUtil {
	
	/**
	 * runs a command in a new terminal window.
	 * the sh name is the file name you want the shell script stored. The appId is to locate your folder
	 */
	public static Process runInNewTerminal(File appdata, String terminal, String appName, String shName, String command, File dir) throws IOException
	{
        if(OSUtil.isWindows())
        {
        	return runInTerminal(terminal, "start /wait " + "\"" + appName + "\" " + command, dir);
        }
        else if(OSUtil.isMac())
        {
        	genAS(terminal);
        	File sh = new File(appdata, shName + ".sh");
        	List<String> cmds = new ArrayList<>();
        	cmds.add("#!/bin/bash");
        	cmds.add("clear && printf '\\e[3J'");//clear the console
        	cmds.add("set +v");//@Echo off
        	cmds.add("echo -n -e \"\\033]0;" + appName + "\\007\"");//Title
        	cmds.add("cd " + dir.getPath().replaceAll(" ", "\\\\ "));//set the proper directory
        	cmds.add(command);//actual command
        	cmds.add("echo -n -e \"\\033]0;" + "_closeMe_" + "\\007\"");//set the title to prepare for the close command
        	cmds.add("osascript " + OpenTerminalConstants.closeMe.getPath().replaceAll(" ", "\\\\ ") + " & exit");
        	IOUtils.saveFileLines(cmds, sh, true);//save the file
        	IOUtils.makeExe(sh);//make it executable
        	return runInTerminal(terminal, "osascript " + OpenTerminalConstants.start.getPath().replaceAll(" ", "\\\\ ") + " \"" + sh.getPath().replaceAll(" ", "\\\\ ") + "\"", dir);
        }
        else if(OSUtil.isLinux())
        {
        	File sh = new File(terminal, shName + ".sh");
        	List<String> cmds = new ArrayList<>();
        	cmds.add("#!/bin/bash");
        	cmds.add("set +v");//@Echo off
        	cmds.add("echo -n -e \"\\033]0;" + appName + "\\007\"");//Title
        	cmds.add(command);//actual command
        	IOUtils.saveFileLines(cmds, sh, true);//save the file
        	IOUtils.makeExe(sh);//make it executable
        	return runInTerminal(OSUtil.getLinuxNewWin(), sh.getAbsolutePath().replaceAll(" ", "%20"), dir);
        }
		return null;
	}

	public static void genAS(String terminal) throws IOException
    {
    	//generate closeMe script
    	if(!OpenTerminalConstants.closeMe.exists())
    	{
    		 List<String> l = new ArrayList<>(1);
             l.add("tell application \"Terminal\" to close (every window whose name contains \"_closeMe_\")");
             compileAS(terminal, l, OpenTerminalConstants.closeMe, new File(OpenTerminalConstants.scripts, "closeMe.applescript"));
    	}
        
        //generate the start script
    	if(!OpenTerminalConstants.start.exists())
    	{
    		List<String> osa = new ArrayList<>(11);
    		osa.add("on run argv");
    		osa.add("	set input to first item of argv");
    		osa.add("	if application \"Terminal\" is running then");
    		osa.add("		tell application \"Terminal\"");
    		osa.add("			do script input");
    		osa.add("			activate");
    		osa.add("		end tell");
    		osa.add("	else");
    		osa.add("		tell application \"Terminal\"");
    		osa.add("			do script input in window 0");
    		osa.add("			activate");
    		osa.add("		end tell");
    		osa.add("	end if");
    		osa.add("end run");
    		compileAS(terminal, osa, OpenTerminalConstants.start, new File(OpenTerminalConstants.scripts, "start.applescript"));
    	}
    }
    
    /**
     * compile an applescript. can only run on macOs
     */
	public static void compileAS(String terminal, List<String> osa, File scpt, File applescript) throws IOException
	{
		IOUtils.saveFileLines(osa, applescript, true);
		Process p = runInTerminal(terminal, "osacompile -o \"" + scpt.getPath() + "\"" + " \"" + applescript.getPath() + "\"", JREUtil.getProgramDir());
   		IOUtils.makeExe(applescript);
		IOUtils.makeExe(scpt);
		while(p.isAlive())
		{
			;
		}
	}
	
    /**
     * enforces it to run in the command prompt terminal as sometimes it doesn't work without it
     */
    public static Process runInTerminal(String terminal, String command, File dir) throws IOException
    {
        return runInTerminal(terminal, OSUtil.getExeAndClose(), command, dir);
    }
    
    /**
     * enforces it to run in the command prompt terminal as sometimes it doesn't work without it
     */
    public static Process runInTerminal(String terminal, String flag, String command, File dir) throws IOException
    {
        return run(new String[]{terminal, flag, command}, dir);
    }
	
    public static Process run(String[] cmdarray, File dir) throws IOException
    {
        return new ProcessBuilder(cmdarray).inheritIO().directory(dir).start();
    }
    
	public static String[] wrapProgramArgs(List<String> arr) 
	{
		String[] args = JavaUtil.toArray(arr, String.class);
		String q = OSUtil.getQuote();
		String esc = OSUtil.getEsc();
		for(int i=0;i<args.length; i++)
			args[i] =  q + args[i].replaceAll(q, esc + q) + q;//wrap the jvm args to the native terminal quotes and escape quotes
		return args;
	}
	
	public static String[] wrapProgramArgs(String[] args) 
	{
		String q = OSUtil.getQuote();
		String esc = OSUtil.getEsc();
		for(int i=0;i<args.length; i++)
			args[i] =  q + args[i].replaceAll(q, esc + q) + q;//wrap the jvm args to the native terminal quotes and escape quotes
		return args;
	}
	
	public static String writeProperty(List<String> list, String propId, String value)
	{
		JavaUtil.removeStarts(list, "-D" + propId, false);
		return  "-D" + propId + "=\"" + value + "\"";
	}

	public static String wrapArgsToCmd(List<String> args) 
	{
		ExeBuilder b = new ExeBuilder();
		for(String s : args)
			b.addCommand(s.replaceAll(" ", OpenTerminalConstants.spacefeed));
		return b.toString();
	}

}
