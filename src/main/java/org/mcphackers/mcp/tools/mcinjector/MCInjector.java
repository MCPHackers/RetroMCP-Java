package org.mcphackers.mcp.tools.mcinjector;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import mcp.mcinjector.LogFormatter;
import mcp.mcinjector.MCInjectorImpl;

public class MCInjector extends MCInjectorImpl {

	protected MCInjector(int index) {
		super(index);
	}

    public static void process(String inFile, String outFile, String mapFile, String logFile, String outMapFile, int index)
        throws IOException
    {
        MCInjectorImpl.log.setUseParentHandlers(false);
        MCInjectorImpl.log.setLevel(Level.ALL);

        if (logFile != null)
        {
            FileHandler filehandler = new FileHandler(logFile);
            filehandler.setFormatter(new LogFormatter());
            MCInjectorImpl.log.addHandler(filehandler);
        }

        MCInjectorImpl mci = new MCInjector(index);
        mci.loadMap(mapFile);
        mci.processJar(inFile, outFile);
        if (outMapFile != null)
        {
            mci.saveMap(outMapFile);
        }
    }

}
