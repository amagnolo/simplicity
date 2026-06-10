package micropolisj.gui;

import java.util.prefs.Preferences;

/**
 * Preference for which tile artwork the map uses: the modern full-color
 * art (default) or the original 1980s-style "classic" art, shipped under
 * the classic/ resource directory.
 */
public final class TileSkin
{
	public static final String MODERN = "modern";
	public static final String CLASSIC = "classic";
	static final String PREF_KEY = "tile_skin";
	private static final String [] SUPPORTED_VALUES = {
		MODERN,
		CLASSIC,
	};

	private TileSkin()
	{
	}

	public static String getPreference()
	{
		Preferences prefs = Preferences.userNodeForPackage(TileSkin.class);
		return normalize(prefs.get(PREF_KEY, MODERN));
	}

	public static void setPreference(String value)
	{
		Preferences prefs = Preferences.userNodeForPackage(TileSkin.class);
		prefs.put(PREF_KEY, normalize(value));
	}

	public static String [] getSupportedValues()
	{
		return SUPPORTED_VALUES.clone();
	}

	/** The resource path prefix for a skin's tile sheets. */
	public static String resourcePrefix(String skin)
	{
		return CLASSIC.equals(skin) ? "classic/" : "";
	}

	private static String normalize(String value)
	{
		return CLASSIC.equals(value != null ? value.trim() : null) ? CLASSIC : MODERN;
	}
}
