package org.mcphackers.mcp;

import java.util.Locale;

/**
 * All available languages
 */
public enum Language {

	ENGLISH(Locale.US),
	SPANISH(new Locale("es", "ES")),
	RUSSIAN(new Locale("ru", "RU")),
	GERMAN(new Locale("de", "DE")),
	FRENCH(new Locale("fr", "FR")),
	CHINESE(new Locale("zh", "CN")),
	CZECH(new Locale("cs", "CZ")),
	NORSK_BOKMAL(new Locale("nb", "NO"));

	/**
	 * Internal locale
	 */
	public final Locale locale;

	Language(Locale locale) {
		this.locale = locale;
	}

	/**
	 * @param locale
	 * @return A language enum from a locale
	 */
	public static Language get(Locale locale) {
		for (Language lang : Language.values()) {
			// Perfect match
			if (lang.locale.equals(locale)) {
				return lang;
			}

			// Language match
			if (lang.locale.getLanguage().equals(locale.getLanguage())) {
				return lang;
			}
		}

		// No matches found for locale
		return ENGLISH;
	}
}
