package org.mcphackers.mcp.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MappingData;
import org.mcphackers.mcp.ProgressListener;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.MCPPaths;
import org.mcphackers.mcp.tools.TriFunction;
import org.mcphackers.mcp.tools.mappings.MappingUtil;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

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
		stage("Recompiling",
		() -> {
			new TaskRecompile(side, mcp, this).doTask();
		}),
		stage("Gathering MD5 hashes", 30,
		() -> {
			new TaskUpdateMD5(side, mcp, this).updateMD5(true);
		}),
		stage("Reobfuscating", 52,
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

		Set<MappingData> mappings = new HashSet<>();
		for(Side localSide : sides) {
			final Path deobfMappings = MCPPaths.get(mcp, MCPPaths.MAPPINGS_DO, localSide);
			final Path reobfJar = MCPPaths.get(mcp, MCPPaths.REOBF_JAR, localSide);
			if(Files.exists(deobfMappings)) {
				MappingData mappingData = new MappingData(localSide, deobfMappings);
				mappingData.flipMappingTree();
				mappings.add(mappingData);
			}
			else {
				Files.deleteIfExists(reobfJar);
				FileUtil.compress(reobfBin, reobfJar);
			}
		}
		for(MappingData mappingData : mappings) {
			Side localSide = mappingData.getSide();
			final Path reobfMappings = MCPPaths.get(mcp, MCPPaths.MAPPINGS_RO, localSide);
			final Path reobfDir = MCPPaths.get(mcp, MCPPaths.REOBF_SIDE, localSide);
			final Path reobfJar = MCPPaths.get(mcp, MCPPaths.REOBF_JAR, localSide);
			MappingUtil.modifyClasses(mappingData.getTree(), reobfBin, getClassPattern(mappingData, enableObfuscation, originalHashes));
			//MappingUtil.modifyFields(mappingData.getTree(), reobfBin, getFieldPattern(mappingData, enableObfuscation, originalHashes));
			mappingData.save(reobfMappings);
			
			Files.deleteIfExists(reobfJar);
			MappingUtil.remap(reobfMappings, reobfBin, reobfJar, TaskDecompile.getLibraryPaths(mcp, localSide), "named", "official");
			
			FileUtil.deleteDirectoryIfExists(reobfDir);
			FileUtil.unzip(reobfJar, reobfDir, entry -> {
				String className = entry.getName().replace(".class", "");
				ClassMapping cls = mappingData.getTree().getClass(className);
				String deobfName = cls == null ? className : cls.getDstName(0);
				String hash			= originalHashes.get(deobfName);
				String hashModified = recompHashes.get(deobfName);
				if(!entry.isDirectory()) {
					if(hash == null) {
						return true;
					}
					else if(!hash.equals(hashModified)) {
						return true;
					}
				}
				return false;
			});
		}
	}

	private Function<String, String> getClassPattern(MappingData mappingData, boolean obf, Map<String, String> hashes) {
		Map<String, Integer> obfIndexes = new HashMap<>();
		MappingTree mappingTree = mappingData.getTree();
		return className -> {
			if (mappingTree.getClass(className) == null && !hashes.containsKey(className)) {
				String packageName = className.lastIndexOf("/") >= 0 ? className.substring(0, className.lastIndexOf("/") + 1) : null;
				String obfPackage = mappingData.packages.get(packageName);
				if (obfPackage == null) {
					return null;
				}
				String clsName = (className.lastIndexOf("/") >= 0 ? className.substring(className.lastIndexOf("/") + 1) : className);
				if(obf) {
					int obfIndex = obfIndexes.getOrDefault(obfPackage, 0);
					String obfName = MappingUtil.getObfuscatedName(obfIndex);
					while(mappingTree.getClass(obfPackage + obfName, 0) != null) {
						obfIndex++;
						obfName = MappingUtil.getObfuscatedName(obfIndex);
					}
					if(obfIndex > obfIndexes.getOrDefault(obfPackage, 0)) {
						obfIndexes.put(obfPackage, obfIndex);
					}
					clsName = obfName;
				}
				return obfPackage + clsName;
			}
			return null; // Returning null skips remapping
		};
	}

	private TriFunction<String, String, String, String> getFieldPattern(MappingData mappingData, boolean obf, Map<String, String> hashes) {
		// TODO It still doesn't work
		MappingTree mappingTree = mappingData.getTree();
		return (className, name, desc) -> {
			if (mappingTree.getField(className, name, desc) == null && !hashes.containsKey(className)) {
				if(obf) {
					int obfIndex = 0;
					String obfName = MappingUtil.getObfuscatedName(obfIndex);
					ClassMapping c = ((MappingTree)mappingTree).getClass(className);
					String fName = c == null ? className : c.getDstName(0);
					while(mappingTree.getField(fName, obfName, desc, 0) != null) {
						obfIndex++;
						obfName = MappingUtil.getObfuscatedName(obfIndex);
					}
					return obfName;
				}
			}
			return null;
		};
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
