package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The player's own ways into Prifddinas, from the travel atlas Codex Wayfinder shares and never
 * written here. Wayfinder saves it in the RuneLite profile as {@code codexwayfinder/atlasV1}:
 * {@code {"version":1,"teleports":[{"name":"Teleport crystal: Prifddinas","x":3264,"y":6066,
 * "plane":0,"seconds":4,"carried":true}],...}}, every teleport the account can use with what it
 * owns. Only those that land in Prifddinas are kept, carried ones first, then the quickest. A
 * missing, damaged or other-version atlas reads as none, and nothing needs it.
 */
public final class WayIn
{
	public static final String GROUP = "codexwayfinder";
	public static final String KEY = "atlasV1";

	/** One teleport into Prifddinas. */
	public static final class Teleport
	{
		private final String name;
		private final int seconds;
		private final boolean carried;

		public Teleport(String name, int seconds, boolean carried)
		{
			this.name = name;
			this.seconds = seconds;
			this.carried = carried;
		}

		/** Wayfinder's label, such as "Teleport crystal: Prifddinas". */
		public String getName()
		{
			return name;
		}

		/** The cast, in seconds, as Wayfinder timed or knows it. */
		public int getSeconds()
		{
			return seconds;
		}

		/** On the player when Wayfinder last looked; otherwise in the bank. */
		public boolean isCarried()
		{
			return carried;
		}
	}

	private final boolean read;
	private final List<Teleport> teleports;

	private WayIn(boolean read, List<Teleport> teleports)
	{
		this.read = read;
		this.teleports = Collections.unmodifiableList(teleports);
	}

	/** No atlas. */
	public static WayIn none()
	{
		return new WayIn(false, new ArrayList<>());
	}

	/** An atlas holding these, for tests and previews. */
	public static WayIn of(List<Teleport> teleports)
	{
		List<Teleport> sorted = new ArrayList<>(teleports);
		sort(sorted);
		return new WayIn(true, sorted);
	}

	/** Whether Wayfinder's atlas was there to read. */
	public boolean isRead()
	{
		return read;
	}

	/** The teleports into Prifddinas: carried first, then the quickest. */
	public List<Teleport> getTeleports()
	{
		return teleports;
	}

	/** Wayfinder's atlas, kept to the teleports that land in Prifddinas. */
	public static WayIn parse(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return none();
		}
		JsonObject o;
		try
		{
			JsonElement e = gson.fromJson(json, JsonElement.class);
			o = e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
		catch (JsonParseException | IllegalStateException | NumberFormatException e)
		{
			return none();
		}
		if (o == null || whole(o.get("version"), -1) != 1)
		{
			return none();
		}
		JsonElement list = o.get("teleports");
		if (list == null || !list.isJsonArray())
		{
			return none();
		}
		List<Teleport> out = new ArrayList<>();
		List<String> seen = new ArrayList<>();
		for (JsonElement e : (JsonArray) list)
		{
			if (e == null || !e.isJsonObject())
			{
				continue;
			}
			JsonObject t = e.getAsJsonObject();
			String name = text(t.get("name"));
			long x = whole(t.get("x"), -1);
			long y = whole(t.get("y"), -1);
			long seconds = whole(t.get("seconds"), 0);
			if (name == null || name.isEmpty() || x <= 0 || y <= 0 || x > 20_000 || y > 20_000 || seen.contains(name))
			{
				continue;
			}
			if (!Zalcano.inPrifddinas(Zalcano.region((int) x, (int) y)))
			{
				continue;
			}
			JsonElement c = t.get("carried");
			boolean carried = c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isBoolean() && c.getAsBoolean();
			seen.add(name);
			out.add(new Teleport(name, (int) Math.max(0, Math.min(3600, seconds)), carried));
		}
		sort(out);
		return new WayIn(true, out);
	}

	private static void sort(List<Teleport> list)
	{
		list.sort((a, b) -> a.carried != b.carried ? (a.carried ? -1 : 1) : Integer.compare(a.seconds, b.seconds));
	}

	private static String text(JsonElement e)
	{
		return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
	}

	private static long whole(JsonElement e, long otherwise)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return otherwise;
		}
		try
		{
			double d = e.getAsDouble();
			return Double.isFinite(d) ? (long) d : otherwise;
		}
		catch (NumberFormatException ex)
		{
			return otherwise;
		}
	}
}
