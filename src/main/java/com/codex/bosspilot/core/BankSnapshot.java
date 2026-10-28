package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A look at the bank by item id, in the shape Codex Bank Steward shares it under
 * {@code codexbanksteward/bankV1}:
 * {@code {"version":1,"savedAt":1760000000000,"items":{"995":1234567,"1511":40},"truncated":false}}.
 * Boss Pilot saves its own in the same shape under {@code codexbosspilot/bankV1} whenever the bank
 * is open, and reads Steward's only when it has none of its own. A missing, damaged or newer
 * version reads as no bank at all.
 */
public final class BankSnapshot
{
	public static final String GROUP = "codexbanksteward";
	public static final String KEY = "bankV1";

	private final Map<Integer, Long> items;
	private final long savedAt;
	private final boolean truncated;

	BankSnapshot(Map<Integer, Long> items, long savedAt, boolean truncated)
	{
		this.items = Collections.unmodifiableMap(items);
		this.savedAt = Math.max(0, savedAt);
		this.truncated = truncated;
	}

	/** A bank holding exactly these, for tests and previews. */
	public static BankSnapshot of(Map<Integer, Long> items, long savedAt)
	{
		return new BankSnapshot(new HashMap<>(items), savedAt, false);
	}

	public static BankSnapshot read(Profile profile, Gson gson)
	{
		return parse(gson, Profile.safe(profile, GROUP, KEY));
	}

	/** The snapshot, or null when there is none, it is damaged, or its version is newer than this reader. */
	public static BankSnapshot parse(Gson gson, String json)
	{
		if (gson == null || json == null || json.trim().isEmpty())
		{
			return null;
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonObject())
			{
				return null;
			}
			JsonObject o = root.getAsJsonObject();
			if (version(o) != 1)
			{
				return null;
			}
			JsonElement list = o.get("items");
			if (list == null || !list.isJsonObject())
			{
				return null;
			}
			Map<Integer, Long> items = new HashMap<>();
			for (Map.Entry<String, JsonElement> e : list.getAsJsonObject().entrySet())
			{
				int id;
				try
				{
					id = Integer.parseInt(e.getKey().trim());
				}
				catch (NumberFormatException ex)
				{
					continue;
				}
				long qty = number(e.getValue(), 0);
				if (id >= 0 && qty > 0)
				{
					// No bank stack passes the integer cap; a bigger figure is damage, held there.
					items.merge(id, Math.min(qty, Integer.MAX_VALUE), (a, b) -> Math.min(Integer.MAX_VALUE, a + b));
				}
			}
			JsonElement t = o.get("truncated");
			boolean truncated = t != null && t.isJsonPrimitive() && t.getAsJsonPrimitive().isBoolean() && t.getAsBoolean();
			return new BankSnapshot(items, number(o.get("savedAt"), 0), truncated);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/** The saved version: 1 when missing (the first writers left it out), -1 when it is not a number. */
	static long version(JsonObject o)
	{
		JsonElement v = o.get("version");
		if (v == null || v.isJsonNull())
		{
			return 1;
		}
		return v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? number(v, -1) : -1;
	}

	/** A whole number from a JSON value, or the fallback when it is missing or not a finite number. */
	static long number(JsonElement e, long fallback)
	{
		if (e == null || !e.isJsonPrimitive())
		{
			return fallback;
		}
		JsonPrimitive p = e.getAsJsonPrimitive();
		if (!p.isNumber())
		{
			return fallback;
		}
		double d = p.getAsDouble();
		if (Double.isNaN(d) || Double.isInfinite(d))
		{
			return fallback;
		}
		if (d >= Long.MAX_VALUE)
		{
			return Long.MAX_VALUE;
		}
		if (d <= Long.MIN_VALUE)
		{
			return Long.MIN_VALUE;
		}
		return (long) d;
	}

	/** How many of any of these items are banked. */
	public long count(int... ids)
	{
		long n = 0;
		for (int id : ids)
		{
			n += items.getOrDefault(id, 0L);
		}
		return n;
	}

	/** Unix milliseconds the bank was last seen, or 0. */
	public long getSavedAt()
	{
		return savedAt;
	}

	/** True when Steward could not save every item, so a missing one may still be there. */
	public boolean isTruncated()
	{
		return truncated;
	}

	/** The items by id. */
	public Map<Integer, Long> getItems()
	{
		return items;
	}

	public int size()
	{
		return items.size();
	}
}
