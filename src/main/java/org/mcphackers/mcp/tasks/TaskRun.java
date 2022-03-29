package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		List<String> cpList = new LinkedList<>();
		if(side == SERVER) {
			if(MCP.config.runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPConfig.BUILD_ZIP_SERVER));
			}
			cpList.add(FileUtil.absolutePathString(MCPConfig.SERVER_BIN));
			cpList.add(FileUtil.absolutePathString(MCPConfig.SERVER));
			try(Stream<Path> stream = Files.list(Paths.get(MCPConfig.DEPS_S)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
		}
		else if (side == CLIENT) {
			if(MCP.config.runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPConfig.BUILD_ZIP_CLIENT));
			}
			cpList.add(FileUtil.absolutePathString(MCPConfig.CLIENT_BIN));
			if(!Files.exists(Paths.get(MCPConfig.CLIENT_FIXED))) {
				cpList.add(FileUtil.absolutePathString(MCPConfig.CLIENT));
			}
			try(Stream<Path> stream = Files.list(Paths.get(MCPConfig.LIB)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
			try(Stream<Path> stream = Files.list(Paths.get(MCPConfig.DEPS_C)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
		}
		
		
		String cp = String.join(System.getProperty("path.separator"), cpList);

		List<String> args = new ArrayList<>(
				Arrays.asList(java,
						"-Xms1024M",
						"-Xmx1024M",
						"-Djava.util.Arrays.useLegacyMergeSort=true",
						"-Dhttp.proxyHost=betacraft.uk",
						"-Dhttp.proxyPort=" + VersionsParser.getProxyPort(),
						"-Dorg.lwjgl.librarypath=" + natives,
						"-Dnet.java.games.input.librarypath=" + natives,
						"-cp", cp,
						side == SERVER ? (VersionsParser.getServerVersion().startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer")
																	  : MCP.config.runBuild ? "net.minecraft.client.Minecraft" : "Start"));
		for(int i = 1; i < MCP.config.runArgs.length; i++) {
			String arg = MCP.config.runArgs[i];
			if(!arg.equals("-runbuild")) {
				for(String arg2 : args) {
					if(arg.indexOf("=") > 0 && arg2.indexOf("=") > 0) {
						if(arg2.substring(0, arg2.indexOf("=")).equals(arg.substring(0, arg.indexOf("=")))) {
							args.remove(arg2);
							break;
						}
					}
					else if(arg2.equals(arg)) {
						args.remove(arg2);
						break;
					}
				}
				args.add(1, arg);
			}
		}
		int exit = Util.runCommand(args.toArray(new String[0]), Paths.get(MCPConfig.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
