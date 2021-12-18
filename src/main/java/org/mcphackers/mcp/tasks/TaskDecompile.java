package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.DiffOperation;
import codechicken.diffpatch.cli.PatchOperation;
import mcp.mcinjector.MCInjectorImpl;
import net.fabricmc.tinyremapper.*;
import org.mcphackers.mcp.Conf;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;
import org.mcphackers.mcp.tools.decompile.Decompiler;

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
        String ffout = side == 1 ? Conf.SERVER_FF_OUT : Conf.CLIENT_FF_OUT;
        Path srcPath = side == 1 ? Paths.get("src", "minecraft_server") : Paths.get("src", "minecraft");

        step = 0;
        if (side == 0) {
            // Remap Minecraft client JAR
            System.out.println("> Remapping client JAR...");
            TinyRemapper remapper = null;

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(rgout)).build()) {
                remapper = remap(TinyUtils.createTinyMappingProvider(Paths.get(Conf.CLIENT_MAPPINGS), "official", "named"), Paths.get(originalJar), outputConsumer, Paths.get("jars", "bin"));
                outputConsumer.addNonClassFiles(Paths.get(originalJar), NonClassCopyMode.FIX_META_INF, remapper);
            } finally {
                if (remapper != null) {
                    remapper.finish();
                }
            }
            step = 1;

            // Apply MCInjector
            System.out.println("> Applying MCInjector...");
            MCInjectorImpl.process(rgout, excout, Paths.get("conf", "client.exc").toString(), null, null, 0);

            // Decompile and extract sources
            System.out.println("> Decompiling...");
            decompiler.decompile(excout, "temp/cls");
            System.out.println("> Extracting sources...");
            Utility.unzipByExtension(Paths.get("temp", "cls", "minecraft_exc.jar"), Paths.get("src", "minecraft"), ".java");

            // Apply patches
            System.out.println("> Applying patches...");
            PatchOperation patchOperation = PatchOperation.builder()
                    .verbose(true)
                    .basePath(srcPath)
                    .patchesPath(Paths.get("conf", "patches_client"))
                    .outputPath(srcPath)
                    .build();
            int code = patchOperation.operate().exit;
            if (code != 0) {
                System.err.println("Patching failed!!!");
            }
        }

        if (side == 1) {
            // Remap Minecraft server JAR
            System.out.println("> Remapping server JAR...");
            TinyRemapper remapper = null;

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(rgout)).build()) {
                remapper = remap(TinyUtils.createTinyMappingProvider(Paths.get("conf", "server.tiny"), "official", "named"), Paths.get(originalJar), outputConsumer, Paths.get("jars", "bin"));
                outputConsumer.addNonClassFiles(Paths.get(originalJar), NonClassCopyMode.FIX_META_INF, remapper);
            } finally {
                if (remapper != null) {
                    remapper.finish();
                }
            }
            step = 1;

            // Apply MCInjector
            System.out.println("> Applying MCInjector...");
            MCInjectorImpl.process(rgout, excout, Paths.get("conf", "server.exc").toString(), null, null, 0);

            // Decompile and extract sources
            System.out.println("> Decompiling...");
            decompiler.decompile(excout, "temp/cls");
            System.out.println("> Extracting sources...");
            Utility.unzipByExtension(Paths.get("temp", "cls", "minecraft_server_exc.jar"), Paths.get("src", "minecraft_server"), ".java");

            // Apply patches
            System.out.println("> Applying patches...");
            PatchOperation patchOperation = PatchOperation.builder()
                    .verbose(true)
                    .basePath(srcPath)
                    .patchesPath(Paths.get("conf", "patches_server"))
                    .outputPath(srcPath)
                    .build();
            int code = patchOperation.operate().exit;
            if (code != 0) {
                System.err.println("Patching failed!!!");
            }
        }

        new TaskRecompile().doTask();
        new TaskUpdateMD5().doTask();
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
            return new ProgressInfo("Decompiling...", 0, 1);
        }
        return decompiler.log.initInfo();
    }

}
