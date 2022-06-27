package org.mcphackers.mcp.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.mcphackers.mcp.Language;
import org.mcphackers.mcp.MCP;

public class TranslatorUtil {
    private static final Language defaultLang = Language.ENGLISH;
    private static final TranslatorUtil instance = new TranslatorUtil();
    private final Map<String, String> translations = new HashMap<>();
    private Language currentLang = Language.get(Locale.getDefault());
    
    public TranslatorUtil() {
    	changeLang(currentLang);
	}
    
    public void changeLang(Language lang) {
    	translations.clear();
    	currentLang = lang;
		readTranslation(MCP.class);
    }

    public void readTranslation(Class cls) {
    	readTranslation(cls, defaultLang);
    	readTranslation(cls, currentLang);
    }

    private void readTranslation(Class cls, Language lang) {
    	readTranslation(translations, cls, lang);
    }

    private void readTranslation(Map<String, String> map, Class cls, Language lang) {
    	InputStream resource = cls.getResourceAsStream("/lang/" + lang.name + ".lang");
    	if(resource == null) {
    		return;
    	}
    	try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
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
    	if(languageName != null) {
    		return languageName;
    	}
    	return "Unknown language";
    }

    public static TranslatorUtil getInstance() {
        return instance;
    }

    public boolean hasKey(String key) {
        return translations.containsKey(key);
    }

    public String translateKey(String key) {
        String translatedString = this.translations.get(key);
        if(translatedString == null) {
        	return key;
        }
        return translatedString;
    }

    public String translateKeyWithFormatting(String key, Object... formatting) {
        String translatedString = this.translations.get(key);
        if(translatedString == null) {
        	return key;
        }
        return String.format(translatedString, formatting);
    }
}
