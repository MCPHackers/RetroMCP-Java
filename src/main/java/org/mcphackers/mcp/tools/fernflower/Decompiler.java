package org.mcphackers.mcp.tools.fernflower;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.mcphackers.mcp.tasks.ProgressListener;

public class Decompiler implements IBytecodeProvider, IResultSaver {
	public final DecompileLogger log;
	private final Path source;
	private final Path destination;
	private final Map<String, Object> mapOptions = new HashMap<>();
	private Map<String, ZipOutputStream> mapArchiveStreams = new HashMap<String, ZipOutputStream>();
	private Map<String, Set<String>> mapArchiveEntries = new HashMap<String, Set<String>>();

	public Decompiler(ProgressListener listener, Path source, Path out, Path javadocs, String ind) {
		this.source = source;
		this.destination = out;
		this.log = new DecompileLogger(listener);
		mapOptions.put(IFernflowerPreferences.NO_COMMENT_OUTPUT, "1");
		mapOptions.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
		mapOptions.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1");
		mapOptions.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		mapOptions.put(IFernflowerPreferences.INDENT_STRING, ind);
	}

	public void decompile() throws IOException {
		BaseDecompiler decompiler = new BaseDecompiler(this, this, mapOptions, log);
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

	  @Override
	  public void saveFolder(String path) {
	  }

	  @Override
	  public void copyFile(String source, String path, String entryName) {
	  }

	  @Override
	  public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
	  }

	  @Override
	  public void createArchive(String path, String archiveName, Manifest manifest) {
		String file = destination.toString();
	    try {
	      Files.createFile(destination);

	      OutputStream fileStream = Files.newOutputStream(destination);
	      ZipOutputStream zipStream = manifest != null ? new JarOutputStream(fileStream, manifest) : new ZipOutputStream(fileStream);
	      mapArchiveStreams.put(file, zipStream);
	    }
	    catch (IOException ex) {
	      DecompilerContext.getLogger().writeMessage("Cannot create archive " + file, ex);
	    }
	  }

	  @Override
	  public void saveDirEntry(String path, String archiveName, String entryName) {
	    saveClassEntry(path, archiveName, null, entryName, null);
	  }

	  @Override
	  public void copyEntry(String source, String path, String archiveName, String entryName) {
		String file = destination.toString();

	    if (!checkEntry(entryName, file)) {
	      return;
	    }

	    try {
	      ZipFile srcArchive = new ZipFile(new File(source));
	      try {
	        ZipEntry entry = srcArchive.getEntry(entryName);
	        if (entry != null) {
	          InputStream in = srcArchive.getInputStream(entry);
	          ZipOutputStream out = mapArchiveStreams.get(file);
	          out.putNextEntry(new ZipEntry(entryName));
	          InterpreterUtil.copyStream(in, out);
	          in.close();
	        }
	      }
	      finally {
	        srcArchive.close();
	      }
	    }
	    catch (IOException ex) {
	      String message = "Cannot copy entry " + entryName + " from " + source + " to " + file;
	      DecompilerContext.getLogger().writeMessage(message, ex);
	    }
	  }

	  @Override
	  public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
	    String file = destination.toString();

	    if (!checkEntry(entryName, file)) {
	      return;
	    }

	    try {
	      ZipOutputStream out = mapArchiveStreams.get(file);
	      out.putNextEntry(new ZipEntry(entryName));
	      if (content != null) {
	        out.write(content.getBytes("UTF-8"));
	      }
	    }
	    catch (IOException ex) {
	      String message = "Cannot write entry " + entryName + " to " + file;
	      DecompilerContext.getLogger().writeMessage(message, ex);
	    }
	  }

	  private boolean checkEntry(String entryName, String file) {
	    Set<String> set = mapArchiveEntries.get(file);
	    if (set == null) {
	      mapArchiveEntries.put(file, set = new HashSet<String>());
	    }

	    boolean added = set.add(entryName);
	    if (!added) {
	      String message = "Zip entry " + entryName + " already exists in " + file;
	      DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
	    }
	    return added;
	  }

	  @Override
	  public void closeArchive(String path, String archiveName) {
		  String file = destination.toString();
	    try {
	      mapArchiveEntries.remove(file);
	      mapArchiveStreams.remove(file).close();
	    }
	    catch (IOException ex) {
	      DecompilerContext.getLogger().writeMessage("Cannot close " + file, IFernflowerLogger.Severity.WARN);
	    }
	  }
}