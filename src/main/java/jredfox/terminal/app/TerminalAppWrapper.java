package jredfox.terminal.app;

import java.util.Map;

import jredfox.common.config.MapConfig;
import jredfox.common.utils.JREUtil;
import jredfox.common.utils.JavaUtil;
import jredfox.terminal.OpenTerminalConstants;

/**
 * TerminalApp but, with wrapped arguments for command line strict jars which don't handle 0 arguments :)
 */
public class TerminalAppWrapper extends TerminalApp {

	public String wrappedMsg = "";
	
	public TerminalAppWrapper(Class<?> iclass, String[] args)
	{
		this("", iclass, args);
	}
	
	public TerminalAppWrapper(String msg, Class<?> iclass, String[] args)
	{
		this(msg, iclass, suggestAppId(iclass), args);
	}
	
	public TerminalAppWrapper(String msg, Class<?> iclass, String id, String[] args)
	{
		this(msg, iclass, id, id, "1.0.0", args);
	}
	
	public TerminalAppWrapper(String msg, Class<?> iclass, String id, String name, String version, String[] args)
	{
		this(msg, iclass, id, name, version, JREUtil.getMainClass(), args);
	}
	
	public TerminalAppWrapper(String msg, Class<?> iclass, String id, String name, String version, Class<?> jvmMain, String[] args)
	{
		this(msg, iclass, id, name, version, jvmMain, args, true);
	}
	
	public TerminalAppWrapper(String msg, Class<?> iclass, String id, String name, String version, Class<?> jvmMain, String[] args, boolean runDeob)
	{
		super(iclass, id, name, version, jvmMain, args, runDeob);
		this.wrappedMsg = this.getProperty("ot.wrappedMsg", msg);
	}
	
	/**
	 * called when launching from a reboot file
	 */
	public TerminalAppWrapper(MapConfig cfg)
	{
		super(cfg);
		this.wrappedMsg = cfg.get("ot.wrappedMsg", null);
	}
	
	@Override
	public Map<String, String> toPropertyMap()
	{
		Map<String, String> props = super.toPropertyMap();
		props.put("ot.wrappedMsg", this.wrappedMsg);
		return props;
	}
	
	/**
	 * is your TerminalApp going to be grabbing arguments from the user before executing the jar?
	 */
	public boolean shouldWrapArgs()
	{
		return this.programArgs.size() == 0;
	}
	
	public String wrappedMsg()
	{
		return this.wrappedMsg;
	}

	public String[] getWrappedArgs(String[] args) 
	{
		return this.shouldWrapArgs() ? this.getInputArgs() : args;
	}

	/**
	 * get input args from the user
	 */
	public String[] getInputArgs()
	{
		System.out.print(this.wrappedMsg());
		return JavaUtil.parseCommandLine(OpenTerminalConstants.scanner.nextLine());
	}

}
