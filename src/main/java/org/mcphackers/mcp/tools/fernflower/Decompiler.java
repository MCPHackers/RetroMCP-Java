package org.mcphackers.mcp.tools.fernflower;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.mcphackers.mcp.ProgressListener;

import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.decompiler.DirectoryResultSaver;
import de.fernflower.main.decompiler.SingleFileSaver;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IResultSaver;
import de.fernflower.util.InterpreterUtil;

public class Decompiler implements IBytecodeProvider {
	public DecompileLogger log;
	public static TinyJavadocProvider tinyJavadocProvider;

	public Decompiler(ProgressListener listener) {
		this.log = new DecompileLogger(listener);
	}

	public void decompile(Path source, Path out, Path javadocs, String ind) throws IOException {
		Map<String, Object> mapOptions = new HashMap<>();
		mapOptions.put("rbr", "0");
		mapOptions.put("asc", "1");
		mapOptions.put("nco", "1");
		mapOptions.put("ind", ind);

		SaveType saveType = SaveType.FOLDER;
		File destination = out.toFile();
		  if (destination.getName().contains(".zip") || destination.getName().contains(".jar")) {
			saveType = SaveType.FILE;
	
			if (destination.getParentFile() != null) {
			  destination.getParentFile().mkdirs();
			}
		  } else {
			destination.mkdirs();
		  }
		List<File> lstSources = new ArrayList<>();
		addPath(lstSources, source.toString());

		if (lstSources.isEmpty()) {
			throw new IOException("No sources found");
		}
		File jdFile = javadocs.toFile();
		tinyJavadocProvider = jdFile.exists() ? new TinyJavadocProvider(jdFile) : null;
		BaseDecompiler decompiler = new BaseDecompiler(this, saveType.getSaver().apply(destination), mapOptions, log, tinyJavadocProvider);
		try {
			for (File source2 : lstSources) {
				decompiler.addSpace(source2, true);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		decompiler.decompileContext();
	}

	private static void addPath(List<File> list, String path) {
		File file = new File(path);
		if (file.exists()) {
			list.add(file);
		}
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

	public enum SaveType {
	  FOLDER(DirectoryResultSaver::new),
	  FILE(SingleFileSaver::new);

	  private final Function<File, IResultSaver> saver;

	  SaveType(Function<File, IResultSaver> saver) {

		this.saver = saver;
	  }

	  public Function<File, IResultSaver> getSaver() {
		return saver;
	  }
	}
}