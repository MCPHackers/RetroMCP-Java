package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.PatchOperation;
import net.fabricmc.tinyremapper.*;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;
import org.mcphackers.mcp.tools.decompile.Decompiler;
import org.mcphackers.mcp.tools.mcinjector.MCInjector;

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
	
	private static final int REMAP = 1;
	private static final int EXCEPTOR = 2;
	private static final int DECOMPILE = 3;
	private static final int EXTRACT = 4;
	private static final int PATCH = 5;
	private static final int RECOMPILE = 6;
	private static final int MD5 = 7;
    private static final int STEPS = 7;

    public TaskDecompile(int side) {
        this.side = side;
        decompiler = new Decompiler();
        md5Task = new TaskUpdateMD5(side);
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get("src"))) {
            throw new Exception("! /src exists! Aborting.");
        }
        String originalJar = side == 1 ? MCPConfig.SERVER : MCPConfig.CLIENT;
        String rgout = side == 1 ? MCPConfig.SERVER_RG_OUT : MCPConfig.CLIENT_RG_OUT;
        String excout = side == 1 ? MCPConfig.SERVER_EXC_OUT : MCPConfig.CLIENT_EXC_OUT;
        String exc = side == 1 ? MCPConfig.EXC_SERVER : MCPConfig.EXC_CLIENT;
        Path srcPath = Utility.getPath((side == 1 ? MCPConfig.SERVER_SOURCES : MCPConfig.CLIENT_SOURCES));
        Path patchesPath = Utility.getPath((side == 1 ? MCPConfig.SERVER_PATCHES : MCPConfig.CLIENT_PATCHES));
		while(step < STEPS) {
		    step();
		    switch (step) {
			case REMAP:
		        TinyRemapper remapper = null;
		
		        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(rgout)).build()) {
		            remapper = remap(TinyUtils.createTinyMappingProvider(Utility.getPath(side == 1 ? MCPConfig.SERVER_MAPPINGS : MCPConfig.CLIENT_MAPPINGS), "official", "named"), Paths.get(originalJar), outputConsumer, Paths.get("jars", "bin"));
		            outputConsumer.addNonClassFiles(Utility.getPath(originalJar), NonClassCopyMode.FIX_META_INF, remapper);
		        } finally {
		            if (remapper != null) {
		                remapper.finish();
		            }
		        }
		        break;
		    case EXCEPTOR:
			    MCInjector.process(rgout, excout, exc, null, null, 0);
		    	break;
		    case DECOMPILE:
			    decompiler.decompile(excout, "temp/cls");
		    	break;
		    case EXTRACT:
		    	Utility.unzipByExtension(Utility.getPath((side == 1 ? "temp/cls/minecraft_server_exc.jar" : "temp/cls/minecraft_exc.jar")), srcPath, ".java", MCPConfig.ignorePackages);
		    	break;
		    case PATCH:
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
		    	break;
		    case RECOMPILE:
			    new TaskRecompile(side).doTask();
		    	break;
		    case MD5:
			    this.md5Task.doTask();
			    this.md5Task = null;
		    	break;
			}
		}
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
    	int total = 100;
    	int current = 0;
	    switch (step) {
	    case REMAP:
        	current = 1;
            return new ProgressInfo("Remapping JAR...", current, total);
	    case EXCEPTOR:
        	current = 2;
            return new ProgressInfo("Applying MCInjector...", current, total);
	    case DECOMPILE: {
        	current = 3;
        	ProgressInfo info = decompiler.log.initInfo();
        	int percent = (int) (info.progress[0] * 100 / info.progress[1] * 0.84D);
        	info.progress[0] = current + percent;
        	info.progress[1] = total;
            return info; }
	    case EXTRACT:
        	current = 88;
            return new ProgressInfo("Extracting sources...", current, total);
	    case PATCH:
        	current = 89;
            return new ProgressInfo("Applying patches...", current, total);
	    case RECOMPILE:
        	current = 90;
            return new ProgressInfo("Recompiling...", current, total);
	    case MD5: {
        	current = 91;
        	ProgressInfo info = md5Task.getProgress();
        	int percent = (int) (info.progress[0] * 100 / info.progress[1] * 0.1D);
        	info.progress[0] = current + percent;
        	info.progress[1] = total;
            return info; }
	    default:
	    	return super.getProgress();
        }
    }
}
