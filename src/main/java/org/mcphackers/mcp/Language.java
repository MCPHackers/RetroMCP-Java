package org.mcphackers.mcp;

import java.util.Locale;

public enum Language {

	ENGLISH("en_us"),
	RUSSIAN("ru_RU"),
	GERMAN("de_DE"),
	FRENCH("fr_FR"),
	CHINESE("zh_CN");
	
	public String name;
	
	private Language(String langName) {
		name = langName;
	}

	public static Language get(Locale locale) {
		for(Language lang : values()) {
			if(lang.name.equals(locale.toString())) {
				return lang;
			}
		}
		return ENGLISH;
	}
}
