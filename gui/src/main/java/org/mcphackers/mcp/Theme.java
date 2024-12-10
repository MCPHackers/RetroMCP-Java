package org.mcphackers.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class Theme {
	public static final Map<String, Theme> THEMES_MAP = new HashMap<>();
	public static final List<Theme> THEMES = new ArrayList<>();

	static {
		for (LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
			addTheme(laf.getName(), laf.getClassName());
		}
		addTheme(FlatLightLaf.NAME, FlatLightLaf.class.getName());
		addTheme(FlatDarkLaf.NAME, FlatDarkLaf.class.getName());
		addTheme(FlatIntelliJLaf.NAME, FlatIntelliJLaf.class.getName());
		addTheme(FlatDarculaLaf.NAME, FlatDarculaLaf.class.getName());
	}

	public final String themeName;
	public final String themeClass;

	Theme(String themeName, String themeClassName) {
		this.themeName = themeName;
		this.themeClass = themeClassName;
	}

	public static void addTheme(String name, String className) {
		Theme theme = new Theme(name, className);
		THEMES_MAP.put(className, theme);
		THEMES.add(theme);
	}
}
