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
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskRun extends Task {
	public TaskRun(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		boolean runBuild = mcp.getBooleanParam(TaskParameter.RUN_BUILD);
		if(side == Side.SERVER && !VersionsParser.hasServer()) {
			throw new Exception("Server isn't available for this version!");
		}
		String java = Util.getJava();
		String natives = FileUtil.absolutePathString(MCPPaths.NATIVES);
		List<String> cpList = new LinkedList<>();
		if(side == Side.SERVER) {
			if(runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPPaths.BUILD_ZIP_SERVER));
			}
			cpList.add(FileUtil.absolutePathString(MCPPaths.SERVER_BIN));
			cpList.add(FileUtil.absolutePathString(MCPPaths.SERVER));
			try(Stream<Path> stream = Files.list(Paths.get(MCPPaths.DEPS_S)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
		}
		else if (side == Side.CLIENT) {
			if(runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPPaths.BUILD_ZIP_CLIENT));
			}
			cpList.add(FileUtil.absolutePathString(MCPPaths.CLIENT_BIN));
			if(!Files.exists(Paths.get(MCPPaths.CLIENT_FIXED))) {
				cpList.add(FileUtil.absolutePathString(MCPPaths.CLIENT));
			}
			try(Stream<Path> stream = Files.list(Paths.get(MCPPaths.LIB)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
			try(Stream<Path> stream = Files.list(Paths.get(MCPPaths.DEPS_C)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
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
						side == Side.SERVER ? (VersionsParser.getServerVersion().startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer")
																	  : runBuild ? "net.minecraft.client.Minecraft" : "Start"));
		for(int i = 1; i < mcp.getStringArrayParam(TaskParameter.RUN_ARGS).length; i++) {
			String arg = mcp.getStringArrayParam(TaskParameter.RUN_ARGS)[i];
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
		int exit = Util.runCommand(args.toArray(new String[0]), Paths.get(MCPPaths.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}

	@Override
	public String getName() {
		return "Run game";
	}
}
