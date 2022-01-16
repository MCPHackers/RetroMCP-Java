package jredfox.terminal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jredfox.common.exe.ExeBuilder;
import jredfox.common.io.IOUtils;
import jredfox.common.os.OSUtil;
import jredfox.common.utils.FileUtil;
import jredfox.common.utils.JREUtil;
import jredfox.common.utils.JavaUtil;
import jredfox.terminal.app.ITerminalApp;
import jredfox.terminal.app.TerminalApp;

public class OpenTerminal {
	
	public static OpenTerminal INSTANCE = new OpenTerminal();
	public TerminalApp app;
	/**
	 * change this to false if you run multiple OpenTerminal instances per init launch
	 */
	public boolean exitOnAppExit = true;
	
	static
	{
		String stage = System.getProperty(OpenTerminalConstants.launchStage);
		if(stage == null)
			System.setProperty(OpenTerminalConstants.launchStage, OpenTerminalConstants.init);
	}
	
	public OpenTerminal()
	{
		
	}
	
	/**
	 * recommended way of running OpenTerminal. Otherwise you need to manual check on whether or not to parse the configs for the TerminalApp properties
	 */
	public TerminalApp run(Class<? extends ITerminalApp> clazz, String[] args)
	{
		TerminalApp app = OpenTerminal.isLaunching() ? ((ITerminalApp)JREUtil.newInstance(clazz)).newApp(args) : TerminalApp.fromProperties(args);
		this.run(app);
		return this.app;
	}
	
	/**
	 * allow custom TerminalApp's to run even if it's not instaceof ITerminalApp
	 */
	public void run(TerminalApp app)
	{
		this.app = app;
		this.start();
	}

	/**
	 * start the launcher loop
	 */
	protected void start()
	{
		if(this.app == null)
			throw new IllegalArgumentException("TerminalApp cannot be null!");
		
		String stage = System.getProperty(OpenTerminalConstants.launchStage);
		if(stage.equals(OpenTerminalConstants.exe))
		{
			this.app.clearProperties();
			return;
		}
		else if(stage.equals(OpenTerminalConstants.wrapping))
		{
			OpenTerminalWrapper.run(this.app);
			return;//return as the TerminalApp is done executing and System#exit has already been called on the child process
		}
		
		this.app.process = this.launch();
		if(this.app.process != null)
			JREUtil.sleep(700);
		int exit = this.app.process != null ? 0 : -1;
		while(this.app.process != null)
		{
			if(!this.app.process.isAlive())
			{
				File reboot = new File(this.app.getAppdata(), "reboot.properties");
				if(this.app.canReboot() && reboot.exists())
				{
					this.relaunch(reboot);
					JREUtil.sleep(700);
					continue;
				}
				exit = this.app.process.exitValue();
				this.app.process = null;
			}
			else
				JREUtil.sleep(1000);
		}
		this.cleanup();
		if(this.exitOnAppExit)
			this.exit(exit);
	}
	
	public void exit(int exit)
	{
		System.out.println("shutting down OpenTerminal Launcher:" + exit);
		JREUtil.shutdown(exit);
	}

	public void cleanup() 
	{
		if(this.app != null)
			IOUtils.deleteDirectory(this.app.getAppdata());
	}

	/**
	 * give the OpenTerminal launcher a chance to deny app privileges
	 */
	public boolean shouldOpen()
	{
		return this.app.shouldOpen();
	}
	
	/**
	 * make sure that all System properties are set before calling this. In memory properties override other properties
	 */
	public Process launch()
	{
        String libs = System.getProperty("java.class.path");
        if(JavaUtil.containsAny(libs, OpenTerminalConstants.INVALID))
        	throw new RuntimeException("one or more LIBRARIES contains illegal parsing characters:(" + libs + "), invalid:" + OpenTerminalConstants.INVALID);
        
        ExeBuilder builder = new ExeBuilder();
    	builder.addCommand("java");
    	List<String> jvm = this.app.hardPause ? new ArrayList<>() : JavaUtil.asArray(this.app.jvmArgs);
    	this.app.writeProperties(jvm);
    	builder.addCommand(OpenTerminalUtil.writeProperty(jvm, OpenTerminalConstants.launchStage, OpenTerminalConstants.wrapping));//always use wrapper due to character limit on the command line
    	builder.addCommand(jvm);
    	builder.addCommand("-cp");
    	String q = OSUtil.getQuote();
    	builder.addCommand(q + libs + q);
    	builder.addCommand(this.app.hardPause ? OpenTerminalWrapper.class.getName() : this.app.mainClass.getName());
    	builder.addCommand(OpenTerminalUtil.wrapProgramArgs(this.app.programArgs));
    	String command = builder.toString();
    	try
    	{
    		return this.shouldOpen() ? OpenTerminalUtil.runInNewTerminal(this.app.getAppdata(), this.app.terminal, this.app.name, this.app.shName, command, this.app.userDir) : OpenTerminalUtil.runInTerminal(this.app.terminal, command, this.app.userDir);
    	}
    	catch(Throwable t)
    	{
    		t.printStackTrace();
    	}
    	return null;
	}

	/**
	 * use {@link TerminalApp#reboot()} for users. This is to simply re-launch your TerminalApp after the reboot file has started from the Parent(Launcher) process not your TerminalApp(child) process
	 */
	public void relaunch(File reboot) 
	{
		System.out.println("re-launching: " + FileUtil.getRealtivePath(OpenTerminalConstants.data, reboot) + " exists:" + reboot.exists());
		this.app = TerminalApp.fromFile(reboot);
//		reboot.delete();
		this.app.process = this.launch();
	}
	
	/**
	 * use this boolean to load configs related to the TerminalApp's variables
	 */
	public static boolean isLaunching()
	{
		String s = System.getProperty(OpenTerminalConstants.launchStage);
		return s.equals(OpenTerminalConstants.init) || s.equals(OpenTerminalConstants.reboot);
	}
	
	/**
	 * is your TerminalApp already a child process? Is your current code executing from not the Launcher but, a child process?
	 */
	public static boolean isChild()
	{
		return !isLaunching();
	}
	
	/**
	 * can your main(String[] args) execute yet? First launch fires launcher, Second launch fires virtual wrapper, Third launch your program now executes as normal
	 */
	public static boolean canExe()
	{
		return System.getProperty(OpenTerminalConstants.launchStage).equals(OpenTerminalConstants.exe);
	}

	/**
	 * Mostly internal use {@link OpenTerminal#isLaunching()} instead. Currently used for populating jvm arguments
	 */
	public static boolean isInit()
	{
		return System.getProperty(OpenTerminalConstants.launchStage).equals(OpenTerminalConstants.init);
	}
	
	public static boolean isReboot()
	{
		return System.getProperty(OpenTerminalConstants.launchStage).equals(OpenTerminalConstants.reboot);
	}

}