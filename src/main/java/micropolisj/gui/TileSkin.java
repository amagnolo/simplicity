package micropolisj.gui;

import java.util.prefs.Preferences;

/**
 * Preference for which tile artwork the map uses: the original
 * 1980s-style "classic" art (default), the modern full-color art, or
 * the fully redrawn "deluxe" art; classic and deluxe ship under the
 * classic/ and deluxe/ resource directories.
 */
public final class TileSkin
{
	public static final String CLASSIC = "classic";
	public static final String MODERN = "modern";
	public static final String DELUXE = "deluxe";
	static final String PREF_KEY = "tile_skin";
	private static final String [] SUPPORTED_VALUES = {
		CLASSIC,
		MODERN,
		DELUXE,
	};

	private TileSkin()
	{
	}

	public static String getPreference()
	{
		Preferences prefs = Preferences.userNodeForPackage(TileSkin.class);
		return normalize(prefs.get(PREF_KEY, CLASSIC));
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
		return MODERN.equals(skin) ? "" : skin + "/";
	}

	private static String normalize(String value)
	{
		String v = value != null ? value.trim() : "";
		return MODERN.equals(v) ? MODERN :
			DELUXE.equals(v) ? DELUXE : CLASSIC;
	}
}
