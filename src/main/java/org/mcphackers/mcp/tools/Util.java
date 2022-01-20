package org.mcphackers.mcp.tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Util {

    public static int runCommand(String[] cmd, Path dir, boolean doLog) throws IOException, InterruptedException {
        ProcessBuilder procBuilder = new ProcessBuilder(cmd);
        if(dir != null) {
        	procBuilder.directory(dir.toAbsolutePath().toFile());
        }
        Process proc = procBuilder.start();
    	new Thread() {
    		public void run() {
    			if(doLog) {
	    			try(Scanner sc = new Scanner(proc.getInputStream())) {
	    				while (sc.hasNextLine()) {
	    					MCP.logger.info(sc.nextLine());
	    				}
	    			}
    			}
    		}
    	}.start();
    	while(proc.isAlive()) {
			if(doLog) {
	    		try(Scanner sc = new Scanner(proc.getErrorStream())) {
					while (sc.hasNextLine()) {
						MCP.logger.info(sc.nextLine());
					}
				}
    		}
    	}
        return proc.exitValue();
    }

    public static OS getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win") ? OS.windows
        	: (osName.contains("mac") ? OS.macos
        	: (osName.contains("solaris") ? OS.linux
        	: (osName.contains("sunos") ? OS.linux
        	: (osName.contains("linux") ? OS.linux
        	: (osName.contains("unix") ? OS.linux
        	: OS.unknown)))));
    }
    
    public enum OS {
    	windows,
    	linux,
    	macos,
    	unknown
    }
    
    public static String time(long time) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
        long milliseconds = TimeUnit.MILLISECONDS.toMillis(time);
        return seconds + "s " + milliseconds + "ms";
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

    public static JSONObject parseJSONFile(Path path) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(path));
        return new JSONObject(content);
    }

    public static JSONObject parseJSONFile(InputStream stream) throws JSONException, IOException {
    	byte[] bytes = readAllBytes(stream);
        String content = new String(bytes);
        return new JSONObject(content);
    }

    public static JSONArray parseJSONArray(InputStream stream) throws JSONException, IOException {
    	byte[] bytes = readAllBytes(stream);
        String content = new String(bytes);
        return new JSONArray(content);
    }
    
    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
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
    
    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
