package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.providers.IJavadocProvider;
import de.fernflower.struct.StructClass;
import de.fernflower.struct.StructField;
import de.fernflower.struct.StructMethod;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TinyJavadocProvider implements IJavadocProvider {
	private final MappingTree mappingTree;

	public TinyJavadocProvider(File tinyFile) {
		mappingTree = readMappings(tinyFile);
	}

	@Override
	public String getClassDoc(StructClass structClass) {
		MappingTree.ClassMapping classMapping = mappingTree.getClass(structClass.qualifiedName);

		if (classMapping == null) {
			return null;
		}

		if (!isRecord(structClass)) {
			return classMapping.getComment();
		} else {
			// A RECORD??? IN MY JAVA 8???
			return null;
		}
	}

	@Override
	public String getFieldDoc(StructClass structClass, StructField structField) {
		// None static fields in records are handled in the class javadoc.
		if (isRecord(structClass) && !isStatic(structField)) {
			return null;
		}

		MappingTree.ClassMapping classMapping = mappingTree.getClass(structClass.qualifiedName);

		if (classMapping == null) {
			return null;
		}

		MappingTree.FieldMapping fieldMapping = classMapping.getField(structField.getName(), structField.getDescriptor());

		return fieldMapping != null ? fieldMapping.getComment() : null;
	}

	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		MappingTree.ClassMapping classMapping = mappingTree.getClass(structClass.qualifiedName);

		if (classMapping == null) {
			return null;
		}

		MappingTree.MethodMapping methodMapping = classMapping.getMethod(structMethod.getName(), structMethod.getDescriptor());

		if (methodMapping != null) {
			List<String> parts = new ArrayList<>();

			if (methodMapping.getComment() != null) {
				parts.add(methodMapping.getComment());
			}

			boolean addedParam = false;

			for (MappingTree.MethodArgMapping argMapping : methodMapping.getArgs()) {
				String comment = argMapping.getComment();

				if (comment != null) {
					if (!addedParam && methodMapping.getComment() != null) {
						//Add a blank line before params when the method has a comment
						parts.add("");
						addedParam = true;
					}

					parts.add(String.format("@param %s %s", argMapping.getName("named"), comment));
				}
			}

			if (parts.isEmpty()) {
				return null;
			}

			return String.join("\n", parts);
		}

		return null;
	}

	private static MappingTree readMappings(File input) throws RuntimeException {
		try (BufferedReader reader = Files.newBufferedReader(input.toPath())) {
			MemoryMappingTree mappingTree = new MemoryMappingTree();
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(mappingTree, "named");
			MappingReader.read(reader, nsSwitch);

			return mappingTree;
		} catch (IOException e) {
			throw new RuntimeException("Failed to read mappings", e);
		}
	}

	public static boolean isRecord(StructClass structClass) {
		return (structClass.getAccessFlags() & Opcodes.ACC_RECORD) != 0;
	}

	public static boolean isStatic(StructField structField) {
		return (structField.getAccessFlags() & Opcodes.ACC_STATIC) != 0;
	}
}
