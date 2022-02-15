package org.mcphackers.mcp.tasks;

import java.util.Comparator;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskTest extends Task {

	public TaskTest(TaskInfo info) {
		super(-1, info);
	}

	@Override
	public void doTask() throws Exception {
		List<String> versions = VersionsParser.getVersionList();
		versions.sort(Comparator.naturalOrder());
		String chosenVersion = MCP.config.setupVersion;
		if(!versions.contains(chosenVersion)) {
			throw new Exception("Not a valid version");
		}
		new TaskSetup(info).doTask();
		new TaskDecompile(CLIENT, info).doTask();
		if(VersionsParser.hasServer()) new TaskDecompile(SERVER, info).doTask();
		new TaskReobfuscate(CLIENT, info).doTask();
		if(VersionsParser.hasServer()) new TaskReobfuscate(SERVER, info).doTask();
		new TaskBuild(CLIENT, info).doTask();
		if(VersionsParser.hasServer()) new TaskBuild(SERVER, info).doTask();
		new TaskCleanup(info).doTask();

	}

}
