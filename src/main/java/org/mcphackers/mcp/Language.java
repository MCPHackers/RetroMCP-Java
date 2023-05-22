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
	CHINESE("zh_CN"),
	CZECH("cs_CZ");

	public static final Language[] VALUES = Language.values();

	/**
	 * Internal name
	 */
	public final String name;

	Language(String langName) {
		name = langName;
	}

	/**
	 * @param locale
	 * @return A language enum from a locale
	 */
	public static Language get(Locale locale) {
		for (Language lang : VALUES) {
			if (lang.name.equals(locale.toString())) {
				return lang;
			}
		}
		return ENGLISH;
	}
}
