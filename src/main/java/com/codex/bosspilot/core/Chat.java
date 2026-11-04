package com.codex.bosspilot.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the game's own boss messages, with colour tags already taken out:
 * "Your Sarachnis kill count is: 52." and "Fight duration: 1:23. Personal best: 1:05."
 */
public final class Chat
{
	private static final Pattern KC = Pattern.compile(
		"Your (?:completion count for |subdued |completed )?(.+?) (?:(?:kill|chest|harvest|completion|success) )?(?:count )?is: ?([0-9,]+)\\.?");
	private static final Pattern DURATION = Pattern.compile("(?i)^(?:Fight )?duration: ([0-9:.]+)");

	/** A kill count message, read. */
	public static final class Kc
	{
		public final String name;
		public final int count;

		Kc(String name, int count)
		{
			this.name = name;
			this.count = count;
		}
	}

	private Chat()
	{
	}

	/** The kill count in a message, or null when it is not one. */
	public static Kc kc(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher m = KC.matcher(message.trim());
		if (!m.find())
		{
			return null;
		}
		try
		{
			return new Kc(m.group(1).trim(), Integer.parseInt(m.group(2).replace(",", "")));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/** The fight duration in seconds, or 0 when the message has none. */
	public static double duration(String message)
	{
		if (message == null)
		{
			return 0;
		}
		Matcher m = DURATION.matcher(message.trim());
		return m.find() ? seconds(m.group(1)) : 0;
	}

	/** "1:23" is 83, "1:23.40" is 83.4, "1:02:03" is 3723; 0 when it cannot be read. */
	public static double seconds(String time)
	{
		if (time == null || time.isEmpty())
		{
			return 0;
		}
		String t = time.endsWith(".") ? time.substring(0, time.length() - 1) : time;
		String[] parts = t.split(":");
		double total = 0;
		try
		{
			for (String p : parts)
			{
				total = total * 60 + Double.parseDouble(p);
			}
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
		return total;
	}
}
