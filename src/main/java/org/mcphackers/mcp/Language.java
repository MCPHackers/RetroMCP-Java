package org.mcphackers.mcp;

import java.util.Locale;

/**
 * All available languages
 */
public enum Language {

	ENGLISH("en_US"),
	SPANISH("es_ES"),
	RUSSIAN("ru_RU"),
	GERMAN("de_DE"),
	FRENCH("fr_FR"),
	CHINESE("zh_CN"),
	CZECH("cs_CZ"),
	NORSK_BOKMAL("nb_NO");

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
		for (Language lang : Language.values()) {
			if (lang.name.equals(locale.toString())) {
				return lang;
			}
		}
		return ENGLISH;
	}
}
