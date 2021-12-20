package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.PatchOperation;
import net.fabricmc.tinyremapper.*;
import org.mcphackers.mcp.Conf;
import org.mcphackers.mcp.tasks.mcinjector.MCInjector;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;
import org.mcphackers.mcp.tools.decompile.Decompiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class TaskDecompile extends Task {

    private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

    private final Decompiler decompiler;
    private final int side;
	private TaskUpdateMD5 md5Task;

    public TaskDecompile(int side) {
        this.side = side;
        decompiler = new Decompiler();
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get("src"))) {
            throw new Exception("! /src exists! Aborting.");
        }
        String originalJar = side == 1 ? Conf.SERVER : Conf.CLIENT;
        String rgout = side == 1 ? Conf.SERVER_RG_OUT : Conf.CLIENT_RG_OUT;
        String excout = side == 1 ? Conf.SERVER_EXC_OUT : Conf.CLIENT_EXC_OUT;
        String exc = side == 1 ? Conf.EXC_SERVER : Conf.EXC_CLIENT;
        Path srcPath = Utility.getPath((side == 1 ? Conf.SERVER_SOURCES : Conf.CLIENT_SOURCES));
        Path patchesPath = Utility.getPath((side == 1 ? Conf.SERVER_PATCHES : Conf.CLIENT_PATCHES));

        step();
        // Remap Minecraft client JAR
        TinyRemapper remapper = null;

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(rgout)).build()) {
            remapper = remap(TinyUtils.createTinyMappingProvider(Paths.get(side == 1 ? Conf.SERVER_MAPPINGS : Conf.CLIENT_MAPPINGS), "official", "named"), Paths.get(originalJar), outputConsumer, Paths.get("jars", "bin"));
            outputConsumer.addNonClassFiles(Paths.get(originalJar), NonClassCopyMode.FIX_META_INF, remapper);
        } finally {
            if (remapper != null) {
                remapper.finish();
            }
        }
        step();
        // Apply MCInjector
        MCInjector.process(rgout, excout, exc, null, null, 0);
        step();
        // Decompile
        decompiler.decompile(excout, "temp/cls");
        step();
        // Extract sources
        Utility.unzipByExtension(Utility.getPath((side == 1 ? "temp/cls/minecraft_server_exc.jar" : "temp/cls/minecraft_exc.jar")), srcPath, ".java", Conf.ignorePackages);
        step();
        // Apply patches
        PatchOperation patchOperation = PatchOperation.builder()
                .verbose(true)
                .basePath(srcPath)
                .patchesPath(patchesPath)
                .outputPath(srcPath)
                .build();
        int code = patchOperation.operate().exit;
        if (code != 0) {
        	throw new Exception("Patching failed!!!");
        }
        step();
        if(side == 1) {
        	throw new Exception("Test");
        }
        new TaskRecompile(side).doTask();
        this.md5Task = new TaskUpdateMD5(side);
        step();
        this.md5Task.doTask();
        this.md5Task = null;
    }

    public static TinyRemapper remap(IMappingProvider mappings, Path input, BiConsumer<String, byte[]> consumer, Path... classpath) {
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .invalidLvNamePattern(MC_LV_PATTERN)
                .withMappings(mappings)
                .fixPackageAccess(true)
                .threads(Runtime.getRuntime().availableProcessors() - 3)
                .rebuildSourceFilenames(true)
                .build();

        remapper.readClassPath(classpath);
        remapper.readInputs(input);
        remapper.apply(consumer);

        return remapper;
    }

    public ProgressInfo getProgress() {
    	//Don't look here
    	int total = 100;
    	int current = 0;
        if (step == 1) {
        	current = 1;
            return new ProgressInfo("Remapping JAR...", current, total);
        }
        if (step == 2) {
        	current = 2;
            return new ProgressInfo("Applying MCInjector...", current, total);
        }
        if (step == 3 && decompiler != null) {
        	current = 3;
        	ProgressInfo info = decompiler.log.initInfo();
        	int percent = (int) (info.progress[0] * 100 / info.progress[1] * 0.84D);
        	info.progress[0] = current + percent;
        	info.progress[1] = total;
            return info;
        }
        if (step == 4) {
        	current = 88;
            return new ProgressInfo("Extracting sources...", current, total);
        }
        if (step == 5) {
        	current = 89;
            return new ProgressInfo("Applying patches...", current, total);
        }
        if (step == 6) {
        	current = 90;
            return new ProgressInfo("Recompiling...", current, total);
        }
        if (step == 7 && md5Task != null) {
        	current = 91;
        	ProgressInfo info = md5Task.getProgress();
        	int percent = (int) (info.progress[0] * 100 / info.progress[1] * 0.1D);
        	info.progress[0] = current + percent;
        	info.progress[1] = total;
            return info;
        }
        return super.getProgress();
    }
}
