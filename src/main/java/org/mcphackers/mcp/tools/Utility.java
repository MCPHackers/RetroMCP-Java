package org.mcphackers.mcp.tools;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class Utility {

    public static int runCommand(String cmd) throws IOException, IllegalArgumentException {
        Process proc = Runtime.getRuntime().exec(cmd);
        while (proc.isAlive()) ;
        return proc.exitValue();
    }

    public static void unzip(final Path zipFile, final Path destDir) throws IOException {
        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        new ZipFile(zipFile.toFile()).extractAll(destDir.toString());
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
                if (!(fileName.startsWith("paulscode") || fileName.startsWith("com")) && fileName.endsWith(extension)) {
                    zipFile.extractFile(fileHeader, destDir.toString());
                }
            }
        }
    }

    public static void downloadFile(URL url, String fileName) {
        try {
            ReadableByteChannel channel = Channels.newChannel(url.openStream());
            try (FileOutputStream stream = new FileOutputStream(fileName)) {
                FileChannel fileChannel = stream.getChannel();
                fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public static void deleteDirectoryStream(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach((file) -> {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.err.println("Failed to delete " + file.getAbsolutePath());
                }
            });
        }
    }

    public static void copyDirectory(Path sourceFolder, Path targetFolder) throws IOException {
        Files.walk(sourceFolder).forEach(source -> {
            Path destination = Paths.get(targetFolder.toString(), source.toString().substring(sourceFolder.toString().length()));
            try {
                Files.copy(source, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
