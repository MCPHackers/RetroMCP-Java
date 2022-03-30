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
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.ProgressListener;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;

public class TaskRecompile extends Task {
	
	private static final int RECOMPILE = 1;
	private static final int COPYRES = 2;
	
	public TaskRecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskRecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	public void doTask() throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			throw new RuntimeException("Could not find compiling API");
		}
		DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();

		Path binPath = Paths.get(chooseFromSide(MCPPaths.CLIENT_BIN, 		MCPPaths.SERVER_BIN));
		Path srcPath = Paths.get(chooseFromSide(MCPPaths.CLIENT_SOURCES, 	MCPPaths.SERVER_SOURCES));

		step();
		FileUtil.deleteDirectoryIfExists(binPath);
		Files.createDirectories(binPath);
		setProgress(2);

		// Compile side
		if (Files.exists(srcPath)) {
			List<File> src = Files.walk(srcPath).filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
			List<File> start = Files.walk(Paths.get(MCPPaths.CONF + "start")).filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
			if(side == Side.CLIENT) {
				src.addAll(start);
			}

			List<String> libraries;
			List<String> options = new ArrayList<>();

			try(Stream<Path> stream = Files.list(Paths.get(MCPPaths.LIB)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				libraries = stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList());
			}

			if(side == Side.SERVER) {
				libraries.add(0, MCPPaths.SERVER);
				try(Stream<Path> stream = Files.list(Paths.get(MCPPaths.DEPS_S)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
					libraries.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
				}
				options.addAll(Arrays.asList("-d", MCPPaths.SERVER_BIN));
			} else if(side == Side.CLIENT) {
				try(Stream<Path> stream = Files.list(Paths.get(MCPPaths.DEPS_C)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
					libraries.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
				}
				options.addAll(Arrays.asList("-d", MCPPaths.CLIENT_BIN));
			}

			int sourceVersion = mcp.getIntParam(TaskParameter.SOURCE_VERSION);
			if (sourceVersion >= 0) {
				options.addAll(Arrays.asList("-source", Integer.toString(sourceVersion)));
			}

			int targetVersion = mcp.getIntParam(TaskParameter.TARGET_VERSION);
			if (targetVersion >= 0) {
				options.addAll(Arrays.asList("-target", Integer.toString(targetVersion)));
			}

			String bootclasspath = mcp.getStringParam(TaskParameter.BOOT_CLASS_PATH);
			if (bootclasspath != null) {
				options.addAll(Arrays.asList("-bootclasspath", bootclasspath));
			}

			options.addAll(Arrays.asList("-cp", String.join(System.getProperty("path.separator"), libraries)));

			setProgress(3);
			recompile(compiler, ds, src, options);
			setProgress(50);
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
				setProgress(50 + (int)((double)i / assets.size() * 49));
			}
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
				String[] kindString = new String[] {"Info", "Warning", "Error"};
				byte kind = (byte) (diagnostic.getKind() == Diagnostic.Kind.ERROR ? 2 : 1);
				JavaFileObject source = diagnostic.getSource();
				if (source == null) {
					addMessage(kindString[kind] + String.format("%n%s%n",
							diagnostic.getMessage(null)),
							kind);
				} else {
					addMessage(kindString[kind] + String.format(" on line %d in %s%n%s%n",
							diagnostic.getLineNumber(),
							source.getName(),
							diagnostic.getMessage(null)),
							kind);
				}
			
			}
		mgr.close();
		if (!success) {
			throw new RuntimeException("Compilation error!");
		}
	}

	@Override
	public String getName() {
		return "Recompile";
	}

	protected void updateProgress() {
		switch (step) {
		case RECOMPILE:
			setProgress("Recompiling...", 1);
			break;
		case COPYRES:
			setProgress("Copying resources...");
			break;
		default:
			super.updateProgress();
			break;
		}
	}
}
