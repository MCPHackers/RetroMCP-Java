package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Utility;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
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
		/*int exit = Utility.runCommand(
				"java -Xms1024M -Xmx1024M -Djava.util.Arrays.useLegacyMergeSort=true -cp " +
				String.join(";", new String[] {(side == 1 ? MCPConfig.SERVER_BIN : MCPConfig.CLIENT_BIN), (side == 1 ? MCPConfig.SERVER : MCPConfig.CLIENT), MCPConfig.LWJGL, MCPConfig.LWJGL_UTIL, MCPConfig.JINPUT}) +
				" -Dhttp.proxyHost=betacraft.uk -Dhttp.proxyPort=11702 -Djava.library.path=" + MCPConfig.NATIVES + " net.minecraft.client.Minecraft");
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}*/

		URL[] urls = getAllJarsInDirectory(side == 1 ? Paths.get("jars") : Paths.get("jars", "bin"));
		URLClassLoader classLoader = new URLClassLoader(urls);
		MethodHandle handle = MethodHandles.publicLookup().findStatic(classLoader.loadClass(side == 1 ? "net.minecraft.server.MinecraftServer" : "net.minecraft.client.Minecraft"), "main", MethodType.methodType(void.class, String[].class));
		try {
			//System.setProperty("org.lwjgl.librarypath", Paths.get("jars", "bin", "natives").toAbsolutePath().toString());

			try {
				final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
				sysPathsField.setAccessible(true);
				sysPathsField.set(null, null);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			handle.invokeExact(new String[] {"fullscreen"});
		} catch (Throwable e) {
			e.printStackTrace();
		}
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
