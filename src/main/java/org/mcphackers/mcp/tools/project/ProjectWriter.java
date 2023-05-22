package org.mcphackers.mcp.tools.project;

import java.io.IOException;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskRun;

public interface ProjectWriter {
	static String getLaunchArgs(MCP mcp, Side side) {
		List<String> args = TaskRun.getLaunchArgs(mcp, side);
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if (arg.contains(" ")) {
				arg = "\"" + arg + "\"";
			}
			args.set(i, arg);
		}
		return String.join(" ", args).replace("\"", "&quot;");
	}

	void createProject(MCP mcp, Side side, int sourceVersion) throws IOException;
}
