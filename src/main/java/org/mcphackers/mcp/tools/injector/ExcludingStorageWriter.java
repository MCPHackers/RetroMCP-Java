package org.mcphackers.mcp.tools.injector;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.nio.ClassStorageWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExcludingStorageWriter extends ClassStorageWriter {
	private final Set<String> excludes;

	public ExcludingStorageWriter(ClassStorage storage, int flags, Set<String> excludes) {
		super(storage, flags);
		this.excludes = excludes;
	}

	public void write(OutputStream out) throws IOException {
		Set<String> writtenClasses = new HashSet<>();
		ZipOutputStream jarOut = new ZipOutputStream(out);

		for(ClassNode classNode : this.storage) {
			if (!writtenClasses.contains(classNode.name)) {
				if (!this.excludes.contains(classNode.name)) {
					ClassWriter writer = new ClassWriter(this.flags);
					classNode.accept(writer);
					jarOut.putNextEntry(new ZipEntry(classNode.name + ".class"));
					jarOut.write(writer.toByteArray());
					jarOut.closeEntry();
					writtenClasses.add(classNode.name);
				}
			}
		}

		jarOut.close();
	}
}
