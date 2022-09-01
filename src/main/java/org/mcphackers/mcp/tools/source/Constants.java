package org.mcphackers.mcp.tools.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public abstract class Constants extends Source {
	
	/**
	 * Replaces constants for all source files in <code>src</code> directory
	 * @param src source directory
	 * @param constants list of constant replacers
	 * @throws IOException
	 */
	public static void replace(Path src, List<Constants> constants) throws IOException {
		modify(src, source -> {
			for(Constants constantReplacer : constants) {
				constantReplacer.replace_constants(source);
			}
		});
	}
	
	/**
	 * Implementation of constant replacing behavior
	 * @param StringBuilder of source file
	 */
	protected abstract void replace_constants(StringBuilder source);
}
