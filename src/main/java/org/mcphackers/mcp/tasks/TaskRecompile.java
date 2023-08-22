package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;

import javax.tools.*;

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

import static org.mcphackers.mcp.MCPPaths.*;

public class TaskRecompile extends TaskStaged {

	public TaskRecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskRecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	public static List<Path> collectClassPath(MCP mcp, Side side) {
		List<Path> classpath = new ArrayList<>();
		classpath.add(MCPPaths.get(mcp, REMAPPED, side));
		if (mcp.getCurrentVersion() != null) {
			classpath.addAll(mcp.getLibraries());
		}
		return classpath;
	}

	@Override
	protected Stage[] setStages() {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new RuntimeException("Could not find compiling API");
		}
		Path binPath = MCPPaths.get(mcp, BIN, side);
		Path srcPath = MCPPaths.get(mcp, SOURCE, side);
		return new Stage[]{
				stage(getLocalizedStage("recompile"), 1,
						() -> {
							Files.createDirectories(binPath);
							FileUtil.cleanDirectory(binPath);
							setProgress(2);
							if (!Files.exists(srcPath)) {
								throw new IOException(side.getName() + " sources not found!");
							}
							try (Stream<Path> paths = Files.list(srcPath)) {
								if (!paths.findAny().isPresent()) {
									return;
								}
							}

							final List<File> src = collectSource();
							final List<Path> classpath = collectClassPath(mcp, side);
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
							if (!bootclasspath.isEmpty()) {
								options.addAll(Arrays.asList("-bootclasspath", String.join(System.getProperty("path.separator"), bootcp)));
							}

							options.addAll(Arrays.asList("-cp", String.join(System.getProperty("path.separator"), cp)));

							setProgress(3);

							DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();
							recompile(compiler, ds, src, options);
						}),
				stage(getLocalizedStage("copyres"), 50,
						() -> {
							// Copy assets from source folder
							List<Path> assets = collectResources();
							int i = 0;
							for (Path path : assets) {
								if (srcPath.relativize(path).getParent() != null) {
									Files.createDirectories(binPath.resolve(srcPath.relativize(path).getParent()));
								}
								Files.copy(path, binPath.resolve(srcPath.relativize(path)));
								i++;
								setProgress(50 + (int) ((double) i / assets.size() * 49));
							}
						})
		};
	}

	public List<Path> collectResources() throws IOException {
		Path srcPath = MCPPaths.get(mcp, SOURCE, side);
		return FileUtil.walkDirectory(srcPath, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".java") && !path.getFileName().toString().endsWith(".class"));
	}

	public List<Path> collectBootClassPath() throws IOException {
		List<Path> bootclasspath = new ArrayList<>();
		String javaHome = mcp.getOptions().getStringParameter(TaskParameter.JAVA_HOME);
		if (!javaHome.isEmpty()) {
			Path libs = Paths.get(javaHome).resolve("lib");
			Path libsJre = Paths.get(javaHome).resolve("jre/lib");
			if (Files.exists(libs)) {
				FileUtil.collectJars(libs, bootclasspath);
			}
			if (Files.exists(libsJre)) {
				FileUtil.collectJars(libsJre, bootclasspath);
			}
		}
		return bootclasspath;
	}

	public List<File> collectSource() throws IOException {
		Path srcPath = MCPPaths.get(mcp, SOURCE, side);
		List<File> src;
		try (Stream<Path> pathStream = Files.walk(srcPath)) {
			src = pathStream.filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
		}
		return src;
	}

	public void recompile(JavaCompiler compiler, DiagnosticCollector<JavaFileObject> ds, Iterable<File> src, Iterable<String> recompileOptions) throws IOException, RuntimeException {
		StandardJavaFileManager mgr = compiler.getStandardFileManager(ds, null, null);
		Iterable<? extends JavaFileObject> sources = mgr.getJavaFileObjectsFromFiles(src);
		JavaCompiler.CompilationTask task = compiler.getTask(null, mgr, ds, recompileOptions, null, sources);
		mgr.close();
		task.call();
		for (Diagnostic<? extends JavaFileObject> diagnostic : ds.getDiagnostics())
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR || diagnostic.getKind() == Diagnostic.Kind.WARNING) {
				String[] kindString = {"Info", "Warning", "Error"};
				byte kind = diagnostic.getKind() == Diagnostic.Kind.ERROR ? Task.ERROR : Task.WARNING;
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
