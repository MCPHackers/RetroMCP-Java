package org.mcphackers.mcp.tools.fernflower;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.mcphackers.mcp.tasks.ProgressListener;

import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.decompiler.DirectoryResultSaver;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IFernflowerPreferences;
import de.fernflower.util.InterpreterUtil;

public class Decompiler implements IBytecodeProvider {
	public final DecompileLogger log;
	private final Path source;
	private final List<Path> libraries;
	private final Path destination;
	private final Map<String, Object> mapOptions = new HashMap<>();

	public Decompiler(ProgressListener listener, Path source, Path out, List<Path> libs, String ind, boolean guessGenerics) {
		this.source = source;
		this.libraries = libs;
		this.destination = out;
		this.log = new DecompileLogger(listener);
		mapOptions.put(IFernflowerPreferences.NO_COMMENT_OUTPUT, "1");
		mapOptions.put(IFernflowerPreferences.REMOVE_BRIDGE, guessGenerics ? "1" : "0");
		mapOptions.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1");
		mapOptions.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		mapOptions.put(IFernflowerPreferences.INDENT_STRING, ind);
	}

	public void decompile() throws IOException {
		BaseDecompiler decompiler = new BaseDecompiler(this, new DirectoryResultSaver(destination.toFile()), mapOptions, log/*, javadocs.exists() ? new TinyJavadocProvider(javadocs) : null*/);
		for(Path lib : libraries) {
			if(Files.exists(lib))
				decompiler.addSpace(lib.toAbsolutePath().toFile(), false);
		}
		decompiler.addSpace(source.toAbsolutePath().toFile(), true);
		decompiler.decompileContext();
	}

	@Override
	public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
		File file = new File(externalPath);
		if (internalPath == null) {
			return InterpreterUtil.getBytes(file);
		} else {
			try (ZipFile archive = new ZipFile(file)) {
				ZipEntry entry = archive.getEntry(internalPath);
				if (entry == null) {
					throw new IOException("Entry not found: " + internalPath);
				}
				return InterpreterUtil.getBytes(archive, entry);
			}
		}
	}
}
