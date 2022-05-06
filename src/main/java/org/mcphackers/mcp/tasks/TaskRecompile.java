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
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;

public class TaskRecompile extends TaskStaged {
	
	public TaskRecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskRecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	protected Stage[] setStages() {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if(compiler == null) {
			throw new RuntimeException("Could not find compiling API");
		}
		Path binPath = MCPPaths.get(mcp, MCPPaths.BIN_SIDE, side);
		Path srcPath = MCPPaths.get(mcp, MCPPaths.SOURCES, side);
		return new Stage[] {
			stage("Recompiling", 1,
			() -> {
				FileUtil.deleteDirectoryIfExists(binPath);
				Files.createDirectories(binPath);
				setProgress(2);
				if (!Files.exists(srcPath)) {
					throw new IOException(side.name + " sources not found!");
				}

				final List<File> src = collectSource();
				final List<Path> classpath = collectClassPath();
				final List<Path> bootclasspath = collectBootClassPath();

				List<String> cp = new ArrayList<>();
				classpath.forEach(p -> cp.add(p.toAbsolutePath().toString()));

				List<String> options = new ArrayList<>(Arrays.asList("-d", binPath.toString()));

				int sourceVersion = mcp.getOptions().getIntParameter(TaskParameter.SOURCE_VERSION);
				if (sourceVersion >= 0) {
					options.addAll(Arrays.asList("-source", Integer.toString(sourceVersion)));
				}

				int targetVersion = mcp.getOptions().getIntParameter(TaskParameter.TARGET_VERSION);
				if (targetVersion >= 0) {
					options.addAll(Arrays.asList("-target", Integer.toString(targetVersion)));
				}

				List<String> bootcp = new ArrayList<>();
				bootclasspath.forEach(p -> bootcp.add(p.toAbsolutePath().toString()));
				if (bootclasspath.size() > 0) {
					options.addAll(Arrays.asList("-bootclasspath", String.join(System.getProperty("path.separator"), bootcp)));
				}

				options.addAll(Arrays.asList("-cp", String.join(System.getProperty("path.separator"), cp)));

				setProgress(3);

				DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();
				recompile(compiler, ds, src, options);
			}),
			stage("Copying resources", 50,
			() -> {
				// Copy assets from source folder
				List<Path> assets = collectResources();
				int i = 0;
				for(Path path : assets) {
					if(srcPath.relativize(path).getParent() != null) {
						Files.createDirectories(binPath.resolve(srcPath.relativize(path).getParent()));
					}
					Files.copy(path, binPath.resolve(srcPath.relativize(path)));
					i++;
					setProgress(50 + (int)((double)i / assets.size() * 49));
				}
			})
		};
	}
	
	public List<Path> collectResources() throws IOException {
		Path srcPath = MCPPaths.get(mcp, MCPPaths.SOURCES, side);
		return FileUtil.walkDirectory(srcPath, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".java") && !path.getFileName().toString().endsWith(".class"));
	}
	
	public List<Path> collectClassPath() throws IOException {
		List<Path> classpath = new ArrayList<>();
		if(side == Side.SERVER) {
			classpath.add(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
		}
		Side[] sides = (side == Side.MERGED) ? new Side[] {Side.CLIENT, Side.SERVER} : new Side[] {side};
		for(Side sideLocal : sides) {
			final Path libPath = MCPPaths.get(mcp, MCPPaths.LIBS, sideLocal);
			if(Files.exists(libPath)) {
				FileUtil.collectJars(libPath, classpath);
			}
		}
		return classpath;
	}
	
	public List<Path> collectBootClassPath() throws IOException {
		List<Path> bootclasspath = new ArrayList<>();
		String javaHome = mcp.getOptions().getStringParameter(TaskParameter.JAVA_HOME);
		if(!javaHome.equals("")) {
			Path libs = Paths.get(javaHome).resolve("lib");
			Path libsJre = Paths.get(javaHome).resolve("jre/lib");
			if(Files.exists(libs)) {
				FileUtil.collectJars(libs, bootclasspath);
			}
			if(Files.exists(libsJre)) {
				FileUtil.collectJars(libsJre, bootclasspath);
			}
		}
		return bootclasspath;
	}
	
	public List<File> collectSource() throws IOException {
		Path srcPath = MCPPaths.get(mcp, MCPPaths.SOURCES, side);
		List<File> src;
		try(Stream<Path> pathStream = Files.walk(srcPath)) {
			src = pathStream.filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
		}
		if(side == Side.CLIENT || side == Side.MERGED) {
			List<File> start;
			try(Stream<Path> pathStream = Files.walk(MCPPaths.get(mcp, MCPPaths.CONF + "start"))) {
				start = pathStream.filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
			}
			src.addAll(start);
		}
		return src;
	}

	public void recompile(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> ds, Iterable<File> src, Iterable<String> recompileOptions) throws IOException, RuntimeException {
		StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null);
		Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(src);
		JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, recompileOptions, null, sources);
		mgr.close();
		boolean success = task.call();
		for (Diagnostic<? extends JavaFileObject> diagnostic : ds.getDiagnostics())
			if(diagnostic.getKind() == Diagnostic.Kind.ERROR || diagnostic.getKind() == Diagnostic.Kind.WARNING) {
				String[] kindString = {"Info", "Warning", "Error"};
				byte kind = (byte) (diagnostic.getKind() == Diagnostic.Kind.ERROR ? Task.ERROR : Task.WARNING);
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
	}
}
