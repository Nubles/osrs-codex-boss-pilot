package com.codex.bosspilot.core;

/**
 * Read access to this account's RuneLite profile config, so the core can be tested without a
 * client. The plugin backs it with {@code ConfigManager.getRSProfileConfiguration(group, key)}.
 * Boss Pilot reads and writes its own keys through it.
 */
public interface Profile
{
	Profile EMPTY = (group, key) -> null;

	/** The stored text, or null when there is none. */
	String get(String group, String key);

	/** Reads a value without letting anything thrown by the store get past. */
	static String safe(Profile p, String group, String key)
	{
		if (p == null)
		{
			return null;
		}
		try
		{
			return p.get(group, key);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}
}
