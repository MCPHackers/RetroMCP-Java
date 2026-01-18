package org.mcphackers.mcp.tools.project;

import static org.mcphackers.mcp.MCPPaths.PROJECT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskRun;

public class VSCProjectWriter implements ProjectWriter {

	@Override
	public void createProject(MCP mcp, Side side, int sourceVersion) throws IOException {
		Path proj = MCPPaths.get(mcp, PROJECT, side);
		Task.Side[] launchSides = side == Task.Side.MERGED ? new Task.Side[]{Task.Side.CLIENT, Task.Side.SERVER} : new Task.Side[]{side};

		String projectName = "Minecraft " + (side == Task.Side.CLIENT ? "Client" : side == Task.Side.SERVER ? "Server" : side == Task.Side.MERGED ? "Merged" : "Project");

		Files.createDirectories(proj.resolve(".vscode"));
		try (BufferedWriter writer = Files.newBufferedWriter(proj.resolve(".vscode/settings.json"))) {
			JSONObject settingsJson = new JSONObject();
			JSONObject searchExclude = new JSONObject();
			searchExclude.put("src_original/**", true);
			searchExclude.put("bin/**", true);
			searchExclude.put("output/**", true);
			settingsJson.put("search.exclude", searchExclude);
			settingsJson.write(writer);
		}
		try (BufferedWriter writer = Files.newBufferedWriter(proj.resolve(".vscode/launch.json"))) {
			JSONObject launchJson = new JSONObject();
			launchJson.put("version", "0.2.0");
			JSONArray configurations = new JSONArray();
			for (Task.Side launchSide : launchSides) {
				JSONObject config = new JSONObject();
				config.put("type", "java");
				config.put("name", projectName);
				config.put("request", "launch");
				config.put("mainClass", TaskRun.getMain(mcp, mcp.getCurrentVersion(), launchSide));
				config.put("vmArgs", "-Djava.library.path=${workspaceFolder}/../libraries/natives");
				config.put("projectName", projectName);
				List<String> args = TaskRun.getLaunchArgs(mcp, launchSide);
				JSONArray arguments = new JSONArray();
				for (String arg : args) {
					arguments.put(arg);
				}
				config.put("args", arguments);
				configurations.put(config);
			}
			launchJson.put("configurations", configurations);
			launchJson.write(writer);
		}
	}

}
