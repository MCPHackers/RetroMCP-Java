package org.mcphackers.mcp.tools.javadoc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mcphackers.mcp.tools.source.Source;

/**
 * Source modification step that injects Javadoc comments above class and field
 * declarations using metadata loaded from {@link JavadocMappings}.
 *
 * <p>This is a textual rewrite, not a parser. To stay reliable without a real
 * Java parser the implementation tracks brace depth and only documents members
 * that live directly in the top-level type body (depth 1 inside the type).
 * Anything else (locals, inner-class members, anonymous bodies, etc.) is left
 * untouched.
 */
public final class JavadocSource extends Source {

	// First class / interface / enum declaration in the file.
	private static final Pattern TOP_LEVEL_TYPE = Pattern.compile(
		"(?m)^(\\s*)(?:public\\s+|abstract\\s+|final\\s+|strictfp\\s+)*(?:class|interface|enum)\\s+(\\w+)");

	// Field declaration on a single line. Captures the field's first identifier.
	// Excludes lines containing '(' so method/constructor declarations are not
	// matched. Requires the line to end with ';' or '=' so a partial match in
	// the middle of an expression is impossible.
	private static final Pattern FIELD_LINE = Pattern.compile(
		"^[ \\t]*(?:(?:public|protected|private|static|final|volatile|transient)\\s+)+"
			+ "[\\w<>\\[\\]\\.,\\s\\?]+?\\s+(\\w+)\\s*[=;]");

	private final JavadocMappings mappings;

	public JavadocSource(JavadocMappings mappings) {
		this.mappings = mappings;
	}

	@Override
	public void apply(String classNameOnDisk, StringBuilder source) {
		if (mappings.isEmpty()) {
			return;
		}
		String internalName = toInternalName(classNameOnDisk);
		if (internalName == null) {
			return;
		}

		List<Insertion> insertions = new ArrayList<>();
		collectClassDoc(source, internalName, insertions);
		collectFieldDocs(source, internalName, insertions);

		// Apply back-to-front so earlier offsets stay valid.
		for (int i = insertions.size() - 1; i >= 0; i--) {
			Insertion ins = insertions.get(i);
			source.insert(ins.offset, ins.text);
		}
	}

	private void collectClassDoc(StringBuilder source, String internalName, List<Insertion> out) {
		String doc = mappings.getClassDoc(internalName);
		if (doc == null || doc.isEmpty()) {
			return;
		}
		Matcher m = TOP_LEVEL_TYPE.matcher(source);
		if (!m.find()) {
			return;
		}
		if (hasPrecedingJavadoc(source, m.start())) {
			return;
		}
		out.add(new Insertion(m.start(), formatJavadoc(doc, m.group(1))));
	}

	private void collectFieldDocs(StringBuilder source, String internalName, List<Insertion> out) {
		// Anchor brace tracking just after the top-level type keyword so that
		// braces inside class-level annotations cannot be mistaken for the body.
		Matcher type = TOP_LEVEL_TYPE.matcher(source);
		if (!type.find()) {
			return;
		}
		int scanStart = source.indexOf("{", type.end());
		if (scanStart < 0) {
			return;
		}
		int depth = 1; // we are now inside the top-level type body
		int len = source.length();

		for (int i = scanStart + 1; i < len; i++) {
			char c = source.charAt(i);

			if (c == '"' || c == '\'') {
				i = skipQuoted(source, i, c);
				continue;
			}
			if (c == '/' && i + 1 < len) {
				char next = source.charAt(i + 1);
				if (next == '/') {
					int eol = source.indexOf("\n", i);
					if (eol < 0) return;
					i = eol;
					// fall through so the '\n' branch below runs on next iteration
					i--;
					continue;
				}
				if (next == '*') {
					int end = source.indexOf("*/", i + 2);
					if (end < 0) return;
					i = end + 1;
					continue;
				}
			}

			if (c == '{') {
				depth++;
				continue;
			}
			if (c == '}') {
				depth--;
				if (depth == 0) {
					return; // closed the top-level type body
				}
				continue;
			}
			if (c == '\n') {
				if (depth == 1) {
					int eol = source.indexOf("\n", i + 1);
					if (eol < 0) eol = len;
					tryFieldOnLine(source, i + 1, eol, internalName, out);
				}
			}
		}
	}

	private void tryFieldOnLine(StringBuilder source, int start, int end, String internalName, List<Insertion> out) {
		String line = source.substring(start, end);
		// Cheap reject: method/constructor declarations contain '('.
		if (line.indexOf('(') >= 0) {
			return;
		}
		Matcher m = FIELD_LINE.matcher(line);
		if (!m.find()) {
			return;
		}
		String fieldName = m.group(1);
		String doc = mappings.getFieldDoc(internalName, fieldName);
		if (doc == null || doc.isEmpty()) {
			return;
		}
		if (hasPrecedingJavadoc(source, start)) {
			return;
		}
		String indent = leadingWhitespace(line);
		out.add(new Insertion(start, formatJavadoc(doc, indent)));
	}

	private static int skipQuoted(StringBuilder source, int start, char quote) {
		int len = source.length();
		for (int i = start + 1; i < len; i++) {
			char c = source.charAt(i);
			if (c == '\\') {
				i++;
				continue;
			}
			if (c == quote) {
				return i;
			}
			if (c == '\n') {
				// Unterminated literal; bail out at end of line.
				return i - 1;
			}
		}
		return len - 1;
	}

	private static String leadingWhitespace(String line) {
		int i = 0;
		while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
			i++;
		}
		return line.substring(0, i);
	}

	private static boolean hasPrecedingJavadoc(StringBuilder source, int pos) {
		int i = pos - 1;
		while (i >= 0 && Character.isWhitespace(source.charAt(i))) {
			i--;
		}
		return i >= 1 && source.charAt(i - 1) == '*' && source.charAt(i) == '/';
	}

	private static String formatJavadoc(String text, String indent) {
		StringBuilder out = new StringBuilder();
		out.append(indent).append("/**\n");
		for (String line : text.split("\\r?\\n", -1)) {
			out.append(indent).append(" * ").append(line).append('\n');
		}
		out.append(indent).append(" */\n");
		return out.toString();
	}

	/**
	 * Converts the path-like file identifier passed by {@link Source#modify} into
	 * the internal JVM class name used as the mapping key.
	 *
	 * <p>{@code Source.modify} hands us the file path with the {@code .java}
	 * extension stripped, e.g. {@code .../src_original/net/minecraft/src/Block}.
	 * Mappings key classes by internal name ({@code net/minecraft/src/Block}).
	 */
	static String toInternalName(String classNameOnDisk) {
		if (classNameOnDisk == null) {
			return null;
		}
		String normalised = classNameOnDisk.replace('\\', '/');
		int marker = normalised.lastIndexOf("/net/minecraft/");
		if (marker >= 0) {
			return normalised.substring(marker + 1);
		}
		int lastSlash = normalised.lastIndexOf('/');
		if (lastSlash <= 0) {
			return normalised;
		}
		int prevSlash = normalised.lastIndexOf('/', lastSlash - 1);
		return prevSlash < 0 ? normalised : normalised.substring(prevSlash + 1);
	}

	private static final class Insertion {
		final int offset;
		final String text;

		Insertion(int offset, String text) {
			this.offset = offset;
			this.text = text;
		}
	}
}
