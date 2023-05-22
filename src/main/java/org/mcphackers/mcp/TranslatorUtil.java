package org.mcphackers.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslatorUtil {
	public static final Language DEFAULT_LANG = Language.ENGLISH;
	private final Map<String, String> translations = new HashMap<>();
	public Language currentLang;

	public void changeLang(Language lang) {
		translations.clear();
		currentLang = lang;
		readTranslation(MCP.class);
	}

	public void readTranslation(Class<?> cls) {
		readTranslation(cls, DEFAULT_LANG);
		readTranslation(cls, currentLang);
	}

	private void readTranslation(Class<?> cls, Language lang) {
		readTranslation(translations, cls, lang);
	}

	private void readTranslation(Map<String, String> map, Class<?> cls, Language lang) {
		String resourceName = "/lang/" + lang.name + ".lang";
		//FIXME Hardcoded MCP.class because Class#getResourceAsStream return result is not the same as ClassLoader#getResourceAsStream
		InputStream resource = (cls == MCP.class) ? cls.getResourceAsStream(resourceName) : cls.getClassLoader().getResourceAsStream(resourceName);
		if (resource == null) {
			return;
		}
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
			bufferedReader.lines().forEach(line -> {
				if (line.startsWith("#") || line.trim().isEmpty()) return;
				String key = line.split("=")[0].trim();
				String translated = line.split("=")[1].trim();
				map.put(key, translated);
			});
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public String getLangName(Language lang) {
		Map<String, String> entries = new HashMap<>();
		readTranslation(entries, MCP.class, lang);
		String languageName = entries.get("language");
		if (languageName != null) {
			return languageName;
		}
		return "Unknown language";
	}

	public boolean hasKey(String key) {
		return translations.containsKey(key);
	}

	public String translateKey(String key) {
		String translatedString = this.translations.get(key);
		if (translatedString == null) {
			return key;
		}
		return translatedString;
	}

	public String translateKeyWithFormatting(String key, Object... formatting) {
		String translatedString = this.translations.get(key);
		if (translatedString == null) {
			return key;
		}
		return String.format(translatedString, formatting);
	}
}
