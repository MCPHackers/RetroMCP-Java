package org.mcphackers.mcp;

import java.util.Locale;

/**
 * All available languages
 */
public enum Language {

	ENGLISH("en_US"),
	RUSSIAN("ru_RU"),
	GERMAN("de_DE"),
	FRENCH("fr_FR"),
	CHINESE("zh_CN");
	
	/**
	 * Internal name
	 */
	public String name;
	
	private Language(String langName) {
		name = langName;
	}

	/**
	 * @param locale
	 * @return A language enum from a locale
	 */
	public static Language get(Locale locale) {
		for(Language lang : values()) {
			if(lang.name.equals(locale.toString())) {
				return lang;
			}
		}
		return ENGLISH;
	}
}
