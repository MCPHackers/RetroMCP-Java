package org.mcphackers.mcp.tools.javadoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * Loads Javadoc comments from a Tiny mapping file and exposes them keyed by the
 * "named" namespace identifiers used in decompiled source.
 *
 * <p>Tiny v2 supports comments on classes, fields and methods. This class only
 * surfaces class and field comments. Methods are intentionally skipped because
 * matching overloads from regenerated source is not reliable enough without a
 * full Java parser.
 */
public final class JavadocMappings {

	/** Class internal name (e.g. {@code net/minecraft/src/Block}) -&gt; Javadoc text. */
	private final Map<String, String> classDocs;

	/** {@code internal/Class.fieldName} -&gt; Javadoc text. */
	private final Map<String, String> fieldDocs;

	private JavadocMappings(Map<String, String> classDocs, Map<String, String> fieldDocs) {
		this.classDocs = classDocs;
		this.fieldDocs = fieldDocs;
	}

	public static JavadocMappings empty() {
		return new JavadocMappings(Collections.emptyMap(), Collections.emptyMap());
	}

	public boolean isEmpty() {
		return classDocs.isEmpty() && fieldDocs.isEmpty();
	}

	public String getClassDoc(String namedInternal) {
		return classDocs.get(namedInternal);
	}

	public String getFieldDoc(String namedInternal, String fieldName) {
		return fieldDocs.get(namedInternal + "." + fieldName);
	}

	/**
	 * Reads a Tiny mapping file and collects every Javadoc comment that targets
	 * the {@code named} namespace.
	 *
	 * @param mappingFile path to a Tiny v1 or v2 mapping file
	 * @return loaded mappings, or {@link #empty()} if the file does not exist
	 * @throws IOException if the file cannot be read
	 */
	public static JavadocMappings read(Path mappingFile) throws IOException {
		if (mappingFile == null || !Files.exists(mappingFile)) {
			return empty();
		}

		MemoryMappingTree tree = new MemoryMappingTree();
		try (BufferedReader reader = Files.newBufferedReader(mappingFile)) {
			MappingReader.read(reader, new MappingSourceNsSwitch(tree, "named"));
		}

		Map<String, String> classDocs = new HashMap<>();
		Map<String, String> fieldDocs = new HashMap<>();
		Set<String> ambiguousFields = new HashSet<>();

		for (MappingTree.ClassMapping cls : tree.getClasses()) {
			String namedClass = cls.getName("named");
			if (namedClass == null) {
				namedClass = cls.getSrcName();
			}
			if (namedClass == null) {
				continue;
			}

			String classComment = cls.getComment();
			if (classComment != null && !classComment.isEmpty()) {
				classDocs.put(namedClass, classComment);
			}

			for (MappingTree.FieldMapping field : cls.getFields()) {
				String fieldComment = field.getComment();
				if (fieldComment == null || fieldComment.isEmpty()) {
					continue;
				}
				String namedField = field.getName("named");
				if (namedField == null) {
					namedField = field.getSrcName();
				}
				if (namedField == null) {
					continue;
				}
				String key = namedClass + "." + namedField;
				// Java forbids two fields sharing a name, but mapping files can still
				// contain duplicates; drop them rather than guess.
				if (ambiguousFields.contains(key)) {
					continue;
				}
				if (fieldDocs.containsKey(key)) {
					fieldDocs.remove(key);
					ambiguousFields.add(key);
					continue;
				}
				fieldDocs.put(key, fieldComment);
			}
		}

		return new JavadocMappings(classDocs, fieldDocs);
	}
}
