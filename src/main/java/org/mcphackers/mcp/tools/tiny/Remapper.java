package org.mcphackers.mcp.tools.tiny;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

public class Remapper {

	private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");
	
	public static void remap(Path mappings, Path input, Path output, Path... cp) throws IOException {
		remap(mappings, input, output, false, cp);
	}
	
	public static void remap(Path mappings, Path input, Path output, boolean deobf, Path... cp) throws IOException {
		TinyRemapper remapper = null;
		String[] names = new String[] {"official", "named"};
		
		if(!deobf) {
			names = new String[] {"named", "official"};
		}

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build()) {
			remapper = applyMappings(TinyUtils.createTinyMappingProvider(mappings, names[0], names[1]), input, outputConsumer, cp);
			if(deobf) outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);
		} finally {
			if (remapper != null) {
				remapper.finish();
			}
		}
	}

	private static TinyRemapper applyMappings(IMappingProvider mappings, Path input, BiConsumer<String, byte[]> consumer, Path... classpath) {
		TinyRemapper remapper = TinyRemapper.newRemapper()
				.renameInvalidLocals(false)
				.rebuildSourceFilenames(true)
				.invalidLvNamePattern(MC_LV_PATTERN)
				.withMappings(mappings)
				.fixPackageAccess(false)
				.threads(Runtime.getRuntime().availableProcessors() - 3)
				.rebuildSourceFilenames(true)
				.build();

		remapper.readClassPath(classpath);
		remapper.readInputs(input);
		remapper.apply(consumer);

		return remapper;
	}

}
