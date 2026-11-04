package com.codex.bosspilot.core;

import java.util.Locale;

/**
 * Text for numbers and times, the way the whole Codex suite writes them: full numbers under
 * 10,000, whole thousands (k) under a million, millions (M) with one decimal and billions (B)
 * with two. Figures are cut down, never rounded up, so 999,999 reads 999k and not 1000k.
 */
public final class Format
{
	private Format()
	{
	}

	public static String number(long n)
	{
		if (n < 0)
		{
			return "-" + number(n == Long.MIN_VALUE ? Long.MAX_VALUE : -n);
		}
		if (n < 10_000)
		{
			return String.format(Locale.ROOT, "%,d", n);
		}
		if (n < 1_000_000)
		{
			return (n / 1000) + "k";
		}
		if (n < 1_000_000_000)
		{
			return String.format(Locale.ROOT, "%d.%dM", n / 1_000_000, (n % 1_000_000) / 100_000);
		}
		return String.format(Locale.ROOT, "%d.%02dB", n / 1_000_000_000, (n % 1_000_000_000) / 10_000_000);
	}

	/** "7,000 coins", "840k coins", "1.2M coins". */
	public static String coins(long n)
	{
		return number(n) + " coins";
	}

	/** "now", "4 min", "1 hr 20 min", "2 days 3 hr". */
	public static String duration(long seconds)
	{
		if (seconds <= 0)
		{
			return "now";
		}
		long mins = (seconds + 59) / 60;
		if (mins < 60)
		{
			return mins + " min";
		}
		long hrs = mins / 60;
		long rem = mins % 60;
		if (hrs < 24)
		{
			return rem == 0 ? hrs + " hr" : hrs + " hr " + rem + " min";
		}
		long days = hrs / 24;
		long h = hrs % 24;
		return (days == 1 ? "1 day" : days + " days") + (h == 0 ? "" : " " + h + " hr");
	}

	/** A short duration for tight spaces: "4m", "1h 20m", "2d 3h". */
	public static String shortDuration(long seconds)
	{
		if (seconds <= 0)
		{
			return "now";
		}
		long mins = (seconds + 59) / 60;
		if (mins < 60)
		{
			return mins + "m";
		}
		long hrs = mins / 60;
		long rem = mins % 60;
		if (hrs < 24)
		{
			return rem == 0 ? hrs + "h" : hrs + "h " + rem + "m";
		}
		long days = hrs / 24;
		long h = hrs % 24;
		return h == 0 ? days + "d" : days + "d " + h + "h";
	}

	/** "1 house", "4 houses". */
	public static String plural(long n, String one, String many)
	{
		return number(n) + " " + (n == 1 ? one : many);
	}

	/** A kill time: "0:48", "1:23", "12:05", or "1:02:03" past an hour. */
	public static String clock(double seconds)
	{
		long s = Math.max(0, Math.round(seconds));
		long h = s / 3600;
		long m = (s % 3600) / 60;
		long sec = s % 60;
		return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) : String.format(Locale.ROOT, "%d:%02d", m, sec);
	}
}
