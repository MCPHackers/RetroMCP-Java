package org.mcphackers.mcp.tasks;

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

        String src = side == 1 ? Conf.SERVER : Conf.CLIENT;
        String rgout = side == 1 ? Conf.SERVER_RG_OUT : Conf.CLIENT_RG_OUT;
        String ffout = side == 1 ? Conf.SERVER_FF_OUT : Conf.CLIENT_FF_OUT;

        if (Files.exists(Paths.get("temp"))) {
            Utility.deleteDirectoryStream(Paths.get("temp"));
        }

        step = 0;
        // Remap Minecraft client JAR
        if (side == 0) {
            TinyRemapper remapper = null;

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(rgout)).build()) {
                remapper = remap(TinyUtils.createTinyMappingProvider(Paths.get("conf", "client.tiny"), "official", "named"), Paths.get(src), outputConsumer, Paths.get("jars", "bin"));
                outputConsumer.addNonClassFiles(Paths.get(src), NonClassCopyMode.FIX_META_INF, remapper);
            } finally {
                if (remapper != null) {
                    remapper.finish();
                }
            }
            step = 1;
            decompiler.decompile(rgout, "temp/cls");
        }

        if (side == 1) {
            TinyRemapper remapper = null;

            try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(rgout)).build()) {
                remapper = remap(TinyUtils.createTinyMappingProvider(Paths.get("conf", "server.tiny"), "official", "named"), Paths.get(src), outputConsumer, Paths.get("jars", "bin"));
                outputConsumer.addNonClassFiles(Paths.get(src), NonClassCopyMode.FIX_META_INF, remapper);
            } finally {
                if (remapper != null) {
                    remapper.finish();
                }
            }
            step = 1;
            decompiler.decompile(rgout, "temp/cls");
        }
    }

    private static TinyRemapper remap(IMappingProvider mappings, Path input, BiConsumer<String, byte[]> consumer, Path... classpath) {
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
            return new ProgressInfo("Renaming...", 0, 1);
        }
        return decompiler.log.initInfo();
    }

}
