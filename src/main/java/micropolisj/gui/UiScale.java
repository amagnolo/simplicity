package micropolisj.gui;

import java.util.prefs.Preferences;

public final class UiScale
{
	public static final String AUTO = "auto";
	static final String PREF_KEY = "ui_scale";
	private static final String [] SUPPORTED_VALUES = {
		AUTO,
		"1",
		"1.25",
		"1.5",
		"2",
		"3",
		"4",
		"5",
	};

	private UiScale()
	{
	}

	public static void applySavedScale()
	{
		String scale = getPreference();
		if (!scale.equals(AUTO)) {
			System.setProperty("sun.java2d.uiScale", scale);
		}
	}

	public static String getPreference()
	{
		Preferences prefs = Preferences.userNodeForPackage(UiScale.class);
		return normalize(prefs.get(PREF_KEY, AUTO));
	}

	public static void setPreference(String value)
	{
		Preferences prefs = Preferences.userNodeForPackage(UiScale.class);
		prefs.put(PREF_KEY, normalize(value));
	}

	public static String [] getSupportedValues()
	{
		return SUPPORTED_VALUES.clone();
	}

	public static int getPercentage(String value)
	{
		return (int) Math.round(Double.parseDouble(normalize(value)) * 100.0);
	}

	private static String normalize(String value)
	{
		if (value == null) {
			return AUTO;
		}

		String trimmed = value.trim();
		if (trimmed.equals(AUTO)) {
			return AUTO;
		}

		for (String supported : SUPPORTED_VALUES) {
			if (supported.equals(trimmed)) {
				return supported;
			}
		}

		try {
			double parsed = Double.parseDouble(trimmed);
			for (String supported : SUPPORTED_VALUES) {
				if (!supported.equals(AUTO) &&
					Math.abs(Double.parseDouble(supported) - parsed) < 0.001)
				{
					return supported;
				}
			}
		}
		catch (NumberFormatException e) {
			// fall through to default
		}

		return AUTO;
	}
}
