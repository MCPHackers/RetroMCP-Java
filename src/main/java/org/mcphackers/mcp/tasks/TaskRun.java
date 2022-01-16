package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class TaskRun extends Task {
    public TaskRun(int side, TaskInfo info) {
        super(side, info);
    }

	@Override
	public void doTask() throws Exception {
		String java = "\"" + System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java" + "\"";
		String natives = Paths.get(MCPConfig.NATIVES).toAbsolutePath().toString();
		List<String> cpList = new LinkedList<String>();
		if(side == SERVER) {
			if(MCPConfig.runBuild) {
				cpList.add(MCPConfig.BUILD_JAR_SERVER);
			}
			else {
				cpList.add(MCPConfig.SERVER_BIN);
				cpList.add(MCPConfig.SERVER);
			}
		}
		else if (side == CLIENT) {
			if(MCPConfig.runBuild) {
				cpList.add(MCPConfig.BUILD_JAR_CLIENT);
			}
			else {
				cpList.add(MCPConfig.CLIENT_BIN);
				cpList.add(Files.exists(Paths.get(MCPConfig.CLIENT_FIXED)) ? MCPConfig.CLIENT_FIXED : MCPConfig.CLIENT);
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
				"-Dhttp.proxyPort=11702", // TODO Get proxy from properties
				"-Dorg.lwjgl.librarypath=" + natives,
				"-Dnet.java.games.input.librarypath=" + natives,
				"-cp " + cp,
				//TODO Get start class from properties
				side == SERVER ? "net.minecraft.server.MinecraftServer" : "net.minecraft.client.Minecraft"
				), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
