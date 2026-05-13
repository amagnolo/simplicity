package micropolisj.gui;

import java.util.Locale;
import java.util.prefs.Preferences;

public final class AppLocale
{
	public static final String AUTO = "auto";
	static final String PREF_KEY = "locale";
	private static final String [] SUPPORTED_VALUES = {
		AUTO,
		"en",
		"de",
		"fr",
		"it",
		"sv",
	};

	private AppLocale()
	{
	}

	public static void applySavedLocale()
	{
		String value = getPreference();
		if (!value.equals(AUTO)) {
			Locale.setDefault(Locale.forLanguageTag(value));
		}
	}

	public static String getPreference()
	{
		Preferences prefs = Preferences.userNodeForPackage(AppLocale.class);
		return normalize(prefs.get(PREF_KEY, AUTO));
	}

	public static void setPreference(String value)
	{
		Preferences prefs = Preferences.userNodeForPackage(AppLocale.class);
		prefs.put(PREF_KEY, normalize(value));
	}

	public static String [] getSupportedValues()
	{
		return SUPPORTED_VALUES.clone();
	}

	private static String normalize(String value)
	{
		if (value == null) {
			return AUTO;
		}

		String trimmed = value.trim();
		if (trimmed.equalsIgnoreCase(AUTO)) {
			return AUTO;
		}

		String tag = trimmed.replace('_', '-').toLowerCase(Locale.ROOT);
		String language = Locale.forLanguageTag(tag).getLanguage();
		for (String supported : SUPPORTED_VALUES) {
			if (supported.equals(language)) {
				return supported;
			}
		}

		return AUTO;
	}
}
