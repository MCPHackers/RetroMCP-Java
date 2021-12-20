package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TaskRecompile extends Task {
    private final int side;
	private int total;
	private int progress;

    public TaskRecompile(int side) {
		this.side = side;
        this.total = 1;
        this.progress = 0;
	}

	@Override
    public void doTask() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();

        Path binPath = side == 1 ? Paths.get("bin", "minecraft_server") : Paths.get("bin", "minecraft");
        Path srcPath = side == 1 ? Paths.get("src", "minecraft_server") : Paths.get("src", "minecraft");

        step();
        if (Files.exists(binPath)) {
            Utility.deleteDirectoryStream(binPath);
        }

        Files.createDirectories(binPath);

        // Compile side
        if (Files.exists(srcPath)) {
            Iterable<File> src = Files.walk(srcPath).filter(path -> !Files.isDirectory(path)).map(Path::toFile).collect(Collectors.toList());
            Iterable<String> options = Arrays.asList("-d", "bin/minecraft", "-cp", "jars/bin/minecraft.jar;jars/bin/lwjgl_util.jar;jars/bin/lwjgl.jar;jars/bin/jinput.jar");
            if(side == 1) {
            	options = Arrays.asList("-d", "bin/minecraft_server", "-cp", "jars/minecraft_server.jar");
            }
            recompile(compiler, ds, src, options);
        } else {
        	throw new IOException((side == 1 ? "Server" : "Client") + " sources not found!");
        }
    }

	public void recompile(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> ds, Iterable<File> src, Iterable<String> recompileOptions) throws IOException, RuntimeException {
        StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null);
        Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(src);
        JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, recompileOptions, null, sources);
        mgr.close();
        boolean success = task.call();
        if (!success) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : ds.getDiagnostics())
                MCP.logger.error(String.format("Error on line %d in %s%n",
                                  diagnostic.getLineNumber(),
                                  diagnostic.getSource().toUri()));
            throw new RuntimeException("Compilation error!");
        }
    }

    @Override
    public ProgressInfo getProgress() {
    	//TODO: Progress values stay at 0 and 1. Add a way to monitor progress of compilation.
        if(step == 1) {
        	return new ProgressInfo("Recompiling...", progress, total);
        }
        return super.getProgress();
    }
}
