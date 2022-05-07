// Retrieve classes of a package and it's nested package from file based class repository

package org.mcphackers.mcp.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class ClassUtils {
	public static <T> List<Class<T>> getClasses(Path p, Class<T> type) throws Exception {
		String pathToJar = p.toAbsolutePath().toString();
		JarFile jarFile = new JarFile(pathToJar);
		Enumeration<JarEntry> e = jarFile.entries();

		List<Class<T>> classes = new ArrayList<>();
		URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
		URLClassLoader cl = URLClassLoader.newInstance(urls);

		while (e.hasMoreElements()) {
		    JarEntry je = e.nextElement();
		    if(je.isDirectory() || !je.getName().endsWith(".class")){
		        continue;
		    }
		    // -6 because of .class
		    String className = je.getName().substring(0,je.getName().length()-6);
		    className = className.replace('/', '.');
		    Class cls = cl.loadClass(className);
		    if(type.isAssignableFrom(cls)) {
		    	classes.add((Class<T>)cls);
		    }
		}
		jarFile.close();
		return classes;
	}

	/** 
	 * @see Modifier#isAbstract(int) does no guarantee that all methods were implemented in the compiled class
	 * And there is a chance it was compiled from a different source where one of the methods didn't exist
	 */
	public static boolean isClassAbstract(Class<?> type) {
		for(Method meth : type.getMethods()) {
			if(Modifier.isAbstract(meth.getModifiers())) {
				return true;
			}
		}
		return false;
	}
}