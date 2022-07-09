package org.mcphackers.mcp.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.rdi.injector.RDInjector;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.util.IOUtil;
import org.objectweb.asm.ClassReader;

public class TaskReobfuscate extends TaskStaged {

	public TaskReobfuscate(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskReobfuscate(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[] {
			stage(getLocalizedStage("recompile"),
			() -> {
				new TaskRecompile(side, mcp, this).doTask();
			}),
			stage(getLocalizedStage("gathermd5"), 30,
			() -> {
				new TaskUpdateMD5(side, mcp, this).updateMD5(true);
			}),
			stage(getLocalizedStage("reobf"), 52,
			() -> {
				reobfuscate();
			})
		};
	}
		
	
	private void reobfuscate() throws IOException {
		final Path reobfBin = MCPPaths.get(mcp, MCPPaths.BIN_SIDE, side);
		final boolean enableObfuscation = mcp.getOptions().getBooleanParameter(TaskParameter.OBFUSCATION);
		
		Side[] sides = side == Side.MERGED ? new Side[] {Side.CLIENT, Side.SERVER} : new Side[] {side};
		
		Map<String, String> originalHashes = gatherMD5Hashes(false);
		Map<String, String> recompHashes = gatherMD5Hashes(true);

		for(Side localSide : sides) {

			final Path reobfDir = MCPPaths.get(mcp, MCPPaths.REOBF_SIDE, localSide);
			final Path reobfJar = MCPPaths.get(mcp, MCPPaths.REOBF_JAR, localSide);
			List<String> classNames = new ArrayList<>();
			Files.walk(reobfBin).forEach(path -> {
				if(path.getFileName().toString().endsWith(".class")) {
					ClassReader classReader;
					try {
						classReader = new ClassReader(Files.readAllBytes(path));
						classNames.add(classReader.getClassName());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			Mappings mappings = Mappings.read(MCPPaths.get(mcp, MCPPaths.MAPPINGS), "named", localSide.name);
			modifyClassMappings(mappings, classNames, enableObfuscation);
			Files.deleteIfExists(reobfJar);
			RDInjector injector = new RDInjector(reobfBin);
			injector.applyMappings(mappings);
			injector.transform();
			IOUtil.write(injector.getStorage(), Files.newOutputStream(reobfJar));

			Map<String, String> reversedNames = new HashMap<>();
			for(Entry<String, String> entry : mappings.classes.entrySet()) {
				reversedNames.put(entry.getValue(), entry.getKey());
			}
			FileUtil.deleteDirectoryIfExists(reobfDir);
			FileUtil.unzip(reobfJar, reobfDir, entry -> {
				String className = entry.getName().replace(".class", "");
				boolean presentInMappings = true;
				String deobfName = reversedNames.get(className);
				if(deobfName == null) {
					deobfName = className;
					presentInMappings = false;
				}
				String hash			= originalHashes.get(deobfName);
				String hashModified = recompHashes.get(deobfName);
				if(!entry.isDirectory()) {
					if(hash == null) {
						return true;
					}
					else if(!hash.equals(hashModified) && presentInMappings) {
						return true;
					}
				}
				return false;
			});
		}
	}

	private void modifyClassMappings(Mappings mappings, List<String> classNames, boolean obf) {
		Map<String, Integer> obfIndexes = new HashMap<>();
		for(String className : classNames) {
			if (mappings.classes.containsKey(className) /*&& !hashes.containsKey(className)*/) {
				String packageName = className.lastIndexOf("/") >= 0 ? className.substring(0, className.lastIndexOf("/") + 1) : null;
				String obfPackage = mappings.getPackageName(packageName);
				if (obfPackage == null) {
					break;
				}
				String clsName = (className.lastIndexOf("/") >= 0 ? className.substring(className.lastIndexOf("/") + 1) : className);
				if(obf) {
					int obfIndex = obfIndexes.getOrDefault(obfPackage, 0);
					String obfName = MappingUtil.getObfuscatedName(obfIndex);
					List<String> obfNames = new ArrayList<>();
					for(Entry<String, String> entry : mappings.classes.entrySet()) {
						obfNames.add(entry.getValue());
					}
					while(obfNames.contains(obfPackage + obfName)) {
						obfIndex++;
						obfName = MappingUtil.getObfuscatedName(obfIndex);
					}
					if(obfIndex > obfIndexes.getOrDefault(obfPackage, 0)) {
						obfIndexes.put(obfPackage, obfIndex);
					}
					clsName = obfName;
				}
				mappings.classes.put(className, obfPackage + clsName);
			}
		}
	}
	
	public void setProgress(int progress) {
		switch (step) {
		case 0: {
			int percent = (int)((double)progress * 0.5D);
			super.setProgress(1 + percent);
			break;
		}
		case 1: {
			int percent = (int)((double)progress * 0.22D);
			super.setProgress(1 + percent);
			break;
		}
		default:
			super.setProgress(progress);
			break;
		}
	}

	private Map<String, String> gatherMD5Hashes(boolean reobf) throws IOException {
		final Path md5 = MCPPaths.get(mcp, reobf ? MCPPaths.MD5_RO : MCPPaths.MD5, side);
		Map<String, String> hashes = new HashMap<>();

		try (BufferedReader reader = Files.newBufferedReader(md5)) {
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				String[] tokens = line.split(" ");
				hashes.put(tokens[0], tokens[1]);
			}
		}
		return hashes;
	}
}
