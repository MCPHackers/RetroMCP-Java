package org.mcphackers.mcp.tools.fernflower;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.decompiler.DirectoryResultSaver;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IFernflowerPreferences;
import de.fernflower.util.InterpreterUtil;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.ProgressListener;
import org.mcphackers.mcp.tasks.mode.TaskParameter;

public class Decompiler implements IBytecodeProvider {
	public final DecompileLogger log;
	private final Path source;
	private final List<Path> libraries;
	private final Path destination;
	private final Map<String, Object> mapOptions;
	private final ZipFileCache openZips = new ZipFileCache();

	public Decompiler(ProgressListener listener, Path source, Path out, List<Path> libs, MCP mcp) {
		this.source = source;
		this.libraries = libs;
		this.destination = out;
		this.log = new DecompileLogger(listener);
		this.mapOptions = mcp.getOptions().getFernflowerOptions();
		this.mapOptions.put(IFernflowerPreferences.REMOVE_BRIDGE, mcp.getOptions().getBooleanParameter(TaskParameter.GUESS_GENERICS) ? "1" : "0");
	}

	public void decompile() throws IOException {
		BaseDecompiler decompiler = new BaseDecompiler(this, new DirectoryResultSaver(destination.toFile()), mapOptions, log/*, javadocs.exists() ? new TinyJavadocProvider(javadocs) : null*/);
		for (Path lib : libraries) {
			if (Files.exists(lib))
				decompiler.addSpace(lib.toAbsolutePath().toFile(), false);
		}
		decompiler.addSpace(source.toAbsolutePath().toFile(), true);
		decompiler.decompileContext();
		this.openZips.close();
	}

	@Override
	public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
		if (internalPath == null) {
			File file = new File(externalPath);
			return InterpreterUtil.getBytes(file);
		} else {
			final ZipFile archive = this.openZips.get(externalPath);
			final ZipEntry entry = archive.getEntry(internalPath);
			if (entry == null) {
				throw new IOException("Entry not found: " + internalPath);
			}
			return InterpreterUtil.getBytes(archive, entry);
		}
	}
}
