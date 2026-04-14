package com.group5.paul_esys.utils;

import java.awt.Window;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_THEME_CLASS_NAME = "selectedThemeClassName";
    private static final String DEFAULT_THEME_CLASS_NAME = "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme";

    private static final List<ThemeOption> THEME_OPTIONS = List.of(
        new ThemeOption("Flat Light", "com.formdev.flatlaf.FlatLightLaf"),
        new ThemeOption("IntelliJ Light", "com.formdev.flatlaf.FlatIntelliJLaf"),
        new ThemeOption("GitHub Light", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme"),
        new ThemeOption("Arc", "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme")
    );

    private ThemeManager() {
    }

    public static List<ThemeOption> getThemeOptions() {
        return THEME_OPTIONS;
    }

    public static ThemeOption getSavedThemeOption() {
        String className = getSavedThemeClassName();
        for (ThemeOption option : THEME_OPTIONS) {
            if (option.getClassName().equals(className)) {
                return option;
            }
        }

        return THEME_OPTIONS.getFirst();
    }

    public static String getSavedThemeClassName() {
        String saved = PREFS.get(PREF_THEME_CLASS_NAME, DEFAULT_THEME_CLASS_NAME);
        return isSupportedTheme(saved) ? saved : DEFAULT_THEME_CLASS_NAME;
    }

    public static boolean applySavedTheme() {
        return applyThemeInternal(getSavedThemeClassName(), false);
    }

    public static boolean applyTheme(String className) {
        return applyThemeInternal(className, true);
    }

    private static synchronized boolean applyThemeInternal(String className, boolean persistSelection) {
        String resolvedClassName = isSupportedTheme(className)
            ? className
            : DEFAULT_THEME_CLASS_NAME;

        try {
            UIManager.setLookAndFeel(resolvedClassName);
            if (persistSelection) {
                PREFS.put(PREF_THEME_CLASS_NAME, resolvedClassName);
            }
            refreshAllWindows();
            return true;
        } catch (Exception ex) {
            logger.error("Unable to apply look and feel: {}", resolvedClassName, ex);
            return false;
        }
    }

    private static boolean isSupportedTheme(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }

        for (ThemeOption option : THEME_OPTIONS) {
            if (option.getClassName().equals(className)) {
                return true;
            }
        }

        return false;
    }

    public static void refreshAllWindows() {
        if (SwingUtilities.isEventDispatchThread()) {
            refreshAllWindowsInternal();
            return;
        }

        SwingUtilities.invokeLater(ThemeManager::refreshAllWindowsInternal);
    }

    private static void refreshAllWindowsInternal() {
        for (Window window : Window.getWindows()) {
            if (!window.isDisplayable()) {
                continue;
            }

            SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }
}
