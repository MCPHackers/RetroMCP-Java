package org.mcphackers.mcp.tools.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;

public class LegacyProjectAdapter {
	public static void updateWorkspaceIfNeeded(MCP mcp) {
		if (isV100Project(mcp)) {
			mcp.log("v1.0 instance detected! Updating workspace automatically...");
			convert100To110(mcp);
		}

		// TODO: Add implementations for earlier versions of RMCP.
	}

	/**
	 * @param mcp MCP instance
	 * @return If the current MCP instance is a v1.0 instance
	 */
	public static boolean isV100Project(MCP mcp) {
		for (Task.Side side : Task.Side.values()) {
			Path oldSourceFolder = MCPPaths.get(mcp, MCPPaths.PROJECT + "source", side);
			if (Files.exists(oldSourceFolder)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts a v1.0 project to v1.1.
	 * <p>
	 * RMCP v1.1 differences:
	 * (side)/source was renamed to (side)/src_original
	 *
	 * @param mcp MCP instance
	 */
	public static void convert100To110(MCP mcp) {
		// Rename source to src_original folder in each side
		for (Task.Side side : Task.Side.values()) {
			Path oldSourceFolder = MCPPaths.get(mcp, MCPPaths.PROJECT + "source", side);
			Path newSourceFolder = MCPPaths.get(mcp, MCPPaths.SOURCE_UNPATCHED, side);
			if (Files.exists(oldSourceFolder)) {
				try {
					Files.move(oldSourceFolder, newSourceFolder);
				} catch (IOException e) {
					mcp.error("Failed to move " + oldSourceFolder + " to " + newSourceFolder + ".\nPlease close RMCP and move the folder yourself.");
				}
			}
		}
	}
}
