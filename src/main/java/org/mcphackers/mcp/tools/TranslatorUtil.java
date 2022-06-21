package org.mcphackers.mcp.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TranslatorUtil {
    private static final TranslatorUtil instance = new TranslatorUtil();
    private final Map<String, String> translations = new HashMap<>();

    private TranslatorUtil() {
        try {
            // Try load current locale
            readTranslation(Locale.getDefault().toString());
        } catch (IOException ex) {
            // Default to en_US
            try {
                readTranslation("en_US");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void readTranslation(String translationName) throws IOException {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/lang/" + translationName + ".lang"), StandardCharsets.UTF_8));
            bufferedReader.lines().forEach(line -> {
                if (line.startsWith("#") || line.trim().isEmpty()) return;

                String key = line.split("=")[0].trim();
                String translated = line.split("=")[1].trim();
                this.translations.put(key, translated);
            });
            bufferedReader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static TranslatorUtil getInstance() {
        return instance;
    }

    public String translateKey (String key) {
        return translations.get(key);
    }

    public String translateKeyWithFormatting(String key, Object... formatting) {
        String translatedString = this.translations.get(key);
        return String.format(translatedString, formatting);
    }
}
