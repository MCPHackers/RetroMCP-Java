package jredfox.common.utils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import jredfox.common.os.OSUtil;
import jredfox.common.thread.ShutdownThread;

public class JREUtil {
	
	public static final String INVALID = "\"'`,";
	
	static
	{	
		System.setProperty("runnables.jar", getFileFromClass(getMainClass()).getPath());
		if(System.getProperty("user.appdata") == null)
		{
			System.setProperty("user.appdata", OSUtil.getAppData().getPath());
		}
		JREUtil.patchDir();//patch os's screwing up initial directory untested, patch macOs java launcher returning junk //TODO: test make sure it works
	}

	/**
	 * Must be called before OpenTerminal#run(TerminalApp app)
	 * NOTE: calling this forces the user directory regardless of os. this changes behavior on linux where user.dir = user.home
	 */
	public static void syncUserDirWithJar()
	{
		setUserDir(new File(System.getProperty("runnables.jar")).getParentFile());
	}
	
	/**
	 * patch macOs
	 */
	public static void patchDir()
	{
		String dir = System.getProperty("user.dir");
		String tmp = System.getProperty("java.io.tmpdir");
		if(dir.contains(tmp) && !dir.startsWith(tmp))
			setUserDir(OSUtil.isLinux() ? new File(System.getProperty("user.home")) : new File(System.getProperty("runnables.jar")).getParentFile());
	}

	/**
	 * must be called before {@link OpenTerminal#runWithCMD(String, String, Class, String[], boolean, boolean)}
	 * @param file
	 */
	public static void setUserDir(File file)
	{
		System.setProperty("user.dir", file.getPath());
	}

	/**
	 *  optimized method for checking if the main executing jar isCompiled
	 */
	public static boolean isCompiled()
	{
		return System.getProperty("runnables.jar").endsWith(".jar");
	}
	
	/**
	 * checks per class if the jar is compiled
	 */
	public static boolean isCompiled(Class<?> mainClass)
	{
		try 
		{
			return getFileFromClass(mainClass).getName().endsWith(".jar");
		}
		catch (RuntimeException e) 
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isDebugMode()
	{
		return java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
	}
	
	/**
	 * get a file from a class
	 * @throws URISyntaxException 
	 */
	public static File getFileFromClass(Class<?> clazz) throws RuntimeException
	{
		clazz = isEclipseJIJ() ? loadSyClass(clazz.getName(), false) : clazz;
		URL jarURL = clazz.getProtectionDomain().getCodeSource().getLocation();//get the path of the currently running jar
		File file = FileUtil.getFile(jarURL);
		String fileName = file.getPath();
		if(fileName.contains(INVALID))
			throw new RuntimeException("jar file contains invalid parsing chars:" + fileName);
		return file;
	}
	
	public static String getMainClassName()
	{
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		String actualMain = main.getClassName();
		return actualMain;
	}
	
	public static Class<?> getMainClass()
	{
		Class<?> mainClass = null;
		try 
		{
			String className = getMainClassName();
			mainClass = Class.forName(className);
		} 
		catch (ClassNotFoundException e1) 
		{
			e1.printStackTrace();
		}
		return mainClass;
	}
	
	
	private static Class<?> JIJ = getClass("org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader", false);
	public static boolean isEclipseJIJ()
	{
		return JIJ != null || getMainClassName().endsWith("jarinjarloader.JarRsrcLoader");
	}
	
	public static Class<?> loadSyClass(String name, boolean init)
	{
		try 
		{
			return Class.forName(name, init, ClassLoader.getSystemClassLoader());
		} 
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClass(String name, boolean print) 
	{
		try 
		{
			return (Class<T>) Class.forName(name);
		} 
		catch(ClassNotFoundException c)
		{
			if(print)
				c.printStackTrace();
		}
		catch (Throwable t) 
		{
			t.printStackTrace();
		}
		return null;
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
	 * shut down your java application
	 */
	public static void shutdown()
	{
		shutdown(0);
	}
	
	/**
	 * shut down your java application
	 */
	public static void shutdown(int code)
	{
		System.gc();
		System.exit(code);
	}

	public static File getProgramDir() 
	{
		return new File(System.getProperty("user.dir"));
	}
	
	public static List<String> getJVMArgs()
	{
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		return runtimeMxBean.getInputArguments();
	}
	
	/**
	 * cause a thread to sleep garenteed even with thread interuptions if boolean is true
	 */
	public static void sleep(long time)
	{
		sleep(time, true);
	}

	/**
	 * cause a thread to sleep to for time in ms. If noInterupt it won't allow interuptions to stop the sleep
	 */
	public static void sleep(long time, boolean noInterupt)
	{
		long startMs = System.currentTimeMillis();
		try 
		{
			Thread.sleep(time);
		}
		catch (InterruptedException | IllegalArgumentException e) 
		{
			if(noInterupt)
			{
				long current = System.currentTimeMillis();
				long passedMs = current - startMs;
				time = time - passedMs;
				long stopMs = System.currentTimeMillis() + time;
				System.err.println("causing manual sleep due to interuption for:" + time);
				while(System.currentTimeMillis() < stopMs)
				{
					;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<?> clazz) 
	{
		try
		{
			return (T) clazz.newInstance();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<?> clazz, Class<?>[] ids, Object... params)
	{
		try 
		{
			return (T) clazz.getConstructor(ids).newInstance(params);
		} 
		catch(InvocationTargetException e)
		{
			if(e.getCause() != null)
				e.getCause().printStackTrace();
			else
				e.printStackTrace();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}

	public static void clearProperty(String s) 
	{
		try
		{
			System.clearProperty(s);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


}
