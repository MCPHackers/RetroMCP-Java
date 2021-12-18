package org.mcphackers.mcp.tasks;

import COM.rl.NameProvider;
import COM.rl.obf.RetroGuardImpl;
import org.mcphackers.mcp.Conf;
import org.mcphackers.mcp.tools.decompile.Decompiler;
import org.mcphackers.mcp.tools.ProgressInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

public class TaskDecompile implements Task {

    private final Decompiler decompiler;
    private int step;
    private final int side;

    public TaskDecompile(int side) {
        this.side = side;
        step = 0;
        decompiler = new Decompiler();
    }

    public void doTask() throws Exception {

        String src = side == 1 ? Conf.SERVER.toString() : Conf.CLIENT.toString();
        String rgout = side == 1 ? Conf.SERVER_CLASSES.toString() : Conf.CLIENT_CLASSES.toString();
        String ffout = side == 1 ? Conf.SERVER_SOURCES.toString() : Conf.CLIENT_SOURCES.toString();

        step = 0;
        createRgCfg();
        //NameProvider.parseCommandLine(new String[]{"-searge", Conf.CFG_RG.toString()});
        RetroGuardImpl.obfuscate(src, rgout, null, null);
        step = 1;
        decompiler.decompile(rgout, ffout);
    }

    public ProgressInfo getProgress() {
        if (step == 0) {
            return new ProgressInfo("Renaming...", 0, 1);
        }
        return decompiler.log.initInfo();
    }

    private void createRgCfg() throws IOException {
        boolean reobf = false;
        boolean keep_lvt = true;
        boolean keep_generics = false;
        /*BufferedWriter rgout = Files.newBufferedWriter(Conf.CFG_RG);
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
        rgout.close();*/
    }

}
