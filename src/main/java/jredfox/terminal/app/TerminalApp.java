package jredfox.terminal.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jredfox.common.config.MapConfig;
import jredfox.common.os.OSUtil;
import jredfox.common.utils.JREUtil;
import jredfox.common.utils.JavaUtil;
import jredfox.terminal.OpenTerminal;
import jredfox.terminal.OpenTerminalConstants;
import jredfox.terminal.OpenTerminalUtil;

public class TerminalApp {
	
	public String terminal;
	public String idHash;
	public boolean background;
	public boolean shouldPause;
	public boolean hardPause;
	public String id;
	public String shName;
	public String name;
	public String version;
	/**
	 * the main class of your program that is never a wrapper class
	 */
	public Class<?> iclass;
	/**
	 * the main class the jvm. may be equal to your program's main class or a wrapper class
	 */
	public Class<?> mainClass;
	public boolean runDeob;
	public boolean forceTerminal;//set this to true to always open up a new window
	public boolean canReboot = true;
	public List<String> jvmArgs;
	public List<String> programArgs;
	public File userDir;
	public File userHome;
	public File tmp;
	public File appdata;
	
	//non serializable vars
	public boolean compiled = JREUtil.isCompiled();
	public Process process;
	public boolean isRebooting;
	
	public TerminalApp(Class<?> iclass, String[] args)
	{
		this(iclass, suggestAppId(iclass), args);
	}
	
	public TerminalApp(Class<?> iclass, String id, String[] args)
	{
		this(iclass, id, id, "1.0.0", args);
	}
	
	public TerminalApp(Class<?> iclass, String id, String name, String version, String[] args)
	{
		this(iclass, id, name, version, JREUtil.getMainClass(), args);
	}
	
	public TerminalApp(Class<?> iclass, String id, String name, String version, Class<?> jvmMain, String[] args)
	{
		this(iclass, id, name, version, jvmMain, args, true);
	}
	
	public TerminalApp(Class<?> iclass, String id, String name, String version, Class<?> jvmMain, String[] args, boolean runDeob)
	{
    	if(JavaUtil.containsAny(id, OpenTerminalConstants.INVALID))
    		throw new RuntimeException("appId contains illegal parsing characters:(" + id + "), invalid:" + OpenTerminalConstants.INVALID);
    	
		this.jvmArgs = OpenTerminal.isInit() ? JavaUtil.asArray(JREUtil.getJVMArgs()) : new ArrayList<>();
		JavaUtil.removeStarts(this.jvmArgs, "-D" + OpenTerminalConstants.jvm, false);
		TerminalApp.addArgs(this.jvmArgs, this.getProperty(OpenTerminalConstants.jvm, ""));
		
		this.programArgs = new ArrayList<>(args.length);
		for(String s : args)
			this.programArgs.add(s);
		
		this.id = this.getProperty("ot.id", id);
		this.shName = this.getProperty("ot.shName", this.id.contains("/") ? JavaUtil.getLastSplit(this.id, "/") : this.id);
		this.name = this.getProperty("ot.name", name);
		this.version = this.getProperty("ot.version", version);
		this.iclass = JREUtil.getClass(this.getProperty("ot.iclass", iclass.getName()), true);
		this.mainClass = JREUtil.getClass(this.getProperty("ot.mainClass", jvmMain.getName()), true);
		this.runDeob = this.getProperty("ot.runDeob", runDeob);
		this.forceTerminal = this.getProperty("ot.forceTerminal", false);
		
		boolean isLaunching = OpenTerminal.isLaunching();
		if(isLaunching)
			this.syncConfig();
		this.terminal = this.getProperty("ot.terminal", this.terminal);
		this.idHash = isLaunching ? this.getProperty("ot.hash", "" + System.currentTimeMillis()) : this.getProperty("ot.hash");
		this.background = this.getProperty("ot.background", false);
		this.shouldPause = this.getProperty("ot.shoulPause", false);
		this.hardPause = this.getProperty("ot.hardPause", false);
		this.userDir = new File(this.getProperty(OpenTerminalConstants.p_userDir));
		this.userHome = new File(this.getProperty(OpenTerminalConstants.p_userHome));
		this.tmp = new File(this.getProperty(OpenTerminalConstants.p_tmp));
		this.appdata = new File(this.getProperty(OpenTerminalConstants.p_appdata));
	}
	
	/**
	 * sync your global TerminalApp properties
	 */
	public void syncConfig()
	{
		File cfgFile = new File(this.getRootAppData(), this.shName + ".cfg");
		MapConfig cfg = new MapConfig(cfgFile);
		cfg.load();
		this.terminal = cfg.get("terminal", "");
		if(this.terminal.isEmpty() || !OSUtil.isTerminalValid(this.terminal))
		{
			this.terminal = OSUtil.getTerminal();
			cfg.set("terminal", this.terminal);
			cfg.save();
		}
	}

	/**
	 * used when parsing from a file usually a reboot
	 */
	public TerminalApp(MapConfig cfg)
	{
		this.terminal = cfg.get("ot.terminal", null);
		this.idHash = cfg.get("ot.hash", null);
		this.background = Boolean.parseBoolean(cfg.get("ot.background", null));
		this.shouldPause = Boolean.parseBoolean(cfg.get("ot.shouldPause", null));
		this.hardPause = Boolean.parseBoolean(cfg.get("ot.hardPause", null));
		this.id = cfg.get("ot.id", null);
		this.shName = cfg.get("ot.shName", null);
		this.name = cfg.get("ot.name", null);
		this.version = cfg.get("ot.version", null);
		this.iclass = JREUtil.getClass(cfg.get("ot.iclass", null), true);
		this.mainClass = JREUtil.getClass(cfg.get("ot.mainClass", null), true);
		this.runDeob = Boolean.parseBoolean(cfg.get("ot.runDeob", null));
		this.forceTerminal = Boolean.parseBoolean(cfg.get("ot.forceTerminal", null));
		this.canReboot = Boolean.parseBoolean(cfg.get("ot.canReboot", null));
		this.userDir = new File((String)cfg.get(OpenTerminalConstants.p_userDir, null));
		this.userHome = new File((String)cfg.get(OpenTerminalConstants.p_userHome, null));
		this.tmp = new File((String)cfg.get(OpenTerminalConstants.p_tmp, null));
		this.appdata = new File((String)cfg.get(OpenTerminalConstants.p_appdata, null));
		String jvm = cfg.get(OpenTerminalConstants.jvm, null);
		String args = cfg.get(OpenTerminalConstants.args, null);
		this.jvmArgs = new ArrayList<>();
		this.programArgs = new ArrayList<>();
		addArgs(this.jvmArgs, jvm);
		addArgs(this.programArgs, args);
	}

	/**
	 * should your TerminalApp open the terminal gui?
	 */
	public boolean shouldOpen()
    {
        return !this.background && (!this.compiled ? this.runDeob && System.console() == null && !JREUtil.isDebugMode() : this.forceTerminal || System.console() == null);
    }
	
	public boolean shouldPause(int exit)
	{
		return exit != OpenTerminalConstants.rebootExit && exit != OpenTerminalConstants.forceExit && this.shouldPause();
	}
    
	public boolean shouldPause() 
	{
		return this.shouldPause || this.hardPause;
	}
	
	/**
	 * shouldPause will NOT catch {@link System#exit(int)}
	 */
	public TerminalApp enablePause()
	{
		this.shouldPause = true;
		return this;
	}
	
	/**
	 * hard pause will catch {@link System#exit(int)}
	 */
	public TerminalApp enableHardPause()
	{
		this.hardPause = true;
		return this;
	}
	
	/**
	 * disables both shouldPause and hardPause
	 */
	public void disablePause()
	{
		this.shouldPause = false;
		this.hardPause = false;
	}
	
	public TerminalApp setRebootable(boolean b)
	{
		this.canReboot = b;
		return this;
	}
	
	public boolean canReboot()
	{
		return this.canReboot;
	}
	
	/**
	 * pause at the end of the program
	 */
	public void pause()
	{
		System.out.println("Press ENTER to continue:");
		OpenTerminalConstants.scanner.nextLine();
	}
	
	/**
	 * used for serialization of the app
	 */
	public Map<String, String> toPropertyMap()
	{
		Map<String, String> props = new HashMap<>(25);
		props.put("ot.appClass", this.getClass().getName());
		props.put("ot.terminal", this.terminal);
		props.put("ot.hash", "" + this.idHash);
		props.put("ot.background", "" + this.background);
		props.put("ot.shoulPause", "" + this.shouldPause);
		props.put("ot.hardPause", "" + this.hardPause);
		props.put("ot.id", this.id);
		props.put("ot.shName", this.shName);
		props.put("ot.name", this.name);
		props.put("ot.version", this.version);
		props.put("ot.iclass", this.iclass.getName());
		props.put("ot.mainClass", this.mainClass.getName());
		props.put("ot.runDeob", "" + this.runDeob);
		props.put("ot.forceTerminal", "" + this.forceTerminal);
		props.put("ot.canReboot", "" + this.canReboot);
		props.put(OpenTerminalConstants.jvm, OpenTerminalUtil.wrapArgsToCmd(this.jvmArgs));
		props.put(OpenTerminalConstants.p_userDir, this.userDir.getPath());
		props.put(OpenTerminalConstants.p_userHome, this.userHome.getPath());
		props.put(OpenTerminalConstants.p_tmp, this.tmp.getPath());
		props.put(OpenTerminalConstants.p_appdata, this.appdata.getPath());
		return props;
	}
	
	/**
	 * write your app vars to jvm properties for wrapping/execution
	 */
	public void writeProperties(List<String> jvm)
	{
		for(Map.Entry<String, String> entry : this.toPropertyMap().entrySet())
		{
			String id = entry.getKey();
			String v = entry.getValue();
			jvm.add(OpenTerminalUtil.writeProperty(jvm, id, v));
		}
	}
	
	/**
	 * reboot your TerminalApp using {@link #programArgs} and {@link #jvmArgs}. in order to have a clean reboot clear them before calling this. 
	 * WARNING: your {@link TerminalApp#iclass} must be instanceof ITerminalApp in order to reboot
	 */
	public void reboot()
	{
		this.reboot(true);
	}
	
	/**
	 * reboot your TerminalApp using {@link #programArgs} and {@link #jvmArgs}. in order to have a clean reboot clear them before calling this. 
	 * WARNING: your {@link TerminalApp#iclass} must be instanceof ITerminalApp in order to reboot if newApp is true
	 */
	public void reboot(boolean newApp)
	{
		this.clearProperties();
		System.setProperty(OpenTerminalConstants.launchStage, OpenTerminalConstants.reboot);
		System.setProperty(OpenTerminalConstants.jvm, OpenTerminalUtil.wrapArgsToCmd(this.jvmArgs));
		TerminalApp app = newApp ? ((ITerminalApp)JREUtil.newInstance(this.iclass)).newApp(this.getProgramArgs()) : this;
		app.idHash = this.idHash;
		app.save();
		JREUtil.shutdown(OpenTerminalConstants.rebootExit);
	}
	
	/**
	 * clears the extra generated open terminal properties that were not in the jvm to begin with from the initial command
	 */
	public void clearProperties()
	{
		//clear properties for new Terminal app
		for(String s : this.toPropertyMap().keySet())
			if(!this.isReservedProperty(s))
				JREUtil.clearProperty(s);
		
		//set properties back into jvm
		for(String s : this.jvmArgs)
		{
			if(s.startsWith("-D"))
			{
				String[] pair = JavaUtil.splitFirst(s, '=', '"', '"');
				System.setProperty(pair[0], pair.length > 1 ? pair[1] : "");
			}
		}
	}

	public boolean isReservedProperty(String s)
	{
		return s.startsWith("user.") || s.equals(OpenTerminalConstants.p_tmp);
	}

	/**
	 * save this app to a disk
	 */
	public void save() 
	{
		List<String> jvm = JavaUtil.asArray(this.jvmArgs);
		this.writeProperties(jvm);//overwrite jvm properties with app properties
		File reboot = new File(this.getAppdata(), "reboot.properties");
		MapConfig cfg = new MapConfig(reboot);
		for(Map.Entry<String, String> entry : this.toPropertyMap().entrySet())
			cfg.list.put(entry.getKey(), entry.getValue());
		cfg.list.put(OpenTerminalConstants.args, OpenTerminalUtil.wrapArgsToCmd(this.programArgs).replaceAll(System.lineSeparator(), OpenTerminalConstants.linefeed));//stop illegal line feed characters from messing up parsing
		cfg.save();
	}
	
	/**
	 * sync user dirs from the properties. call this if you set system properties and want to reboot or use the TerminalApp directory vars again
	 */
	public void syncDirs()
	{
		this.userDir = new File(this.getProperty(OpenTerminalConstants.p_userDir));
		this.userHome = new File(this.getProperty(OpenTerminalConstants.p_userHome));
		this.tmp = new File(this.getProperty(OpenTerminalConstants.p_tmp));
		this.appdata = new File(this.getProperty(OpenTerminalConstants.p_appdata));
	}
	
	public boolean getProperty(String propId, boolean b)
	{
		return Boolean.parseBoolean(this.getProperty(propId, String.valueOf(b)));
	}

	public String getProperty(String propId, String defaults) 
	{
		String p = System.getProperty(propId);
		return p != null ? p : defaults;
	}

	public boolean getBooleanProperty(String propId) 
	{
		return Boolean.parseBoolean(this.getProperty(propId));
	}

	public String getProperty(String propId)
	{
		return System.getProperty(propId);
	}
	
	/**
	 * returns the appdata current instance
	 */
	public File getAppdata()
	{
		return new File(OpenTerminalConstants.data, this.id + "/instances/" + this.idHash);
	}
	
	/**
	 * returns the global appdata folder for this application
	 */
	public File getRootAppData()
	{
		return new File(OpenTerminalConstants.data, this.id);
	}
	
	/**
	 * return the suggested appId based on the main class name
	 */
	public static String suggestAppId(Class<?> clazz)
	{
		return suggestAppId(clazz.getName());
	}
	
	/**
	 * return the suggested appId based on the main class name
	 */
	public static String suggestAppId(String name)
	{
		return name.replaceAll("\\.", "/");
	}

	public String[] getProgramArgs() 
	{
		return JavaUtil.toArray(this.programArgs, String.class);
	}

	/**
	 * used to parse any TerminalApp from the disk into memory not just a specific class
	 */
	public static TerminalApp fromFile(File reboot)
	{
		MapConfig cfg = new MapConfig(reboot);
		cfg.load();
		TerminalApp app = JREUtil.newInstance(JREUtil.getClass(cfg.get("ot.appClass", null), true), new Class<?>[]{MapConfig.class}, cfg);
		return app;
	}
	
	public static TerminalApp fromProperties(String[] args) 
	{
		return JREUtil.newInstance(JREUtil.getClass(System.getProperty("ot.appClass"), true), new Class<?>[]{Class.class, String[].class}, JREUtil.getClass(System.getProperty("ot.iclass"), true), (Object)args);
	}

	public static List<String> addArgs(List<String> args, String argStr) 
	{
		if(argStr.isEmpty())
			return args;
		String[] arr = JavaUtil.parseCommandLine(argStr);
		for(String s : arr)
		{
			JavaUtil.removeStarts(args, JavaUtil.splitFirst(s, '=', '"', '"')[0], false);
			args.add(s.replaceAll(OpenTerminalConstants.spacefeed, " "));
		}
		return args;
	}


}
