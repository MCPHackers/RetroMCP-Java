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

    @Override
    public void doTask() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();

        Path clientBinPath = Paths.get("bin", "minecraft");
        Path clientSrcPath = Paths.get("src", "minecraft");
        Path serverBinPath = Paths.get("bin", "minecraft_server");
        Path serverSrcPath = Paths.get("src", "minecraft_server");

        if (Files.exists(clientBinPath)) {
            Utility.deleteDirectoryStream(clientBinPath);
        }

        if (Files.exists(serverBinPath)) {
            Utility.deleteDirectoryStream(serverBinPath);
        }

        Files.createDirectories(clientBinPath);
        Files.createDirectories(serverBinPath);

        // Compile client
        if (Files.exists(clientSrcPath)) {
            Iterable<File> clientSrc = Files.walk(clientSrcPath).filter(path -> !Files.isDirectory(path)).map(Path::toFile).collect(Collectors.toList());
            Iterable<String> options = Arrays.asList("-d", "bin/minecraft", "-cp", "jars/bin/minecraft.jar;jars/bin/lwjgl_util.jar;jars/bin/lwjgl.jar;jars/bin/jinput.jar");
            recompile(compiler, ds, clientSrc, options);
        } else {
            System.err.println("Client sources not found!");
        }

        // Compile server
        if (Files.exists(serverSrcPath)) {
            Iterable<File> serverSrc = Files.walk(serverSrcPath).filter(path -> !Files.isDirectory(path)).map(Path::toFile).collect(Collectors.toList());
            Iterable<String> options = Arrays.asList("-d", "bin/minecraft_server", "-cp", "jars/minecraft_server.jar;jars/bin/lwjgl_util.jar;jars/bin/lwjgl.jar;jars/bin/jinput.jar");
            recompile(compiler, ds, serverSrc, options);
        } else {
            System.err.println("Server sources not found!");
        }
    }

    public void recompile(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> ds, Iterable<File> serverSrc, Iterable<String> recompileOptions) {
        try (StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null)) {
            Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(serverSrc);
            JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, recompileOptions, null, sources);
            boolean success = task.call();
            if (!success) {
                for (Diagnostic<?> diagnostic : ds.getDiagnostics()) {
                    System.err.println(diagnostic.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Recompiling...", 0, 1);
    }
}
