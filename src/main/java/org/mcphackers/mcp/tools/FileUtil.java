package org.mcphackers.mcp.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.mcphackers.mcp.MCP;

public abstract class FileUtil {

	public static void delete(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		if (Files.isDirectory(path)) {
			deleteDirectory(path);
		} else {
			Files.delete(path);
		}
	}

	public static void createDirectories(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}
	}

	@SuppressWarnings("RedundantCast")
	public static void packFilesToZip(Path sourceZip, Iterable<Path> files, Path relativeTo) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(sourceZip, (ClassLoader) null)) {
			for (Path file : files) {
				Path fileInsideZipPath = fs.getPath("/" + relativeTo.relativize(file));
				Files.deleteIfExists(fileInsideZipPath);
				if (fileInsideZipPath.getParent() != null && !Files.exists(fileInsideZipPath.getParent()))
					Files.createDirectories(fileInsideZipPath.getParent());
				Files.copy(file, fileInsideZipPath);
			}
		}
	}

	@SuppressWarnings("RedundantCast")
	public static void deleteFileInAZip(Path sourceZip, String file) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(sourceZip, (ClassLoader) null)) {
			Path fileInsideZipPath = fs.getPath(file);
			Files.deleteIfExists(fileInsideZipPath);
		}
	}

	@SuppressWarnings("RedundantCast")
	public static void copyFileFromAZip(Path sourceZip, String file, Path out) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(sourceZip, (ClassLoader) null)) {
			Path fileInsideZipPath = fs.getPath(file);
			Files.copy(fileInsideZipPath, out);
		}
	}

	public static void extract(final Path zipFile, final Path destDir) throws IOException {
		extract(zipFile, destDir, entry -> true);
	}

	public static void extractByExtension(final Path zipFile, final Path destDir, String extension) throws IOException {
		extract(zipFile, destDir, entry -> entry.toString().endsWith(extension));
	}

	public static void extract(final InputStream zipFile, final Path destDir) throws IOException {
		if (!Files.exists(destDir)) {
			Files.createDirectories(destDir);
		}
		try (ZipInputStream zipInputStream = new ZipInputStream(zipFile)) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				Path toPath = destDir.resolve(entry.getName()).normalize();
				Files.deleteIfExists(toPath);
				if (!entry.isDirectory()) {
					createDirectories(toPath.getParent());
					Files.copy(zipInputStream, toPath);
				} else {
					createDirectories(toPath);
				}
			}
		}
	}

	public static void extract(final Path zipFile, final Path destDir, Function<ZipEntry, Boolean> match) throws IOException {
		if (!Files.exists(destDir)) {
			Files.createDirectories(destDir);
		}
		try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				Path toPath = destDir.resolve(entry.getName()).normalize();
				Files.deleteIfExists(toPath);
				if (match.apply(entry)) {
					if (!entry.isDirectory()) {
						createDirectories(toPath.getParent());
						Files.copy(zipInputStream, toPath);
					} else {
						createDirectories(toPath);
					}
				}
			}
		}
	}

	public static void downloadFile(String url, Path output) throws IOException {
		downloadFile(new URL(url), output);
	}

	public static void downloadFile(URL url, Path output) throws IOException {
		ReadableByteChannel channel = Channels.newChannel(openURLStream(url));
		try (FileOutputStream stream = new FileOutputStream(output.toAbsolutePath().toString())) {
			FileChannel fileChannel = stream.getChannel();
			fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);
		}
	}

	public static InputStream openURLStream(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("User-Agent", "RetroMCP/" + MCP.VERSION);
		return connection.getInputStream();
	}

	public static void deleteDirectoryIfExists(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			deleteDirectory(path);
		}
	}

	public static void cleanDirectory(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					deleteDirectory(entry);
				}
			}
		}
	}

	public static void deleteDirectory(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					deleteDirectory(entry);
				}
			}
		}
		Files.delete(path);
	}

	public static List<Path> walkDirectory(Path path) throws IOException {
		if (!Files.isDirectory(path)) {
			throw new IOException(path + "is not a directory");
		}
		try (Stream<Path> pathStream = Files.walk(path)) {
			return pathStream.collect(Collectors.toList());
		}
	}

	public static List<Path> walkDirectory(Path path, Predicate<Path> predicate) throws IOException {
		if (!Files.isDirectory(path)) {
			throw new IOException(path + "is not a directory");
		}
		try (Stream<Path> pathStream = Files.walk(path)) {
			return pathStream.filter(predicate).collect(Collectors.toList());
		}
	}

	public static void copyDirectory(Path sourceFolder, Path targetFolder) throws IOException {
		if (!Files.exists(targetFolder)) {
			Files.createDirectories(targetFolder);
		}
		try (Stream<Path> pathStream = Files.walk(sourceFolder)) {
			pathStream.forEach(source -> {
				Path destination = targetFolder.resolve(sourceFolder.relativize(source));
				try {
					if (!Files.isDirectory(destination)) {
						Files.copy(source, destination);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public static void compress(Path sourceDir, Path target) throws IOException {
		final ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(target));
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

	public static void collectJars(Path libPath, List<Path> list, boolean walk) throws IOException {
		try (Stream<Path> stream = walk ? Files.walk(libPath) : Files.list(libPath)
				.filter(library -> library.getFileName().toString().endsWith(".jar"))
				.filter(library -> !Files.isDirectory(library))) {
			list.addAll(stream.collect(Collectors.toList()));
		}
	}

	public static void collectJars(Path libPath, List<Path> list) throws IOException {
		collectJars(libPath, list, false);
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
					if (sourceFolder.relativize(source).startsWith(Paths.get(excludedFolder))) {
						try {
							Files.delete(source);
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

	public static List<Path> getPathsOfType(Path startDirectory, String... types) {
		if (!Files.isDirectory(startDirectory)) {
			return Collections.emptyList();
		}

		try (Stream<Path> pathStream = Files.walk(startDirectory).parallel().filter(path -> Arrays.stream(types).anyMatch(type -> path.getFileName().toString().endsWith(type)))) {
			return pathStream.collect(Collectors.toList());
		} catch (IOException ex) {
			ex.printStackTrace();
			return Collections.emptyList();
		}
	}
}
