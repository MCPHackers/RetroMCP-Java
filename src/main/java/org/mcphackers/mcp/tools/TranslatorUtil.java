package org.mcphackers.mcp.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.mcphackers.mcp.MCP;

public class TranslatorUtil {
    private static final TranslatorUtil instance = new TranslatorUtil();
    private static final String defaultLang = "en_US";
    private final Map<String, String> translations = new HashMap<>();
    private String currentLang = Locale.getDefault().toString();
    
    public TranslatorUtil() {
    	changeLang(currentLang);
	}
    
    public void changeLang(String langName) {
    	translations.clear();
    	currentLang = langName;
		readTranslation(MCP.class);
    }

    public void readTranslation(Class cls) {
    	readTranslation(cls, defaultLang);
    	readTranslation(cls, currentLang);
    }

    private void readTranslation(Class cls, String lang) {
    	InputStream resource = cls.getResourceAsStream("/lang/" + lang + ".lang");
    	if(resource == null) {
    		return;
    	}
    	try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            bufferedReader.lines().forEach(line -> {
                if (line.startsWith("#") || line.trim().isEmpty()) return;

                String key = line.split("=")[0].trim();
                String translated = line.split("=")[1].trim();
                this.translations.put(key, translated);
            });
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
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
