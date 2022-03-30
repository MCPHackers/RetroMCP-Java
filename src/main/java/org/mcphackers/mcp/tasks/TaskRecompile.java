package org.mcphackers.mcp.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.ProgressInfo;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;

public class TaskRecompile extends Task {
	private final int total;
	private int progress;
	
	private static final int RECOMPILE = 1;
	private static final int COPYRES = 2;

	public TaskRecompile(int side, TaskInfo info) {
		super(side, info);
		this.total = 100;
		this.progress = 0;
	}

	@Override
	public void doTask() throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			throw new RuntimeException("Could not find compiling API");
		}
		DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();

		Path binPath = Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, 		MCPConfig.SERVER_BIN));
		Path srcPath = Paths.get(chooseFromSide(MCPConfig.CLIENT_SOURCES, 	MCPConfig.SERVER_SOURCES));

		step();
		this.progress = 1;
		FileUtil.deleteDirectoryIfExists(binPath);
		Files.createDirectories(binPath);
		this.progress = 2;

		// Compile side
		if (Files.exists(srcPath)) {
			List<File> src = Files.walk(srcPath).filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
			List<File> start = Files.walk(Paths.get(MCPConfig.CONF + "start")).filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
			if(side == CLIENT) {
				src.addAll(start);
			}

			List<String> libraries;
			List<String> options = new ArrayList<>();

			try(Stream<Path> stream = Files.list(Paths.get(MCPConfig.LIB)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				libraries = stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList());
			}

			if(side == SERVER) {
				libraries.add(0, MCPConfig.SERVER);
				try(Stream<Path> stream = Files.list(Paths.get(MCPConfig.DEPS_S)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
					libraries.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
				}
				options.addAll(Arrays.asList("-d", MCPConfig.SERVER_BIN));
			} else if (side == CLIENT) {
				try(Stream<Path> stream = Files.list(Paths.get(MCPConfig.DEPS_C)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
					libraries.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
				}
				options.addAll(Arrays.asList("-d", MCPConfig.CLIENT_BIN));
			}

            if (MCP.config.source >= 0) {
                options.addAll(Arrays.asList("-source", Integer.toString(MCP.config.source)));
            }
            if (MCP.config.target >= 0) {
                options.addAll(Arrays.asList("-target", Integer.toString(MCP.config.target)));
            }

            if (MCP.config.bootclasspath != null) {
                options.addAll(Arrays.asList("-bootclasspath", MCP.config.bootclasspath));
            }

            options.addAll(Arrays.asList("-cp", String.join(System.getProperty("path.separator"), libraries)));

			this.progress = 3;
			recompile(compiler, ds, src, options);
			this.progress = 50;
			// Copy assets from source folder
			step();
			List<Path> assets = FileUtil.walkDirectory(srcPath, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".java"));
			int i = 0;
			for(Path path : assets) {
				if(srcPath.relativize(path).getParent() != null) {
					Files.createDirectories(binPath.resolve(srcPath.relativize(path).getParent()));
				}
				Files.copy(path, binPath.resolve(srcPath.relativize(path)));
				i++;
				this.progress = 50 + (int)((double)i / assets.size() * 49);
			}
			this.progress = 99;
		} else {
			throw new IOException(chooseFromSide("Client", "Server") + " sources not found!");
		}
	}

	public void recompile(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> ds, Iterable<File> src, Iterable<String> recompileOptions) throws IOException, RuntimeException {
		StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null);
		Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(src);
		JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, recompileOptions, null, sources);
		mgr.close();
		boolean success = task.call();
		for (Diagnostic<? extends JavaFileObject> diagnostic : ds.getDiagnostics())
			if(diagnostic.getKind() == Diagnostic.Kind.ERROR || diagnostic.getKind() == Diagnostic.Kind.WARNING) {
				String kind = diagnostic.getKind() == Diagnostic.Kind.ERROR ? "Error" : "Warning";
                JavaFileObject source = diagnostic.getSource();
                if (source == null) {
                    info.addInfo(kind + String.format("%n%s%n",
                            diagnostic.getMessage(null)));
                } else {
                    info.addInfo(kind + String.format(" on line %d in %s%n%s%n",
                            diagnostic.getLineNumber(),
                            source.getName(),
                            diagnostic.getMessage(null)));
                }
			}
		mgr.close();
		if (!success) {
			throw new RuntimeException("Compilation error!");
		}
	}

	@Override
	public ProgressInfo getProgress() {
		if(step == RECOMPILE) {
			return new ProgressInfo("Recompiling...", progress, total);
		}
		if(step == COPYRES) {
			return new ProgressInfo("Copying resources...", progress, total);
		}
		return super.getProgress();
	}
}
