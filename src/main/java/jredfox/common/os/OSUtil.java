package jredfox.common.os;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jredfox.common.file.FileUtils;
import jredfox.common.thread.ShutdownThread;
import jredfox.common.utils.FileUtil;
import jredfox.common.utils.JavaUtil;

public class OSUtil {
	
	private static String osName = System.getProperty("os.name").toLowerCase();
	private static boolean isWindows = osName.contains("windows");
	private static boolean isMac = osName.contains("mac");
	private static boolean isLinux = osName.contains("linux");
	
	public static String[] windows_terminals = new String[]
	{
		"cmd",
		"powershell",//seems to freak out and seems to be beta even in 2020 with all it's bugs
	};
	
	public static String[] mac_terminals = new String[]
	{
		"/bin/bash"	
	};
	
	public static String[] linux_terminals = new String[]
	{
			"/usr/bin/gcm-calibrate",
			"/usr/bin/gnome-terminal",
			"/usr/bin/mosh-client",
			"/usr/bin/mosh-server",
			"/usr/bin/mrxvt",           
			"/usr/bin/mrxvt-full",        
			"/usr/bin/roxterm",          
			"/usr/bin/rxvt-unicode",        
			"/usr/bin/urxvt",             
			"/usr/bin/urxvtd",
			"/usr/bin/vinagre",
			"/usr/bin/x-terminal-emulator",
			"/usr/bin/xfce4-terminal",   
			"/usr/bin/xterm",
			"/usr/bin/aterm",
			"/usr/bin/guake",
			"/usr/bin/Kuake",
			"/usr/bin/rxvt",
			"/usr/bin/rxvt-unicode",
			"/usr/bin/Terminator",
			"/usr/bin/Terminology",
			"/usr/bin/tilda",
			"/usr/bin/wterm",
			"/usr/bin/Yakuake",
			"/usr/bin/Eterm",
			"/usr/bin/gnome-terminal.wrapper",
			"/usr/bin/koi8rxterm",
			"/usr/bin/konsole",
			"/usr/bin/lxterm",
			"/usr/bin/mlterm",
			"/usr/bin/mrxvt-full",
			"/usr/bin/roxterm",
			"/usr/bin/rxvt-xpm",
			"/usr/bin/rxvt-xterm",
			"/usr/bin/urxvt",
			"/usr/bin/uxterm",
			"/usr/bin/xfce4-terminal.wrapper",
			"/usr/bin/xterm",
			"/usr/bin/xvt"
	};
	
	public static String getTerminal()
	{
		String[] cmds = getTerminals();
		for(String cmd : cmds)
		{
			try 
			{
				Runtime.getRuntime().exec(cmd + " " + getExeAndClose() + " cd " + System.getProperty("user.dir"));
				return cmd;
			}
			catch (Throwable e) {}
		}
		System.err.println("Unable to find Os terminal for:" + System.getProperty("os.name") + " report to https://github.com/jredfox/SelfCommandPrompt/issues");
		return null;
	}
	
	/**
	 * test if your terminal string is actually your terminal
	 */
	public static boolean isTerminalValid(String term) 
	{
		try 
		{
			Runtime.getRuntime().exec(term + " " + getExeAndClose() + " cd " + System.getProperty("user.dir"));
			return true;
		} 
		catch (Throwable e) {}
		return false;
	}

	public static String[] getTerminals()
	{
		return isWindows() ? windows_terminals : isMac() ? mac_terminals : isLinux() ? linux_terminals : null;
	}
	
	/**
	 * runs the command in the background by default and closes
	 */
	public static String getExeAndClose()
	{
		return isWindows() ? "/c" : (isMac() || isLinux()) ?  "-c" : null;
	}
	
	/**
	 * returns the linux execute in new window flag
	 */
	public static String getLinuxNewWin()
	{
		return "-x";
	}
	
	/**
	 * @return the terminal's quote
	 */
	public static String getQuote() 
	{
		return "\"";
	}

	/**
	 * @return the escape sequence to preserve characters
	 */
	public static String getEsc() 
	{
		return "\\\"";
	}
	
	public static boolean isWindows()
	{
		return isWindows;
	}
	
	public static boolean isMac()
	{
		return isMac;
	}
	
	public static boolean isLinux()
	{
		return isLinux;
	}
	
	public static boolean isUnsupported()
	{
		return !isWindows() && !isMac() && !isLinux();
	}
	
	public static File getAppData()
	{
		if(isWindows())
			return new File(System.getenv("APPDATA"));
		
	    String path = System.getProperty("user.home");
	    if(isMac())
	    	path += "/Library/Application Support";
	    return new File(path);
	}
	
	/**
	 * NOTE: this isn't a shutdown event to prevent shutdown only a hook into the shutdown events. 
	 * That would be app specific this is jvm program (non app) specific which works for both
	 */
	public static void addShutdownThread(ShutdownThread sht)
	{
		throw new RuntimeException("Unsupported Check back in a future version!");
	}
	
	/**
	 * doesn't call {@link OSUtil#toOSFile(File)} this is simply a method if you don't need the whole process. Re-written from Evil Notch Lib
	 */
	public static File toWinFile(File file)
	{
		if(possiblyReserved(file))
		{
			List<String> paths = new ArrayList<>(15);
			File fpath = file;
			while(fpath != null)
			{
				String fileName = FileUtils.getTrueName(fpath);
				String filtered = isReserved(fileName) ? ( (fileName.contains(".") ? JavaUtil.inject(fileName, '.', '_') : fileName + "_") + FileUtil.getExtensionFull(fpath)) : (fpath.getParent() != null ? fpath.getName() : fpath.getPath());
				paths.add(filtered);
				fpath = fpath.getParentFile();
			}
			StringBuilder builder = new StringBuilder();
			int size = paths.size();
			for(int i= size - 1; i >= 0; i--)
			{
				String s = paths.get(i);
				builder.append(s + (i == 0 || i == size - 1 ? "" : File.separator));
			}
			return new File(builder.toString());
		}
		return file;
	}

	/**
	 * converts the file to a cross platform file if needed. Re-written from Evil Notch Lib
	 */
	public static File toOSFile(File file)
	{
		String invalid = "*/<>?\":|'";//java replaces trailing "\" or "/" and you can't get a file name with "/\" in java so don't check it
		if(JavaUtil.containsAny(file.getPath(), invalid))
		{
			file = filter(file, invalid);
		}
		return toWinFile(file);
	}

	public static File filter(File file, String invalid) 
	{
		List<String> paths = new ArrayList<>(15);
		File fpath = file;
		while(fpath != null)
		{
			File newPath = fpath.getParentFile();
			String filtered = newPath == null ? fpath.getPath() : filter(fpath.getName(), invalid);
			if(!filtered.isEmpty())
				paths.add(filtered);
			fpath = newPath;
		}
		StringBuilder builder = new StringBuilder();
		int size = paths.size();
		for(int i=size - 1; i >= 0; i--)
		{
			String s = paths.get(i);
			builder.append(s + (i == 0 || i == size - 1 ? "" : File.separator));
		}
		return new File(builder.toString());
	}

	public static String filter(String name, String invalid) 
	{
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < name.length(); i++)
		{
			String c = name.substring(i, i + 1);
			if(!invalid.contains(c))
				b.append(c);
		}
		return b.toString();
	}

	public static final String[] winReserved = new String[] 
	{
		"CON", "PRN", "AUX", "NUL",
		"COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
		"LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
	};
	
	public static boolean isReserved(String name)
	{
		for(String r : winReserved)
		{
			if(name.equalsIgnoreCase(r) || name.toUpperCase().startsWith(r + "."))
				return true;
		}
		return false;
	}
	
	public static boolean possiblyReserved(File file) 
	{
		String path = file.getPath().toUpperCase();
		for(String s : winReserved)
		{
			if(path.contains(s))
				return true;
		}
		return false;
	}

}
