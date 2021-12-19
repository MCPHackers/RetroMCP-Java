package org.mcphackers.mcp.tasks;

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

public class TaskRecompile implements Task {
    private final int side;

    public TaskRecompile(int side) {
		this.side = side;
	}

	@Override
    public void doTask() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();

        Path binPath = side == 1 ? Paths.get("bin", "minecraft_server") : Paths.get("bin", "minecraft");
        Path srcPath = side == 1 ? Paths.get("src", "minecraft_server") : Paths.get("src", "minecraft");

        if (Files.exists(binPath)) {
            Utility.deleteDirectoryStream(binPath);
        }

        Files.createDirectories(binPath);

        // Compile side
        if (Files.exists(srcPath)) {
            Iterable<File> clientSrc = Files.walk(srcPath).filter(path -> !Files.isDirectory(path)).map(Path::toFile).collect(Collectors.toList());
            Iterable<String> options = Arrays.asList("-d", "bin/minecraft", "-cp", "jars/bin/minecraft.jar;jars/bin/lwjgl_util.jar;jars/bin/lwjgl.jar;jars/bin/jinput.jar");
            recompile(compiler, ds, clientSrc, options);
        } else {
        	throw new IOException((side == 1 ? "Server" : "Client") + " sources not found!");
        }
    }

    public void recompile(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> ds, Iterable<File> serverSrc, Iterable<String> recompileOptions) throws RuntimeException, IOException {
        StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null);
        Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(serverSrc);
        JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, recompileOptions, null, sources);
        boolean success = task.call();
        if (!success) {
            for (Diagnostic<?> diagnostic : ds.getDiagnostics()) {
                throw new RuntimeException(diagnostic.toString());
            }
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Recompiling...", 0, 1);
    }
}
