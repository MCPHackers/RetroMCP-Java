package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.PatchOperation;
import net.fabricmc.tinyremapper.*;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.GLConstants;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.mcinjector.MCInjector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class TaskDecompile extends Task {

    private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

    private final Decompiler decompiler;
	private TaskUpdateMD5 md5Task;
	
	private static final int REMAP = 1;
	private static final int EXCEPTOR = 2;
	private static final int DECOMPILE = 3;
	private static final int EXTRACT = 4;
	private static final int PATCH = 5;
	private static final int CONSTS = 6;
	private static final int COPYSRC = 7;
	private static final int RECOMPILE = 8;
	private static final int MD5 = 9;
    private static final int STEPS = 9;

    public TaskDecompile(int side, TaskInfo info) {
        super(side, info);
        decompiler = new Decompiler();
        md5Task = new TaskUpdateMD5(side, info);
        md5Task.recompile = false;
    }

    @Override
    public void doTask() throws Exception {
        String originalJar 	= side == 1 ? MCPConfig.SERVER 				: MCPConfig.CLIENT;
        String tinyOut 		= side == 1 ? MCPConfig.SERVER_TINY_OUT 	: MCPConfig.CLIENT_TINY_OUT;
        String excOut 		= side == 1 ? MCPConfig.SERVER_EXC_OUT 		: MCPConfig.CLIENT_EXC_OUT;
        String exc 			= side == 1 ? MCPConfig.EXC_SERVER 			: MCPConfig.EXC_CLIENT;
		String ffOut 		= side == 1 ? MCPConfig.SERVER_TEMP_SOURCES : MCPConfig.CLIENT_TEMP_SOURCES;
		String srcZip 		= side == 1 ? MCPConfig.SERVER_SRC 			: MCPConfig.CLIENT_SRC;
        Path srcPath 		= Util.getPath((side == 1 ? MCPConfig.SERVER_SOURCES : MCPConfig.CLIENT_SOURCES));
        Path patchesPath 	= Util.getPath((side == 1 ? MCPConfig.SERVER_PATCHES : MCPConfig.CLIENT_PATCHES));
        
        if (Files.exists(srcPath)) {
            throw new Exception("/src exists! Aborting.");
        }
        Path[] pathsToDelete = new Path[] { Util.getPath(tinyOut), Util.getPath(excOut), Util.getPath(ffOut), Util.getPath(srcZip)};
        for (Path path : pathsToDelete) {
        	if (Files.exists(path)) {
        		Util.deleteDirectory(path);
        	}
        }
        
		while(step < STEPS) {
		    step();
		    switch (step) {
			case REMAP:
		        TinyRemapper remapper = null;
		
		        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get(tinyOut)).build()) {
		            remapper = remap(TinyUtils.createTinyMappingProvider(Util.getPath(side == 1 ? MCPConfig.SERVER_MAPPINGS : MCPConfig.CLIENT_MAPPINGS), "official", "named"), Paths.get(originalJar), outputConsumer, Paths.get("jars", "bin"));
		            outputConsumer.addNonClassFiles(Util.getPath(originalJar), NonClassCopyMode.FIX_META_INF, remapper);
		        } finally {
		            if (remapper != null) {
		                remapper.finish();
		            }
		        }
		        break;
		    case EXCEPTOR:
			    MCInjector.process(tinyOut, excOut, exc, null, null, 0);
		    	break;
		    case DECOMPILE:
				this.decompiler.decompile(excOut, srcZip, side == 1 ? MCPConfig.JAVADOC_SERVER : MCPConfig.JAVADOC_CLIENT);
		    	break;
		    case EXTRACT:
				if(Files.notExists(Paths.get("src"))) {
					Files.createDirectory(Paths.get("src"));
				}
				Util.unzipByExtension(Util.getPath(srcZip), Util.getPath(ffOut), ".java");
		    	break;
		    case PATCH:
		    	if(MCPConfig.patch) {
				    PatchOperation patchOperation = PatchOperation.builder()
			            .verbose(true)
			            .basePath(Util.getPath(ffOut))
			            .patchesPath(patchesPath)
			            .outputPath(Util.getPath(ffOut))
			            .build();
				    int code = patchOperation.operate().exit;
				    if (code != 0) {
				    	info.addInfo("Patching failed!");
				    }
		    	}
		    	break;
		    case CONSTS:
				GLConstants.replace(Util.getPath(ffOut));
		    	break;
		    case COPYSRC:
				Util.copyDirectory(Util.getPath(ffOut), srcPath, MCPConfig.ignorePackages);
		    	break;
		    case RECOMPILE:
			    new TaskRecompile(side, info).doTask();
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
                .renameInvalidLocals(false)
                .rebuildSourceFilenames(true)
                .invalidLvNamePattern(MC_LV_PATTERN)
                .withMappings(mappings)
                .fixPackageAccess(false)
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
        	int percent = (int) (info.getCurrent() * 100 / info.getTotal() * 0.82D);
            return new ProgressInfo(info.getMessage(), current + percent, total); }
	    case EXTRACT:
        	current = 86;
            return new ProgressInfo("Extracting sources...", current, total);
	    case PATCH:
        	current = 87;
            return new ProgressInfo("Applying patches...", current, total);
	    case CONSTS:
        	current = 88;
            return new ProgressInfo("Replacing LWJGL constants...", current, total);
		case COPYSRC:
			current = 89;
			return new ProgressInfo("Copying sources...", current, total);
	    case RECOMPILE:
        	current = 90;
            return new ProgressInfo("Recompiling...", current, total);
	    case MD5: {
        	current = 91;
        	ProgressInfo info = md5Task.getProgress();
        	int percent = (int) (info.getCurrent() * 100 / info.getTotal() * 0.09D);
            return new ProgressInfo(info.getMessage(), current + percent, total); }
	    default:
	    	return super.getProgress();
        }
    }
}
