package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskRun extends Task {
    public TaskRun(int side, TaskInfo info) {
        super(side, info);
    }

	@Override
	public void doTask() throws Exception {
		if(side == SERVER && !VersionsParser.hasServer()) {
			throw new Exception("Server isn't available for this version!");
		}
		String java = Util.getJava();
		String natives = FileUtil.absolutePathString(MCPConfig.NATIVES);
		List<String> cpList = new LinkedList<String>();
		if(side == SERVER) {
			if(MCP.config.runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPConfig.BUILD_JAR_SERVER));
			}
			else {
				cpList.add(FileUtil.absolutePathString(MCPConfig.SERVER_BIN));
				cpList.add(FileUtil.absolutePathString(MCPConfig.SERVER));
			}
		}
		else if (side == CLIENT) {
			if(MCP.config.runBuild) {
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

		List<String> args = new ArrayList<String>(
			Arrays.asList(new String[] {
					java,
					"-Xms1024M",
					"-Xmx1024M",
					"-Djava.util.Arrays.useLegacyMergeSort=true",
					"-Dhttp.proxyHost=betacraft.uk",
					"-Dhttp.proxyPort=" + VersionsParser.getProxyPort(),
					"-Dorg.lwjgl.librarypath=" + natives,
					"-Dnet.java.games.input.librarypath=" + natives,
					"-cp", cp,
					side == SERVER ? (VersionsParser.getServerVersion().startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer") : "Start"
				}));
		for(int i = 1; i < MCP.config.runArgs.length; i++) {
			String arg = MCP.config.runArgs[i];
			if(args.contains(arg)) {
				args.remove(arg);
			}
			args.add(1, arg);
		}
		int exit = Util.runCommand(args.toArray(new String[0]), Paths.get(MCPConfig.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
