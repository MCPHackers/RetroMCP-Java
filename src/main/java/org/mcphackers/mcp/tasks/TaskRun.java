package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Util;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TaskRun extends Task {
    public TaskRun(int side, TaskInfo info) {
        super(side, info);
    }

	@Override
	public void doTask() throws Exception {
		String java = "\"" + System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java" + "\"";
		String natives = Util.getPath(MCPConfig.NATIVES).toAbsolutePath().toString();
		List<String> cpList = new ArrayList<String>();
		if(side == 1) {
			if(MCPConfig.runBuild) {
				cpList.add(MCPConfig.BUILD_JAR_SERVER);
			}
			else {
				cpList.add(MCPConfig.SERVER_BIN);
				cpList.add(MCPConfig.SERVER);
			}
		}
		else {
			if(MCPConfig.runBuild) {
				cpList.add(MCPConfig.BUILD_JAR_CLIENT);
			}
			else {
				cpList.add(MCPConfig.CLIENT_BIN);
				cpList.add(MCPConfig.CLIENT);
			}
			cpList.add(MCPConfig.LWJGL);
			cpList.add(MCPConfig.LWJGL_UTIL);
			cpList.add(MCPConfig.JINPUT);
		}
		
		
		String cp = String.join(";", cpList);
		int exit = Util.runCommand(
			String.join(" ",
				java,
				"-Xms1024M",
				"-Xmx1024M",
				"-Djava.util.Arrays.useLegacyMergeSort=true",
				"-Dhttp.proxyHost=betacraft.uk",
				"-Dhttp.proxyPort=11702",
				"-Dorg.lwjgl.librarypath=" + natives,
				"-Dnet.java.games.input.librarypath=" + natives,
				"-cp " + cp,
				side == 1 ? "net.minecraft.server.MinecraftServer" : "net.minecraft.client.Minecraft"
				), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}

//		System.setProperty("org.lwjgl.librarypath", Util.getPath(MCPConfig.NATIVES).toAbsolutePath().toString());
//		System.setProperty("net.java.games.input.librarypath", Util.getPath(MCPConfig.NATIVES).toAbsolutePath().toString());
//		System.setProperty("http.proxyHost", "betacraft.uk");
//		System.setProperty("http.proxyPort", "11702");
//
//		URL[] urls = getAllJarsInDirectory(side == 1 ? Paths.get("jars") : Paths.get("jars", "bin"));
//		URLClassLoader classLoader = new URLClassLoader(urls);
//		MethodHandle handle = MethodHandles.publicLookup().findStatic(classLoader.loadClass(side == 1 ? "net.minecraft.server.MinecraftServer" : "net.minecraft.client.Minecraft"), "main", MethodType.methodType(void.class, String[].class));
//		try {
//			handle.invoke(new String[] {"User"});
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
	}

	public static URL[] getAllJarsInDirectory(Path path) {
		List<URL> urlList = new ArrayList<>();
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().endsWith(".jar")) {
						urlList.add(file.toUri().toURL());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		URL[] urls = new URL[] {};
		return urlList.toArray(urls);
	}
}
