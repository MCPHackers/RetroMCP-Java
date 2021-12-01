package org.mcphackers.mcp;

import org.mcphackers.mcp.tools.Decompile;
import org.mcphackers.mcp.tools.DecompileInfo;

public class SideThread extends Thread {
	
	private int side;
	private Decompile decompile;
	
	public SideThread(int i) {
		side = i;
		decompile = new Decompile(side);
	}

	public void run()
	{
		decompile.start();
	}

	public String getSideName()
	{
		return decompile.getSide();
	}

	public DecompileInfo getInfo()
	{
		return decompile.log.initInfo();
	}
}
