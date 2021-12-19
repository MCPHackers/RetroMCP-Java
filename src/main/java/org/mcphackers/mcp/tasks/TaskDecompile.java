package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.PatchOperation;
import net.fabricmc.tinyremapper.*;
import org.mcphackers.mcp.Conf;
import org.mcphackers.mcp.tasks.mcinjector.MCInjector;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;
import org.mcphackers.mcp.tools.decompile.Decompiler;

import java.io.PrintStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class TaskDecompile implements Task {

    private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

    private final Decompiler decompiler;
    private int step;
    private final int side;

    public TaskDecompile(int side) {
        this.side = side;
        step = 0;
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

        step = 0;
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
        step = 1;

        // Apply MCInjector
        MCInjector.process(rgout, excout, exc, null, null, 0);

        // Decompile and extract sources
        step = 2;
        decompiler.decompile(excout, "temp/cls");
        step = 3;
        //TODO: Client and Server
        Utility.unzipByExtension(Utility.getPath((side == 1 ? "temp/cls/minecraft_server_exc.jar" : "temp/cls/minecraft_exc.jar")), srcPath, ".java");

        // Apply patches
        step = 4;
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
        step = 5;
        new TaskRecompile(side).doTask();
        step = 6;
        new TaskUpdateMD5(side).doTask();
        step = 7;
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
        if (step == 0) {
            return new ProgressInfo("Remapping JAR...", 0, 1);
        }
        if (step == 1) {
            return new ProgressInfo("Applying MCInjector...", 0, 1);
        }
        if (step == 2) {
            return decompiler.log.initInfo();
        }
        if (step == 3) {
            return new ProgressInfo("Extracting sources...", 99, 100);
        }
        if (step == 4) {
            return new ProgressInfo("Applying patches...", 99, 100);
        }
        if (step == 5) {
            return new ProgressInfo("Recompiling...", 99, 100);
        }
        if (step == 6) {
            return new ProgressInfo("Updating MD5...", 99, 100);
        }
        return new ProgressInfo("Done!", 1, 1);
    }
}
