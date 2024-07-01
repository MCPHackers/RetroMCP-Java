package org.mcphackers.mcp.tools;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class Util {
	public static final ExecutorService SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

	public static int runCommand(String[] cmd, Path dir, boolean killOnShutdown) throws IOException {
		ProcessBuilder procBuilder = new ProcessBuilder(cmd);
		if (dir != null) {
			procBuilder.directory(dir.toAbsolutePath().toFile());
		}
		Process proc = procBuilder.start();
		Thread hook = new Thread(proc::destroy);
		if (killOnShutdown) {
			Runtime.getRuntime().addShutdownHook(hook);
		}
		BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		Thread stderr = new Thread(()-> {
			String line;
			while (true) {
				try {
					line = err.readLine();
					if(line != null) {
						System.out.println("Minecraft STDERR: " + line);
					}
				} catch (IOException ignored) {
					// we don't really care what happens here
				}
			}

		});
		Thread stdout = new Thread(()-> {
			String line;
			while (true) {
				try {
					line = in.readLine();
					if(line != null) {
						System.out.println( line);
					}
				} catch (IOException ignored) {
					// we don't really care what happens here
				}
			}

		});
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException("thread interrupted while runCommand was waiting for a process to finish", e );
		}
		in.close();
		err.close();
		stderr.interrupt();
		stdout.interrupt();

		if (killOnShutdown) {
			Runtime.getRuntime().removeShutdownHook(hook);
		}
		return proc.exitValue();
	}

	public static void runCommand(String[] cmd) throws IOException {
		ProcessBuilder procBuilder = new ProcessBuilder(cmd);
		procBuilder.start();
	}

	public static void copyToClipboard(String text) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	public static Thread operateOnThread(Runnable function) {
		Thread thread = new Thread(function);
		thread.start();
		return thread;
	}

	public static Future<?> enqueueRunnable(Runnable function) {
		return SINGLE_THREAD_EXECUTOR.submit(function);
	}

	public static void openUrl(String url) {
		try {
			if (Objects.requireNonNull(OS.getOs()) == OS.linux) {
				new ProcessBuilder("/usr/bin/env", "xdg-open", url).start();
			} else {
				if (Desktop.isDesktopSupported()) {
					Desktop desktop = Desktop.getDesktop();
					desktop.browse(new URI(url));
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public static byte[] readAllBytes(InputStream inputStream) throws IOException {
		final int bufLen = 4 * 0x400; // 4KB
		byte[] buf = new byte[bufLen];
		int readLen;
		IOException exception = null;

		try {
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
					outputStream.write(buf, 0, readLen);

				return outputStream.toByteArray();
			}
		} catch (IOException e) {
			exception = e;
			throw e;
		} finally {
			if (exception == null) inputStream.close();
			else try {
				inputStream.close();
			} catch (IOException e) {
				exception.addSuppressed(e);
			}
		}
	}

	public static String getMD5(Path file) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = getDigest(md, file);

			StringBuilder sb = new StringBuilder();
			for (byte bite : digest) {
				sb.append(String.format("%02x", bite & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static String getSHA1(Path file) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = getDigest(md, file);

			StringBuilder sb = new StringBuilder();
			for (byte bite : digest) {
				sb.append(Integer.toString((bite & 255) + 256, 16).substring(1).toLowerCase());
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static byte[] getDigest(MessageDigest md, Path file) {
		try (InputStream fs = Files.newInputStream(file)) {
			BufferedInputStream bs = new BufferedInputStream(fs);
			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
				md.update(buffer, 0, bytesRead);
			}
            return md.digest();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return new byte[] {};
	}

	public static String firstUpperCase(String s) {
		if (s == null) {
			return null;
		}
		if (s.length() <= 1) {
			return s.toUpperCase();
		} else {
			return Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}
	}

	public static String convertFromEscapedString(String s) {
		return s.replace("\\n", "\n").replace("\\t", "\t");
	}

	public static String convertToEscapedString(String s) {
		return s.replace("\n", "\\n").replace("\t", "\\t");
	}

	public static String getJava() {
		return System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
	}

	public static int getJavaVersion() {
		String javaVersion = System.getProperty("java.version");
		String[] versionParts = javaVersion.split("\\.");
		int versionNumber = Integer.parseInt(versionParts[0]);

		if (versionNumber < 9) {
			versionNumber = Integer.parseInt(versionParts[1]);
		} else {
			versionNumber = Integer.parseInt(versionParts[0]);
		}
		return versionNumber;
	}
}
