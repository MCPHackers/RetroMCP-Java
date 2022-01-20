package org.mcphackers.mcp.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

public class FileUtil {
    
    public static void createDirectories(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }
    
    public static void packFilesToZip(Path sourceZip, Iterable<Path> files, Path relativeTo) throws IOException {
        try(FileSystem fs = FileSystems.newFileSystem(sourceZip, null)) {
            for(Path file : files) {
            	Path fileInsideZipPath = fs.getPath(relativeTo.relativize(file).toString());
            	Files.deleteIfExists(fileInsideZipPath);
            	if(fileInsideZipPath.getParent() != null && !Files.exists(fileInsideZipPath.getParent()))
            		Files.createDirectories(fileInsideZipPath.getParent());
            	Files.copy(file, fileInsideZipPath);
            }
        }
    }
    
    public static void deleteFileInAZip(Path sourceZip, String file) throws IOException {
        try(FileSystem fs = FileSystems.newFileSystem(sourceZip, null)) {
        	Path fileInsideZipPath = fs.getPath(file);
        	Files.deleteIfExists(fileInsideZipPath);
        }
    }

    @SuppressWarnings("resource")
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

    public static void downloadFile(URL url, Path output) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(url.openStream());
        try (FileOutputStream stream = new FileOutputStream(output.toAbsolutePath().toString())) {
            FileChannel fileChannel = stream.getChannel();
            fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
        }
    }
    
    @Deprecated
    public static void downloadGitDir(URL url, Path output) throws IOException, URISyntaxException {
		InputStream in = url.openStream();
		JSONArray json = Util.parseJSONArray(in);
		for(Object object : json) {
			if(object instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject)object;
				if(jsonObject.getString("type").equals("dir")) {
					Path dir = output.resolve(jsonObject.getString("name"));
					createDirectories(dir);
					downloadGitDir(new URI(jsonObject.getString("url")).toURL(), dir);
				}
				else if (jsonObject.getString("type").equals("file")) {
					downloadFile(new URI(jsonObject.getString("download_url")).toURL(), output.resolve(jsonObject.getString("name")));
				}
			}
		}
    }
    
    public static String absolutePathString(String path) {
    	return Paths.get(path).toAbsolutePath().toString();
    }
    
    public static void deleteDirectoryIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
        	deleteDirectory(path);
        }
    }

    public static void deleteDirectory(Path path) throws IOException {
  	  Files.walk(path)
	    .sorted(Comparator.reverseOrder())
	    .map(Path::toFile)
	    .forEach(File::delete);
	}

    public static List<Path> listDirectory(Path path) throws IOException {
    	if(!Files.isDirectory(path)) {
    		throw new IOException(path + "is not a directory");
    	}
    	return Files.walk(path).collect(Collectors.toList());
	}

    public static List<Path> listDirectory(Path path, Predicate<Path> predicate) throws IOException {
		if(!Files.isDirectory(path)) {
			throw new IOException(path + "is not a directory");
		}
		return Files.walk(path).filter(predicate).collect(Collectors.toList());
	}

    public static void copyDirectory(Path sourceFolder, Path targetFolder, String[] excludedFolders) throws IOException {
        Files.walk(sourceFolder).filter(p -> !(Files.isDirectory(p) && p.toFile().list().length != 0)).forEach(source -> {
            Path destination = Paths.get(targetFolder.toString(), source.toString().substring(sourceFolder.toString().length()));
		    boolean doCopy = true;
            for (String excludedFolder : excludedFolders) {
            	if(targetFolder.relativize(destination).startsWith(Paths.get(excludedFolder))) {
            		doCopy = false;
            		break;
            	}
            }
            if(doCopy) {
	        	try {
					Files.createDirectories(destination.getParent());
		            Files.copy(source, destination);
				} catch (IOException e) {
				}
		    }
        });
    }

    public static void copyDirectory(Path sourceFolder, Path targetFolder) throws IOException {
    	copyDirectory(sourceFolder, targetFolder, new String[] {});
    }

    public static void compress(Path sourceDir, Path target) throws IOException {
        final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(target.toFile()));
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Path targetFile = sourceDir.relativize(file);
                outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                byte[] bytes = Files.readAllBytes(file);
                outputStream.write(bytes, 0, bytes.length);
                outputStream.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });
        outputStream.close();
    }

}
