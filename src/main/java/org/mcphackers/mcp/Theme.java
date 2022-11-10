package org.mcphackers.mcp;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public enum Theme {
    SWING("swing", UIManager.getCrossPlatformLookAndFeelClassName()),
    FLATLIGHTLAF("flatlightlaf", FlatLightLaf.class.getName()),
    FLATDARKLAF("flatdarklaf", FlatDarkLaf.class.getName()),
    FLATDARCULALAF("flatdarculalaf", FlatDarculaLaf.class.getName());

    public static final Theme[] VALUES = Theme.values();

    public final String themeName;
    public final String themeClass;

    Theme(String themeName, String themeClassName) {
        this.themeName = themeName;
        this.themeClass = themeClassName;
    }
}
