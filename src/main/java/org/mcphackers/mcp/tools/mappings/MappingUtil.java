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

import org.mcphackers.mcp.TriFunction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
	public static void modifyClasses(MemoryMappingTree mappingTree, Path classPath, Function<String, String> getDstName) throws IOException {
		modifyMappings(mappingTree, classPath, MappedElementKind.CLASS, getDstName);
	}
	
	public static void modifyFields(MemoryMappingTree mappingTree, Path classPath, TriFunction<String, String, String, String> getDstName) throws IOException {
		modifyMappings(mappingTree, classPath, MappedElementKind.FIELD, getDstName);
	}
	
	public static void modifyMethods(MemoryMappingTree mappingTree, Path classPath, TriFunction<String, String, String, String> getDstName) throws IOException {
		modifyMappings(mappingTree, classPath, MappedElementKind.METHOD, getDstName);
	}
	
	private static void modifyMappings(MemoryMappingTree mappingTree, Path classPath, MappedElementKind kind, Object getDstName) throws IOException {
		do {
			if (mappingTree.visitHeader()) mappingTree.visitNamespaces(mappingTree.getSrcNamespace(), mappingTree.getDstNamespaces());

			if (mappingTree.visitContent()) {
				Files.walkFileTree(classPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toString().endsWith(".class")) {
							ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
								private String currentClass;
								
								@Override
								public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {	
									currentClass = name;
									if (kind == MappedElementKind.CLASS) {
										String dstName = ((Function<String, String>)getDstName).apply(name);
										if(dstName != null) {
											if (mappingTree.visitClass(name)) {
												mappingTree.visitDstName(MappedElementKind.CLASS, 0, dstName);
											}
										}
									}
									super.visit(version, access, name, signature, superName, interfaces);
								}

								@Override
								public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
									if (kind == MappedElementKind.FIELD) {
										String dstName = ((TriFunction<String, String, String, String>)getDstName).apply(currentClass, name, descriptor);
										if(dstName != null) {
											mappingTree.visitClass(currentClass);
											if (mappingTree.visitField(name, descriptor)) {
												mappingTree.visitDstName(MappedElementKind.FIELD, 0, dstName);
											}
										}
									}
									return super.visitField(access, name, descriptor, signature, value);
								}

								@Override
								public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
									if (kind == MappedElementKind.METHOD) {
										String dstName = ((TriFunction<String, String, String, String>)getDstName).apply(currentClass, name, descriptor);
										if(dstName != null) {
											mappingTree.visitClass(currentClass);
											if (mappingTree.visitMethod(name, descriptor)) {
												mappingTree.visitDstName(MappedElementKind.METHOD, 0, dstName);
											}
										}
									}
									return super.visitMethod(access, name, descriptor, signature, exceptions);
								}
							};
							ClassReader reader = new ClassReader(Files.readAllBytes(file));
							reader.accept(visitor, 0);
						}
						return super.visitFile(file, attrs);
					}
				});
			}
		} while (!mappingTree.visitEnd());
	}
	
	public static void remap(Path mappings, Path input, Path output, Path[] cp, String srcNamespace, String dstNamespace) throws IOException {
		TinyRemapper remapper = null;

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build()) {
			remapper = applyMappings(TinyUtils.createTinyMappingProvider(mappings, srcNamespace, dstNamespace), input, outputConsumer, cp);
			outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);
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
