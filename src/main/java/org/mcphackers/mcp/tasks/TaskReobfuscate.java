package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.injector.SourceFileTransformer;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.nio.ClassStorageWriter;
import org.mcphackers.rdi.nio.MappingsIO;
import org.mcphackers.rdi.nio.RDInjector;
import org.objectweb.asm.ClassWriter;

public class TaskReobfuscate extends TaskStaged {

	public TaskReobfuscate(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskReobfuscate(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	private static Map<String, String> getPackageMappings(Map<String, String> classMappings) {
		Map<String, String> packageMappings = new HashMap<>();
		for (Entry<String, String> entry : classMappings.entrySet()) {
			int i1 = entry.getKey().lastIndexOf('/');
			int i2 = entry.getValue().lastIndexOf('/');
			String name1 = i1 == -1 ? "" : entry.getKey().substring(0, i1 + 1);
			String name2 = i2 == -1 ? "" : entry.getKey().substring(0, i2 + 1);
			packageMappings.put(name1, name2);
		}
		return packageMappings;
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[]{
				stage(getLocalizedStage("gathermd5"), 0,
						() -> new TaskUpdateMD5(side, mcp, this).updateMD5(true)),
				stage(getLocalizedStage("reobf"), 50,
						this::reobfuscate)
		};
	}

	private void reobfuscate() throws IOException {
		final Path reobfBin = MCPPaths.get(mcp, BIN, side);
		final boolean stripSourceFile = mcp.getOptions().getBooleanParameter(TaskParameter.STRIP_SOURCE_FILE);

		Side[] sides = side == Side.MERGED ? new Side[]{Side.CLIENT, Side.SERVER} : new Side[]{side};

		Map<String, String> originalHashes = gatherMD5Hashes(false);
		Map<String, String> recompHashes = gatherMD5Hashes(true);

		for (Side localSide : sides) {
			final Path reobfDir = MCPPaths.get(mcp, REOBF_SIDE, localSide);
			if (!Files.exists(reobfDir)) {
				Files.createDirectories(reobfDir);
			}
			final Path reobfJar = MCPPaths.get(mcp, REOBF_JAR, localSide);
			if (!Files.exists(reobfJar.getParent())) {
				Files.createDirectories(reobfJar.getParent());
			}
			Files.deleteIfExists(reobfJar);
			RDInjector injector = new RDInjector(reobfBin);
			Mappings mappings = getMappings(injector.getStorage(), localSide);
			if (mappings != null) {
				injector.applyMappings(mappings);
			}
			if (stripSourceFile) {
				injector.addTransform(SourceFileTransformer::removeSourceFileAttributes);
			}
			injector.transform();
			new ClassStorageWriter(injector.getStorage(), ClassWriter.COMPUTE_MAXS).write(Files.newOutputStream(reobfJar));

			Map<String, String> reversedNames = new HashMap<>();
			if (mappings != null) {
				for (Entry<String, String> entry : mappings.classes.entrySet()) {
					reversedNames.put(entry.getValue(), entry.getKey());
				}
			}
			FileUtil.cleanDirectory(reobfDir);
			Pattern regexPattern = Pattern.compile(mcp.getOptions().getStringParameter(TaskParameter.EXCLUDED_CLASSES));
			FileUtil.extract(reobfJar, reobfDir, entry -> {
				if (entry.isDirectory()) {
					return false;
				}
				String obfClassName = entry.getName().replace(".class", "");
				// Force inner classes to compare outer class hash
				String className = obfClassName;
				int index = className.indexOf('$');
				if (index != -1) {
					className = className.substring(0, index);
				}
				String deobfName = reversedNames.get(className);
				if (deobfName == null) {
					deobfName = className;
				}
				String hash = originalHashes.get(deobfName);
				String hashModified = recompHashes.get(deobfName);
				boolean extract = (hash == null) || !hash.equals(hashModified) && !regexPattern.matcher(deobfName).matches();
				if (extract) {
					System.out.println(reversedNames.get(obfClassName) + " : " + obfClassName);
				}
				return extract;
			});
		}
	}

	private Mappings getMappings(ClassStorage storage, Side side) throws IOException {
		Path mappingsPath = MCPPaths.get(mcp, MAPPINGS);
		if (!Files.exists(mappingsPath)) {
			return new Mappings();
		}
		final boolean enableObfuscation = mcp.getOptions().getBooleanParameter(TaskParameter.OBFUSCATION);
		final boolean srgObfuscation = mcp.getOptions().getBooleanParameter(TaskParameter.SRG_OBFUSCATION);
		List<String> nss = MappingUtil.readNamespaces(mappingsPath);
		boolean joined = srgObfuscation ? nss.contains("searge") : nss.contains("official");
		Mappings mappings = MappingsIO.read(mappingsPath, "named",
				joined ? (srgObfuscation ? "searge" : "official") : side.name);
		modifyClassMappings(mappings, storage.getAllClasses(), enableObfuscation);
		return mappings;
	}

	private void modifyClassMappings(Mappings mappings, List<String> classNames, boolean obf) {
		Map<String, Integer> obfIndexes = new HashMap<>();
		Map<String, String> packageMappings = getPackageMappings(mappings.classes);
		for (String className : classNames) {
			String reobfName = mappings.classes.get(className);
			if (reobfName == null /*&& !hashes.containsKey(className)*/) {
				int i1 = className.lastIndexOf('/');
				String packageName = i1 == -1 ? "" : className.substring(0, i1 + 1);
				String obfPackage = packageMappings.get(packageName);
				String clsName = i1 == -1 ? className : className.substring(i1 + 1);
				if (obf) {
					int obfIndex = obfIndexes.getOrDefault(obfPackage, 0);
					String obfName = MappingUtil.getObfuscatedName(obfIndex);
					List<String> obfNames = new ArrayList<>();
					for (Entry<String, String> entry : mappings.classes.entrySet()) {
						obfNames.add(entry.getValue());
					}
					while (obfNames.contains(obfPackage + obfName)) {
						obfIndex++;
						obfName = MappingUtil.getObfuscatedName(obfIndex);
					}
					if (obfIndex > obfIndexes.getOrDefault(obfPackage, 0)) {
						obfIndexes.put(obfPackage, obfIndex);
					}
					clsName = obfName;
				}
				if (obf || obfPackage != null) {
					String className2 = (obfPackage == null ? packageName : obfPackage) + clsName;
					mappings.classes.put(className, className2);
				}
			}
		}
	}

	@Override
	public void setProgress(int progress) {
		if (step == 0) {
			int percent = (int) (progress * 0.42D);
			super.setProgress(1 + percent);
		} else {
			super.setProgress(progress);
		}
	}

	private Map<String, String> gatherMD5Hashes(boolean reobf) throws IOException {
		final Path md5 = MCPPaths.get(mcp, reobf ? MCPPaths.MD5_RO : MCPPaths.MD5, side);
		Map<String, String> hashes = new HashMap<>();

		try (Stream<String> lines = Files.lines(md5)) {
			lines.forEach((line) -> {
				String[] tokens = line.split(" ");
				hashes.put(tokens[0], tokens[1]);
			});
		}
		return hashes;
	}
}
