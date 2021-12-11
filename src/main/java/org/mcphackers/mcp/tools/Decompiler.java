package org.mcphackers.mcp.tools;

import de.fernflower.main.DecompilerContext;
import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IFernflowerLogger;
import de.fernflower.main.extern.IResultSaver;
import de.fernflower.util.InterpreterUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Decompiler implements IBytecodeProvider, IResultSaver {

    private final Map<String, ZipOutputStream> mapArchiveStreams = new HashMap<String, ZipOutputStream>();
    private final Map<String, Set<String>> mapArchiveEntries = new HashMap<String, Set<String>>();
    public DecompileLogger log;
    private String pathout;

    public Decompiler() {
        this.log = new DecompileLogger();
    }

    private static void addPath(List<File> list, String path) {
        File file = new File(path);
        if (file.exists()) {
            list.add(file);
        }
    }

    public void decompile(String source, String out) throws IOException {
        Map<String, Object> mapOptions = new HashMap<String, Object>();
        mapOptions.put("rbr", "0");
        mapOptions.put("asc", "1");
        mapOptions.put("nco", "1");
        mapOptions.put("ind", "\t");

        pathout = out;
        File destination = new File(out);
        if (!destination.isDirectory()) {
            if (!destination.mkdirs()) {
                throw new IOException("Could not create '" + destination + "'");
            }
        }
        List<File> lstSources = new ArrayList<File>();
        addPath(lstSources, source);

        if (lstSources.isEmpty()) {
            throw new IOException("No sources found");
        }

        BaseDecompiler decompiler = new BaseDecompiler(this, this, mapOptions, log);
        try {
            for (File source2 : lstSources) {
                decompiler.addSpace(source2, true);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        decompiler.decompileContext();
    }

    private String getAbsolutePath(String path) {
        return (new File(pathout, path)).getAbsolutePath();
    }

    @Override
    public void saveFolder(String path) {
        File dir = new File(getAbsolutePath(path));
        if (!(dir.mkdirs() || dir.isDirectory())) {
            throw new RuntimeException("Cannot create directory " + dir);
        }
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
        try {
            InterpreterUtil.copyFile(new File(source), new File(getAbsolutePath(path), entryName));
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot copy " + source + " to " + entryName, ex);
        }
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        File file = new File(getAbsolutePath(path), entryName);
        try {
            try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                out.write(content);
            }
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot write class file " + file, ex);
        }
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
        File file = new File(getAbsolutePath(path), archiveName);
        try {
            if (!(file.createNewFile() || file.isFile())) {
                throw new IOException("Cannot create file " + file);
            }

            FileOutputStream fileStream = new FileOutputStream(file);
            ZipOutputStream zipStream = manifest != null ? new JarOutputStream(fileStream, manifest) : new ZipOutputStream(fileStream);
            mapArchiveStreams.put(file.getPath(), zipStream);
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot create archive " + file, ex);
        }
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
        saveClassEntry(path, archiveName, null, entryName, null);
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entryName) {
        String file = new File(getAbsolutePath(path), archiveName).getPath();

        if (!checkEntry(entryName, file)) {
            return;
        }

        try {
            try (ZipFile srcArchive = new ZipFile(new File(source))) {
                ZipEntry entry = srcArchive.getEntry(entryName);
                if (entry != null) {
                    InputStream in = srcArchive.getInputStream(entry);
                    ZipOutputStream out = mapArchiveStreams.get(file);
                    out.putNextEntry(new ZipEntry(entryName));
                    InterpreterUtil.copyStream(in, out);
                    in.close();
                }
            }
        } catch (IOException ex) {
            String message = "Cannot copy entry " + entryName + " from " + source + " to " + file;
            DecompilerContext.getLogger().writeMessage(message, ex);
        }
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        String file = new File(getAbsolutePath(path), archiveName).getPath();

        if (!checkEntry(entryName, file)) {
            return;
        }

        try {
            ZipOutputStream out = mapArchiveStreams.get(file);
            out.putNextEntry(new ZipEntry(entryName));
            if (content != null) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
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
        String file = new File(getAbsolutePath(path), archiveName).getPath();
        try {
            mapArchiveEntries.remove(file);
            mapArchiveStreams.remove(file).close();
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot close " + file, IFernflowerLogger.Severity.WARN);
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
}