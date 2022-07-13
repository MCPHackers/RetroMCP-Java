package org.mcphackers.mcp.tools.fernflower;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IFernflowerPreferences;
import de.fernflower.util.InterpreterUtil;
import org.mcphackers.mcp.tasks.ProgressListener;

public class Decompiler implements IBytecodeProvider {
	public final DecompileLogger log;
	private final Path source;
	private final Path destination;
	private final Map<String, Object> mapOptions = new HashMap<>();

	public Decompiler(ProgressListener listener, Path source, Path out, Path javadocs, String ind, boolean override) {
		this.source = source;
		this.destination = out;
		this.log = new DecompileLogger(listener);
		//FIXME
		//mapOptions.put(IFernflowerPreferences.OVERRIDE_ANNOTATION, override ? "1" : "0");
		mapOptions.put(IFernflowerPreferences.NO_COMMENT_OUTPUT, "1");
		mapOptions.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
		mapOptions.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1");
		mapOptions.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		mapOptions.put(IFernflowerPreferences.INDENT_STRING, ind);
	}

	public void decompile() throws IOException {
		BaseDecompiler decompiler = new BaseDecompiler(this, new SingleFileSaver(destination), mapOptions, log);
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