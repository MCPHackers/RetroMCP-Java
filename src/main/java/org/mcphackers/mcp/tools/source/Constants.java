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
		modify(src, code -> {
			for(Constants constantReplacer : constants) {
				code = constantReplacer.replace_constants(code);
			}
			return code;
		});
	}
	
	/**
	 * Implementation of constant replacing behavior
	 * @param code string of source file
	 * @return modified code string
	 */
	protected abstract String replace_constants(String code);
}
