package com.codex.bosspilot.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the doses or charges at the end of an item name, as in "Prayer potion(3)" or "Ring of dueling(8)". */
public final class Dose
{
	private static final Pattern SUFFIX = Pattern.compile("^(.*?)\\s*\\((\\d)\\)$");

	private Dose()
	{
	}

	/** The doses in the name, or 0 when it has none. */
	public static int of(String itemName)
	{
		if (itemName == null)
		{
			return 0;
		}
		Matcher m = SUFFIX.matcher(itemName.trim());
		return m.matches() ? Integer.parseInt(m.group(2)) : 0;
	}

	/** The name without its doses: "Prayer potion(3)" is "Prayer potion". */
	public static String base(String itemName)
	{
		if (itemName == null)
		{
			return "";
		}
		Matcher m = SUFFIX.matcher(itemName.trim());
		return m.matches() ? m.group(1) : itemName.trim();
	}
}
