package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Utility;

public class TaskRun extends Task {
    public TaskRun(int side, TaskInfo info) {
        super(side, info);
    }

	@Override
	public void doTask() throws Exception {
		int exit = Utility.runCommand(
				"java -Xms1024M -Xmx1024M -Djava.util.Arrays.useLegacyMergeSort=true -cp " +
				String.join(";", new String[] {(side == 1 ? MCPConfig.SERVER_BIN : MCPConfig.CLIENT_BIN), (side == 1 ? MCPConfig.SERVER : MCPConfig.CLIENT), MCPConfig.LWJGL, MCPConfig.LWJGL_UTIL, MCPConfig.JINPUT}) +
				" -Dhttp.proxyHost=betacraft.uk -Dhttp.proxyPort=11702 -Djava.library.path=" + MCPConfig.NATIVES + " net.minecraft.client.Minecraft");
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
