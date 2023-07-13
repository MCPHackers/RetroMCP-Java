package org.mcphackers.mcp.api.language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {
	public static final Language DEFAULT_LANGUAGE = new Language("en_US").setSimplifiedName("English");

	static {
		// Read default language
		LanguageManager.readLanguage(DEFAULT_LANGUAGE);
	}

	private Language selectedLanguage = null;

	public static void readLanguage(Language language) {
		try {
			Class<?> callingClass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
			ClassLoader classLoader = callingClass.getClassLoader();
			readLanguage(classLoader, language);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	public static void readLanguage(ClassLoader classLoader, Language language) {
		// TODO: Get locales of the same language, e.g. if es-ES is requested but not found but es-AR exists, use es-AR
		try (InputStream languageStream = classLoader.getResourceAsStream("lang/" + language.getLocale() + ".lang")) {
			if (languageStream != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(languageStream, StandardCharsets.UTF_8))) {
					reader.lines().forEach(line -> {
						if (line.startsWith("#") || line.trim().isEmpty()) return;
						String key = line.split("=")[0].trim();
						String translated = line.split("=")[1].trim();
						language.getTranslations().put(key, translated);
					});
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public static String translate(Language language, String key) {
		String translation = language.getTranslations().get(key);
		if (translation == null) {
			translation = DEFAULT_LANGUAGE.getTranslations().get(key);
			if (translation == null) {
				return key;
			}
		}
		return translation;
	}

	public String translate(String key) {
		if (this.selectedLanguage == null) {
			return translate(DEFAULT_LANGUAGE, key);
		}
		return translate(this.selectedLanguage, key);
	}

	public void setSelectedLanguage(Language selectedLanguage) {
		this.selectedLanguage = selectedLanguage;
	}
}
