package org.mcphackers.mcp.tasks;

import java.io.FileWriter;
import java.io.IOException;

import org.mcphackers.mcp.Conf;
import org.mcphackers.mcp.tools.Decompiler;
import org.mcphackers.mcp.tools.ProgressInfo;

import COM.rl.NameProvider;
import COM.rl.obf.RetroGuardImpl;

public class TaskDecompile extends Task {

	private Decompiler decompiler;
	private int step;
	
	public TaskDecompile(int side) {
		super(side);
		step = 0;
		decompiler = new Decompiler();
	}

	public void doTask() throws Exception {
		
		String src = side == 1 ? Conf.SERVER : Conf.CLIENT;
		String rgout = side == 1 ? Conf.SERVER_RG_OUT : Conf.CLIENT_RG_OUT;
		String ffout = side == 1 ? Conf.SERVER_FF_OUT : Conf.CLIENT_FF_OUT;
		
		step = 0;
		createRgCfg();
		NameProvider.parseCommandLine(new String[] {"-searge", Conf.CFG_RG});
		RetroGuardImpl.obfuscate(src, rgout, null, null);
		step = 1;
		decompiler.decompile(rgout, ffout);
	}

	public ProgressInfo getProgress() {
		switch(step) {
		case 0:
			return new ProgressInfo("Renaming...", 0, 1);
		default:
			return decompiler.log.initInfo();
		}
	}
	
	private void createRgCfg() throws IOException
	{
		boolean reobf = false;
		boolean keep_lvt = true;
		boolean keep_generics = false;
		FileWriter rgout = new FileWriter(Conf.CFG_RG);
		rgout.write(".option Application\n");
		rgout.write(".option Applet\n");
		rgout.write(".option Repackage\n");
		rgout.write(".option Annotations\n");
		rgout.write(".option MapClassString\n");
		rgout.write(".attribute LineNumberTable\n");
		rgout.write(".attribute EnclosingMethod\n");
		rgout.write(".attribute Deprecated\n");
		if (keep_lvt)
			rgout.write(".attribute LocalVariableTable\n");
		if (keep_generics) {
			rgout.write(".option Generic\n");
			rgout.write(".attribute LocalVariableTypeTable\n");
		}
		if (reobf)
			rgout.write(".attribute SourceFile\n");
		rgout.close();
	}

}
