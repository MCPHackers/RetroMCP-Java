package org.mcphackers.mcp.api.language;

import java.util.HashMap;
import java.util.Map;

public class Language {
	private final String locale;
	private String simplifiedName;
	private final Map<String, String> translations = new HashMap<>();

	public Language (String locale) {
		this.locale = locale;
	}

	public String getLocale() {
		return this.locale;
	}

	public String getSimplifiedName() {
		return this.simplifiedName;
	}

	public Language setSimplifiedName(String simplifiedName) {
		this.simplifiedName = simplifiedName;
		return this;
	}

	public Map<String, String> getTranslations() {
		return this.translations;
	}
}
