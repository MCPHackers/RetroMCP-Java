package org.mcphackers.mcp.tools;

import java.awt.Desktop;
import java.awt.Toolkit;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public abstract class Util {

	public static int runCommand(String[] cmd, Path dir, boolean killOnShutdown) throws IOException {
		ProcessBuilder procBuilder = new ProcessBuilder(cmd);
		if(dir != null) {
			procBuilder.directory(dir.toAbsolutePath().toFile());
		}
		Process proc = procBuilder.start();
		Thread hook = new Thread(proc::destroy);
		if(killOnShutdown) {
			Runtime.getRuntime().addShutdownHook(hook);
		}
		BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		while(proc.isAlive()) {
			while(in.ready()) System.out.println(in.readLine());
			while(err.ready()) System.err.println(err.readLine());
		}
		in.close();
		err.close();
		if(killOnShutdown) {
			Runtime.getRuntime().removeShutdownHook(hook);
		}
		return proc.exitValue();
	}

	public static void runCommand(String[] cmd) throws IOException {
		ProcessBuilder procBuilder = new ProcessBuilder(cmd);
		procBuilder.start();
	}
	
	//official named -> named client server
	public static void mergeMappings(Path client, Path server, Path out) throws IOException {
		MemoryMappingTree clientTree = new MemoryMappingTree();
		MemoryMappingTree serverTree = new MemoryMappingTree();
		try (BufferedReader reader = Files.newBufferedReader(client)) {
			Tiny2Reader.read(reader, clientTree);
		}
		try (BufferedReader reader = Files.newBufferedReader(server)) {
			Tiny2Reader.read(reader, serverTree);
		}
		clientTree.setSrcNamespace("client");
		serverTree.setSrcNamespace("server");
		MemoryMappingTree namedClientTree = new MemoryMappingTree();
		{
			Map<String, String> namespaces = new HashMap<>();
			namespaces.put("named", "client");
			MappingNsCompleter nsCompleter = new MappingNsCompleter(namedClientTree, namespaces);
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
			clientTree.accept(nsSwitch);
		}
		MemoryMappingTree namedServerTree = new MemoryMappingTree();
		{
			Map<String, String> namespaces = new HashMap<>();
			namespaces.put("named", "server");
			MappingNsCompleter nsCompleter = new MappingNsCompleter(namedServerTree, namespaces);
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
			serverTree.accept(nsSwitch);
		}
		namedServerTree.accept(namedClientTree);
		try(Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(out), false)) {
			namedClientTree.accept(writer);
		}
	}
	
	//official named -> named client
	public static void mergeMappings(Path client, Path out) throws IOException {
		MemoryMappingTree clientTree = new MemoryMappingTree();
		try (BufferedReader reader = Files.newBufferedReader(client)) {
			Tiny2Reader.read(reader, clientTree);
		}
		clientTree.setSrcNamespace("client");
		MemoryMappingTree namedClientTree = new MemoryMappingTree();
		{
			Map<String, String> namespaces = new HashMap<>();
			namespaces.put("named", "client");
			MappingNsCompleter nsCompleter = new MappingNsCompleter(namedClientTree, namespaces);
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
			clientTree.accept(nsSwitch);
		}
		try(Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(out), false)) {
			namedClientTree.accept(writer);
		}
	}
	
	public static void copyToClipboard(String text) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	public static Thread operateOnThread(Runnable function) {
		Thread thread = new Thread(function);
		thread.start();
		return thread;
	}

	public static void openUrl(String url) {
		try {
			switch (OS.getOs()) {
				case linux:
					new ProcessBuilder("/usr/bin/env", "xdg-open", url).start();
					break;
				default:
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
	
	@Deprecated
	public static Map<String, Object> jsonToMap(JSONObject jsonobj)  throws JSONException {
		Map<String, Object> map = new HashMap<>();
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

	@Deprecated
	public static List<Object> jsonToList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<>();
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

	public static JSONObject parseJSONFile(Path path) throws JSONException, IOException {
		String content = new String(Files.readAllBytes(path));
		return new JSONObject(content);
	}

	public static JSONObject parseJSON(InputStream stream) throws JSONException, IOException {
		byte[] bytes = readAllBytes(stream);
		String content = new String(bytes);
		return new JSONObject(content);
	}

	public static JSONArray parseJSONArray(InputStream stream) throws JSONException, IOException {
		byte[] bytes = readAllBytes(stream);
		String content = new String(bytes);
		return new JSONArray(content);
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
			InputStream fs = Files.newInputStream(file);
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
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static String getSHA1(Path file) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			InputStream fs = Files.newInputStream(file);
			BufferedInputStream bs = new BufferedInputStream(fs);
			byte[] buffer = new byte[1024];
			int bytesRead;
	
			while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
				md.update(buffer, 0, bytesRead);
			}
			byte[] digest = md.digest();
	
			StringBuilder sb = new StringBuilder();
			for (byte bite : digest) {
				sb.append(Integer.toString((bite & 255) + 256, 16).substring(1).toLowerCase());
			}
			bs.close();
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
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
}
