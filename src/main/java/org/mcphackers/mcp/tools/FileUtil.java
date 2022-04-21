package org.mcphackers.mcp.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

public class FileUtil {
	
	public static void createDirectories(Path path) throws IOException {
		if (Files.notExists(path)) {
			Files.createDirectories(path);
		}
	}
	
	public static void packFilesToZip(Path sourceZip, Iterable<Path> files, Path relativeTo) throws IOException {
		try(FileSystem fs = FileSystems.newFileSystem(sourceZip, (ClassLoader)null)) {
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
		try(FileSystem fs = FileSystems.newFileSystem(sourceZip, (ClassLoader)null)) {
			Path fileInsideZipPath = fs.getPath(file);
			Files.deleteIfExists(fileInsideZipPath);
		}
	}
	
	public static void copyFileFromAZip(Path sourceZip, String file, Path out) throws IOException {
		try(FileSystem fs = FileSystems.newFileSystem(sourceZip, (ClassLoader)null)) {
			Path fileInsideZipPath = fs.getPath(file);
			Files.copy(fileInsideZipPath, out);
		}
	}

	public static void unzip(final Path zipFile, final Path destDir) throws IOException {
		unzip(zipFile, destDir, false);
	}

	public static void unzip(final Path zipFile, final Path destDir, boolean deleteZip) throws IOException {
		unzip(zipFile, destDir, entry -> true);
		if (deleteZip) Files.deleteIfExists(zipFile);
	}

	public static void unzipByExtension(final Path zipFile, final Path destDir, String extension) throws IOException {
		unzip(zipFile, destDir, entry -> entry.toString().endsWith(extension));
	}

	public static void unzip(final Path zipFile, final Path destDir, Function<ZipEntry,Boolean> match) throws IOException {
		if (Files.notExists(destDir)) {
			Files.createDirectories(destDir);
		}
		try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				Path toPath = destDir.resolve(entry.getName());
				if (match.apply(entry)) {
					if(!entry.isDirectory()) {
						createDirectories(toPath.getParent());
						Files.copy(zipInputStream, toPath);
					}
					else {
						createDirectories(toPath);
					}
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
	public static void downloadGitDir(URL url, Path output) throws IOException {
		InputStream in = url.openStream();
		JSONArray json = Util.parseJSONArray(in);
		for(Object object : json) {
			if(object instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject)object;
				if(jsonObject.getString("type").equals("dir")) {
					Path dir = output.resolve(jsonObject.getString("name"));
					createDirectories(dir);
					downloadGitDir(new URL(jsonObject.getString("url")), dir);
				}
				else if (jsonObject.getString("type").equals("file")) {
					downloadFile(new URL(jsonObject.getString("download_url")), output.resolve(jsonObject.getString("name")));
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
		AtomicBoolean a = new AtomicBoolean(true);
		try (Stream<Path> pathStream = Files.walk(path)) {
			pathStream.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(f -> {
						boolean b = f.delete();
						if(!b) a.set(false);
					});
		}
		if(!a.get()) {
			throw new IOException("One or more files were locked");
		}
	}

	public static List<Path> walkDirectory(Path path) throws IOException {
		if(!Files.isDirectory(path)) {
			throw new IOException(path + "is not a directory");
		}
		try (Stream<Path> pathStream = Files.walk(path)) {
			return pathStream.collect(Collectors.toList());
		}
	}

	public static List<Path> walkDirectory(Path path, Predicate<Path> predicate) throws IOException {
		if(!Files.isDirectory(path)) {
			throw new IOException(path + "is not a directory");
		}
		try (Stream<Path> pathStream = Files.walk(path)) {
			return pathStream.filter(predicate).collect(Collectors.toList());
		}
	}

	/**
	 * @see #deletePackages(Path, String[])
	 */
	@Deprecated
	public static void copyDirectory(Path sourceFolder, Path targetFolder, String[] excludedFolders) throws IOException {
		try (Stream<Path> pathStream = Files.walk(sourceFolder)) {
			pathStream.filter(p -> !(Files.isDirectory(p) && p.toFile().list().length != 0)).forEach(source -> {
				Path destination = targetFolder.resolve(sourceFolder.relativize(source));
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
						e.printStackTrace();
					}
				}
			});
		}
	}

	public static void copyDirectory(Path sourceFolder, Path targetFolder) throws IOException {
		try (Stream<Path> pathStream = Files.walk(sourceFolder)) {
			pathStream.forEach(source -> {
				Path destination = targetFolder.resolve(sourceFolder.relativize(source));
				try {
					Files.copy(source, destination);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public static void compress(Path sourceDir, Path target) throws IOException {
		final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(target.toFile().toPath()));
		Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				Path targetFile = sourceDir.relativize(file);
				outputStream.putNextEntry(new ZipEntry(targetFile.toString().replace("\\", "/")));
				byte[] bytes = Files.readAllBytes(file);
				outputStream.write(bytes, 0, bytes.length);
				outputStream.closeEntry();
				return FileVisitResult.CONTINUE;
			}
		});
		outputStream.close();
	}
	
	public static void copyResource(InputStream is, Path out) throws IOException {
		byte[] data = Util.readAllBytes(is);
		Files.write(out, data);
	}

	public static void deleteEmptyFolders(Path path) throws IOException {
		try (Stream<Path> pathStream = Files.walk(path)) {
			pathStream.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.filter(File::isDirectory)
					.forEach(File::delete);
		}
	}

	public static void deletePackages(Path sourceFolder, String[] excludedFolders) throws IOException {
		try (Stream<Path> pathStream = Files.walk(sourceFolder)) {
			pathStream.filter(p -> !(Files.isDirectory(p) && p.toFile().list().length != 0)).forEach(source -> {
				for (String excludedFolder : excludedFolders) {
					if(sourceFolder.relativize(source).startsWith(Paths.get(excludedFolder))) {
						try {
							Files.deleteIfExists(source);
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					}
				}
			});
		}
		deleteEmptyFolders(sourceFolder);
	}
}
