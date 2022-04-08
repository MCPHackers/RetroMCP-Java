package org.mcphackers.mcp.tools.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
public abstract class Constants {
	
	public static void replace(Path src, List<Constants> constants) throws IOException {
		Source.modify(src, code -> {
			for(Constants constantReplacer : constants) {
				code = constantReplacer.replace_constants(code);
			}
			return code;
		});
	}
	
	protected abstract String replace_constants(String code);
}
