package org.mcphackers.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.mcphackers.mcp.tools.Util;

public class Update {
	
	public static void main(String[] args) {
    	if(args.length >= 1) {
    		boolean keepTrying = true;
    		long startTime = System.currentTimeMillis();
    		while(keepTrying) {
				try {
					Files.deleteIfExists(Paths.get(args[0]));
					Files.copy(Paths.get(MCPConfig.UPDATE_JAR), Paths.get(args[0]));
					Util.runCommand(new String[] {
						Util.getJava(),
						"-jar",
						args[0]
					});
					keepTrying = false;
				} catch (IOException e) {
					keepTrying = System.currentTimeMillis() - startTime < 10000;
				}
    		}
    	}
	}
}
