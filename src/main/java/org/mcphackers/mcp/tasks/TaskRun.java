package org.mcphackers.mcp.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;

public class TaskRun extends Task {
    public TaskRun(int side, TaskInfo info) {
        super(side, info);
    }

	@Override
	public void doTask() throws Exception {
		String java = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		String natives = FileUtil.absolutePathString(MCPConfig.NATIVES);
		List<String> cpList = new LinkedList<String>();
		if(side == SERVER) {
			if(MCPConfig.runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPConfig.BUILD_JAR_SERVER));
			}
			else {
				cpList.add(FileUtil.absolutePathString(MCPConfig.SERVER_BIN));
				cpList.add(FileUtil.absolutePathString(MCPConfig.SERVER));
			}
		}
		else if (side == CLIENT) {
			if(MCPConfig.runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPConfig.BUILD_JAR_CLIENT));
			}
			else {
				cpList.add(FileUtil.absolutePathString(MCPConfig.CLIENT_BIN));
				cpList.add(FileUtil.absolutePathString(Files.exists(Paths.get(MCPConfig.CLIENT_FIXED)) ? MCPConfig.CLIENT_FIXED : MCPConfig.CLIENT));
			}
			cpList.add(FileUtil.absolutePathString(MCPConfig.LWJGL));
			cpList.add(FileUtil.absolutePathString(MCPConfig.LWJGL_UTIL));
			cpList.add(FileUtil.absolutePathString(MCPConfig.JINPUT));
		}
		
		
		String cp = String.join(";", cpList);
		int exit = Util.runCommand(
			new String[] {
				java,
				"-Xms1024M",
				"-Xmx1024M",
				"-Djava.util.Arrays.useLegacyMergeSort=true",
				"-Dhttp.proxyHost=betacraft.uk",
				"-Dhttp.proxyPort=11702", // TODO Get proxy from properties
				"-Dorg.lwjgl.librarypath=" + natives,
				"-Dnet.java.games.input.librarypath=" + natives,
				"-cp", cp,
				//TODO Get start class from properties
				side == SERVER ? "net.minecraft.server.MinecraftServer" : "net.minecraft.client.Minecraft"
			}, Paths.get(MCPConfig.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
