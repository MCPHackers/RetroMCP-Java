package org.mcphackers.mcp.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.mcp.tools.versions.json.Version;

public class TaskRun extends Task {
	
	public TaskRun(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		Version currentVersion = mcp.getCurrentVersion();
		
		boolean runBuild = mcp.getOptions().getBooleanParameter(TaskParameter.RUN_BUILD);
		List<Path> cpList = getClasspath(mcp, currentVersion, side, runBuild);
		
		List<String> classPath = new ArrayList<>();
		cpList.forEach(p -> classPath.add(p.toAbsolutePath().toString()));
		String cp = String.join(System.getProperty("path.separator"), classPath);

		Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES).toAbsolutePath();

		List<String> args = new ArrayList<>(
				Arrays.asList(
						Util.getJava(),
						"-Djava.library.path=" + natives,
						"-cp", cp,
						currentVersion.mainClass
						)
				);
		String[] runArgs = mcp.getOptions().getStringArrayParameter(TaskParameter.RUN_ARGS);
		for (String arg : runArgs) {
			args.add(1, arg);
		}
		int exit = Util.runCommand(args.toArray(new String[0]), MCPPaths.get(mcp, MCPPaths.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}

	private static List<Path> getClasspath(MCP mcp, Version version, Side side, boolean runBuild) throws IOException {
		List<Path> cpList = new ArrayList<>();
		if(runBuild) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP, side));
		}
		if(!Files.exists(MCPPaths.get(mcp, MCPPaths.REMAPPED, side))) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
		}
		else {
			cpList.add(MCPPaths.get(mcp, MCPPaths.REMAPPED, side));
		}
		if(side == Side.CLIENT || side == Side.MERGED) {
			cpList.addAll(DownloadData.getLibraries(mcp, version));
		}
		return cpList;
	}
}
