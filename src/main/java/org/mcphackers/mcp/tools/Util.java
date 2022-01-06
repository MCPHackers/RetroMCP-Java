package org.mcphackers.mcp.tools;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {

    public static int runCommand(String cmd) throws IOException, IllegalArgumentException {
    	return runCommand(cmd, false);
    }

    public static int runCommand(String cmd, boolean doLog) throws IOException, IllegalArgumentException {
        Process proc = Runtime.getRuntime().exec(cmd);
        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        while (proc.isAlive()) {
        	String line;
        	if(doLog && (line = input.readLine()) != null) {
        	    MCP.logger.println(line);
        	}
        }
        input.close();
        return proc.exitValue();
    }
    
    public static Path getPath(String pathStr) {
        String[] all = pathStr.split("/");
        String[] more = new String[all.length - 1];
        for(int i = 1; i < all.length; i++) {
            more[i - 1] = all[i];
        }
        return Paths.get(all[0], more);
    }

    public static void unzip(final Path zipFile, final Path destDir, boolean deleteZip) throws IOException {
        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        new ZipFile(zipFile.toFile()).extractAll(destDir.toString());
        if (deleteZip) Files.deleteIfExists(zipFile);
    }

    public static void unzip(final Path zipFile, final Path destDir) throws IOException {
    	unzip(zipFile, destDir, false);
    }

    public static void unzipByExtension(final Path src, final Path destDir, String extension) throws IOException {
        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        if (Files.exists(src)) {
            ZipFile zipFile = new ZipFile(src.toFile());
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                String fileName = fileHeader.getFileName();
                if (!fileHeader.isDirectory() && fileName.endsWith(extension)) {
                    zipFile.extractFile(fileHeader, destDir.toString());
                }
            }
        }
    }

    public static void downloadFile(URL url, String fileName) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(url.openStream());
        try (FileOutputStream stream = new FileOutputStream(fileName)) {
            FileChannel fileChannel = stream.getChannel();
            fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
        }
    }

    public static String getOperatingSystem() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().startsWith("windows")) {
            return "windows";
        } else {
            // TODO: Make this work for other OSes
            return "null";
        }
    }

    public static JSONObject parseJSONFile(Path path) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(path));
        return new JSONObject(content);
    }

    public static void deleteDirectory(Path path) throws IOException {
  	  Files.walk(path)
	    .sorted(Comparator.reverseOrder())
	    .map(Path::toFile)
	    .forEach(File::delete);
	}

    public static void copyDirectory(Path sourceFolder, Path targetFolder, String[] excludedFolders) throws IOException {
        Files.walk(sourceFolder).filter(p -> !(Files.isDirectory(p) && p.toFile().list().length != 0)).forEach(source -> {
            Path destination = Paths.get(targetFolder.toString(), source.toString().substring(sourceFolder.toString().length()));
		    boolean doCopy = true;
            for (String excludedFolder : excludedFolders) {
            	if(targetFolder.relativize(destination).startsWith(Util.getPath(excludedFolder))) {
            		doCopy = false;
            		break;
            	}
            }
            if(doCopy)
            	try {
		        	Files.createDirectories(destination.getParent());
		            Files.copy(source, destination);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
        });
    }

    public static void copyDirectory(Path sourceFolder, Path targetFolder) throws IOException {
    	copyDirectory(sourceFolder, targetFolder, new String[] {});
    }

    public static String getMD5OfFile(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fs = new FileInputStream(file);
        BufferedInputStream bs = new BufferedInputStream(fs);
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        byte[] digest = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte bite : digest) {
            sb.append(String.format("%02x", bite & 0xff));
        }
        bs.close();
        return sb.toString();
    }

    public static void compress(Path sourceDir, Path target) throws IOException {
        final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(target.toFile()));
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                try {
                    Path targetFile = sourceDir.relativize(file);
                    outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                    byte[] bytes = Files.readAllBytes(file);
                    outputStream.write(bytes, 0, bytes.length);
                    outputStream.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }
        });
        outputStream.close();
    }
    
    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
	
    public static Map<String, Object> jsonToMap(JSONObject jsonobj)  throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<String> keys = jsonobj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }   
            map.put(key, value);
        }   return map;
    }

    public static List<Object> jsonToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
            }
            else if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}
