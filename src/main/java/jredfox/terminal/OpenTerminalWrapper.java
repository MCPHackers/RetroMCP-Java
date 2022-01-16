package jredfox.terminal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import jredfox.common.exe.ExeBuilder;
import jredfox.common.utils.JREUtil;
import jredfox.terminal.app.TerminalApp;
import jredfox.terminal.app.TerminalAppWrapper;

public class OpenTerminalWrapper {
	
	/**
	 * virtual wrapper doesn't catch {@link System#exit(int)}
	 */
	public static void run(TerminalApp app)
	{
		System.setProperty(OpenTerminalConstants.launchStage, OpenTerminalConstants.exe);//set the state from wrapping to execute
		boolean err = false;
		try
		{
			String[] args = app instanceof TerminalAppWrapper ? (((TerminalAppWrapper)app).getWrappedArgs(app.getProgramArgs())) : app.getProgramArgs();
			Method method = app.mainClass.getMethod("main", String[].class);
			method.invoke(null, new Object[]{args});
		}
		catch(InvocationTargetException e)
		{
			err = true;
			if(e.getCause() != null)
				e.getCause().printStackTrace();
			else
				e.printStackTrace();
		}
		catch(Throwable t)
		{
			err = true;
			t.printStackTrace();
		}
		
		int exit = err ? -1 : 0;
		if(app.shouldPause(exit))
			app.pause();
		System.exit(exit);
	}
	
	/**
	 * hard wrapper catches {@link System#exit(int)}
	 */
	public static void main(String[] args)
	{
		try
		{
			TerminalApp app = TerminalApp.fromProperties(args);
			args = app instanceof TerminalAppWrapper ? (((TerminalAppWrapper)app).getWrappedArgs(args)) : app.getProgramArgs();
			ExeBuilder b = new ExeBuilder();
			b.addCommand("java");
			List<String> jvm = app.jvmArgs;
			app.writeProperties(jvm);
			b.addCommand(OpenTerminalUtil.writeProperty(jvm, OpenTerminalConstants.launchStage, OpenTerminalConstants.exe));
			b.addCommand(jvm);
			b.addCommand("-cp");
			b.addCommand("\"" + System.getProperty("java.class.path") + "\"");
			b.addCommand(app.mainClass.getName());
			b.addCommand(OpenTerminalUtil.wrapProgramArgs(args));
			Process p = OpenTerminalUtil.runInTerminal(app.terminal, b.toString(), app.userDir);
			JREUtil.sleep(100);
			while(p.isAlive())
			{
			
			}
		
			if(app.shouldPause(p.exitValue()))
				app.pause();
		}
		catch(Throwable t)
		{
			System.err.println("unhandled / unexpected exception:");
			t.printStackTrace();
		}
	}

}
