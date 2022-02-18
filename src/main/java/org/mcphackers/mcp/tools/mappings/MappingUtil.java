package org.mcphackers.mcp.tools.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

public class MappingUtil {

	private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

	public static void readMappings(Path mappings, MemoryMappingTree mappingTree) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(mappings)) {
			Tiny2Reader.read(reader, mappingTree);
		}
	}

	public static void writeMappings(Path mappings, MemoryMappingTree mappingTree) throws IOException {
		try (Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(mappings), false)) {
			mappingTree.accept(writer);
		}
	}
	
	public static void modifyMappings(MemoryMappingTree mappingTree, Path classPath, Function<String, String> getDstName) throws IOException {
		do {
			if (mappingTree.visitHeader()) mappingTree.visitNamespaces(mappingTree.getSrcNamespace(), mappingTree.getDstNamespaces());

			if (mappingTree.visitContent()) {
				Files.walkFileTree(classPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toString().endsWith(".class")) {
							ClassReader classReader = new ClassReader(Files.readAllBytes(file));
							String className = classReader.getClassName();
							if (mappingTree.visitClass(className)) {
								String dstName = getDstName.apply(className);
								if(dstName != null) {
									mappingTree.visitDstName(MappedElementKind.CLASS, 0, dstName);
								}
							}
						}
						return super.visitFile(file, attrs);
					}
				});
			}
		} while (!mappingTree.visitEnd());
	}
	
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
