package com.codex.bosspilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * What Boss Pilot knows of the player's charged items, kept per account: the charges the game
 * last said each one had, the kills since, and what those kills are thought to have used. Each
 * new check from the game sets an item straight, and the difference between what was thought
 * and what the game says teaches Boss Pilot the player's own pace at that boss, so the next
 * estimate is the player's, not a formula's.
 */
public final class ChargeBook
{
	/** RS-profile config key it is saved under. */
	public static final String KEY = "chargesV1";
	static final int VERSION = 1;
	/** How much a new check moves a learnt pace: half way to what it saw. */
	static final double LEARN = 0.5;

	/** One item as last checked. Saved as JSON: never rename a field. */
	public static final class Known
	{
		/** What the game said, a value for each pool, -1 where it did not say. */
		public int[] checked;
		/** When, in Unix milliseconds. */
		public long at;
		/** Kills with it carried since then. */
		public int kills;
		/** What those kills are thought to have used, for each pool. */
		public double[] used;
		/** Kills since then for which no use could be worked out, so the estimate is short of them. */
		public int blind;
		/** The boss the kills since were at, or null; "*" when at more than one. */
		public String boss;
	}

	/** A pace learnt from the player's own checks, a pool's use a kill. Saved as JSON. */
	public static final class Pace
	{
		public double[] perKill;
		public int samples;
	}

	/** What an item has now, as best known. */
	public static final class Estimate
	{
		private final int[] checked;
		private final double[] left;
		private final int kills;
		private final int blind;
		private final long at;

		Estimate(int[] checked, double[] left, int kills, int blind, long at)
		{
			this.checked = checked;
			this.left = left;
			this.kills = kills;
			this.blind = blind;
			this.at = at;
		}

		/** What the game last said, for each pool, -1 where it did not. */
		public int[] getChecked()
		{
			return checked.clone();
		}

		/** What is thought left now, for each pool, -1 where not known. */
		public double[] getLeft()
		{
			return left.clone();
		}

		/** Kills since the last check. */
		public int getKills()
		{
			return kills;
		}

		/** Kills since the last check whose use is not counted, so the figure may be high. */
		public int getBlind()
		{
			return blind;
		}

		public long getAt()
		{
			return at;
		}

		/** Whether it is what the game said, with no kills since. */
		public boolean isExact()
		{
			return kills == 0;
		}
	}

	public int version = VERSION;
	public Map<String, Known> items = new HashMap<>();
	/** Learnt paces by boss key, "/", item key. */
	public Map<String, Pace> paces = new HashMap<>();

	public static ChargeBook parse(Gson gson, String json)
	{
		if (gson == null || json == null || json.trim().isEmpty())
		{
			return new ChargeBook();
		}
		try
		{
			ChargeBook b = gson.fromJson(json, ChargeBook.class);
			if (b == null)
			{
				return new ChargeBook();
			}
			if (b.items == null)
			{
				b.items = new HashMap<>();
			}
			if (b.paces == null)
			{
				b.paces = new HashMap<>();
			}
			return b;
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return new ChargeBook();
		}
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/**
	 * The game said what an item has. When the kills since the last check were all at one boss
	 * and nothing was added, what they really used becomes the pace learnt for that boss.
	 */
	public void read(Charged item, int[] values, long now)
	{
		if (item == null || values == null || values.length != item.getPools().size())
		{
			return;
		}
		Known old = items.get(item.getKey());
		int[] checked = values.clone();
		if (old != null && old.checked != null && old.checked.length == checked.length)
		{
			for (int i = 0; i < checked.length; i++)
			{
				if (checked[i] < 0 && old.checked[i] >= 0)
				{
					// The message did not say this pool: carry the old one forward as it stands.
					checked[i] = (int) Math.max(0, Math.round(old.checked[i] - old.used[i]));
				}
			}
			learn(item, old, checked);
		}
		Known k = new Known();
		k.checked = checked;
		k.at = now;
		k.used = new double[checked.length];
		items.put(item.getKey(), k);
	}

	private void learn(Charged item, Known old, int[] now)
	{
		if (old.kills <= 0 || old.blind > 0 || old.boss == null || "*".equals(old.boss))
		{
			return;
		}
		double[] perKill = new double[now.length];
		boolean any = false;
		for (int i = 0; i < now.length; i++)
		{
			if (old.checked[i] < 0 || now[i] < 0)
			{
				perKill[i] = -1;
				continue;
			}
			int used = old.checked[i] - now[i];
			if (used < 0)
			{
				// Topped up since: what was used cannot be told.
				return;
			}
			if (now[i] == 0)
			{
				// It ran dry part way: the kills since did not all have it.
				return;
			}
			perKill[i] = (double) used / old.kills;
			any = true;
		}
		if (!any)
		{
			return;
		}
		String key = paceKey(old.boss, item);
		Pace p = paces.get(key);
		if (p == null || p.perKill == null || p.perKill.length != perKill.length)
		{
			p = new Pace();
			p.perKill = perKill;
		}
		else
		{
			for (int i = 0; i < perKill.length; i++)
			{
				if (perKill[i] >= 0)
				{
					p.perKill[i] = p.perKill[i] < 0 ? perKill[i] : p.perKill[i] + (perKill[i] - p.perKill[i]) * LEARN;
				}
			}
		}
		p.samples++;
		paces.put(key, p);
	}

	/**
	 * A kill with the item carried.
	 *
	 * @param perKill what the kill is thought to have used of each pool, or null when not known
	 */
	public void kill(String bossKey, Charged item, double[] perKill)
	{
		Known k = items.get(item.getKey());
		if (k == null)
		{
			return;
		}
		k.kills++;
		k.boss = k.boss == null || k.boss.equals(bossKey) ? bossKey : "*";
		if (perKill == null || perKill.length != k.used.length)
		{
			k.blind++;
			return;
		}
		for (int i = 0; i < perKill.length; i++)
		{
			k.used[i] += Math.max(0, perKill[i]);
		}
	}

	/** What the item has now, as best known, or null before the game has said. */
	public Estimate estimate(Charged item)
	{
		Known k = items.get(item.getKey());
		if (k == null || k.checked == null)
		{
			return null;
		}
		double[] left = new double[k.checked.length];
		for (int i = 0; i < left.length; i++)
		{
			left[i] = k.checked[i] < 0 ? -1 : Math.max(0, k.checked[i] - (k.used == null || i >= k.used.length ? 0 : k.used[i]));
		}
		return new Estimate(k.checked.clone(), left, k.kills, k.blind, k.at);
	}

	/** The pace learnt for this item at this boss, a value a kill for each pool (-1 unknown), or null. */
	public double[] pace(String bossKey, Charged item)
	{
		Pace p = paces.get(paceKey(bossKey, item));
		return p == null || p.perKill == null ? null : p.perKill.clone();
	}

	static String paceKey(String bossKey, Charged item)
	{
		return bossKey + "/" + item.getKey();
	}
}
