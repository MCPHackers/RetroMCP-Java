package org.mcphackers.mcp.tools.injector;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.transform.Injection;
import org.objectweb.asm.tree.ClassNode;

public abstract class SourceFileTransformer implements Injection {
	public static void removeSourceFileAttributes(ClassStorage storage) {
		for (ClassNode clazz : storage.getClasses()) {
			clazz.sourceFile = null;
		}
	}

	public abstract void transform(ClassStorage storage);
}
