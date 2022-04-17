package org.mcphackers.mcp.tools.mcinjector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

import mcp.mcinjector.MCInjectorImpl;

public class MCInjector extends MCInjectorImpl {

	protected MCInjector(int index) {
		super(index);
	}

	public static void process(Path inFile, Path outFile, Path mapFile, int index) throws IOException
	{
		MCInjectorImpl.log.setUseParentHandlers(false);
		MCInjectorImpl.log.setLevel(Level.ALL);
		MCInjectorImpl mci = new MCInjector(index);
		mci.loadMap(mapFile.toString());
		mci.processJar(inFile.toString(), outFile.toString());
	}

}
