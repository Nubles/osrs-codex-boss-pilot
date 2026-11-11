package com.codex.bosspilot.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boss Pilot's measured rates, shared with the other Codex apps under the RS profile key
 * {@code codexbosspilot/ratesV1} (contract N10). Gear Roadmap reads it for Payback and kill
 * efficiency, Money Pilot and Autopilot to weigh a boss against other ways to spend an hour, and
 * Annals for the account's story. Worked out only from the player's own logbook.
 *
 * <pre>
 * {"version":1,"savedAt":ms,"bosses":{"Vorkath":{"trips":6,"kills":41,"killsPerHour":24.6,
 *   "killSeconds":{"best":71.4,"median":88.2},"lootPerHour":2950000,"suppliesPerHour":310000,
 *   "foodPerKill":44.0,"dosesPerKill":1.6,"deathsPerHour":0.12,"overheadShare":0.21}}}
 * </pre>
 *
 * <ul>
 * <li>Bosses are keyed by the name the game and the panel give them.</li>
 * <li>Rates are an hour of trip time at the boss, from first sight to last kill or sighting, as the
 * debrief counts them. {@code overheadShare} is the share of that hour spent not fighting, so a
 * reader wanting the rate over the whole hour, banking and walking included, multiplies by
 * {@code 1 - overheadShare}.</li>
 * <li>Gold is whole coins at the prices of the day each trip ended. {@code suppliesPerHour} is the
 * food, potions, runes and ammunition used up (deaths are not in it).</li>
 * <li>-1 means not known: {@code killSeconds} without kills the game timed, {@code suppliesPerHour}
 * from trips saved before supplies were priced, {@code foodPerKill} and {@code dosesPerKill}
 * without kills, {@code overheadShare} without timed kills.</li>
 * <li>A boss appears once it has a trip with a kill. Shape pinned by {@code RatesV1ShapeTest}; a
 * breaking change gets a new key.</li>
 * </ul>
 */
public final class RatesV1
{
	public static final String KEY = "ratesV1";
	public static final int VERSION = 1;
	/** At most this many bosses, those with the most kills first: well under the size other apps expect. */
	static final int MAX_BOSSES = 60;

	public int version = VERSION;
	public long savedAt;
	public Map<String, BossRates> bosses = new LinkedHashMap<>();

	/** One boss's rates. */
	public static final class BossRates
	{
		public int trips;
		public int kills;
		public double killsPerHour;
		public KillSeconds killSeconds = new KillSeconds();
		public long lootPerHour;
		public long suppliesPerHour = -1;
		public double foodPerKill = -1;
		public double dosesPerKill = -1;
		public double deathsPerHour;
		public double overheadShare = -1;
	}

	/** The fastest kill the game has told Boss Pilot of, and the median of the last kills it timed; -1 when not known. */
	public static final class KillSeconds
	{
		public double best = -1;
		public double median = -1;
	}

	/** The rates of every boss with a trip with a kill, from the logbook. */
	public static RatesV1 of(Logbook book, long now)
	{
		RatesV1 out = new RatesV1();
		out.savedAt = now;
		if (book == null)
		{
			return out;
		}
		List<Map.Entry<String, BossRates>> all = new ArrayList<>();
		for (Map.Entry<String, Logbook.Entry> e : book.bosses.entrySet())
		{
			Boss boss = book.boss(e.getKey());
			if (boss == null)
			{
				continue;
			}
			BossRates r = rates(book, boss);
			if (r != null)
			{
				all.add(new java.util.AbstractMap.SimpleEntry<>(boss.getName(), r));
			}
		}
		all.sort((a, b) -> Integer.compare(b.getValue().kills, a.getValue().kills));
		for (Map.Entry<String, BossRates> e : all.subList(0, Math.min(MAX_BOSSES, all.size())))
		{
			out.bosses.put(e.getKey(), e.getValue());
		}
		return out;
	}

	/** One boss's rates, or null when it has no trip with a kill. */
	static BossRates rates(Logbook book, Boss boss)
	{
		Logbook.Rates rates = book.rates(boss);
		if (!rates.isKnown())
		{
			return null;
		}
		Logbook.Entry e = book.peek(boss);
		BossRates r = new BossRates();
		r.trips = rates.trips;
		r.kills = rates.kills;
		r.killsPerHour = round(rates.killsPerHour(), 10);
		r.lootPerHour = rates.lootPerHour();
		r.foodPerKill = round(rates.foodPerKill(), 10);
		r.dosesPerKill = round(rates.dosesPerKill(), 100);
		r.killSeconds.best = e.best > 0 ? round(e.best, 10) : -1;
		double median = book.median(boss);
		r.killSeconds.median = median > 0 ? round(median, 10) : -1;
		double allMinutes = 0;
		int deaths = 0;
		double pricedMinutes = 0;
		long supplies = 0;
		double fight = 0;
		double fightSpan = 0;
		for (Logbook.TripRecord t : e.trips)
		{
			allMinutes += t.minutes();
			deaths += Math.max(0, t.deaths);
			if (t.supplies >= 0 && t.kills > 0)
			{
				pricedMinutes += t.minutes();
				supplies += t.supplies;
			}
			double share = Overhead.share(t);
			if (share >= 0)
			{
				double span = Overhead.span(t);
				fight += span * (1 - share);
				fightSpan += span;
			}
		}
		r.deathsPerHour = allMinutes <= 0 ? 0 : round(deaths * 60 / allMinutes, 100);
		r.suppliesPerHour = pricedMinutes <= 0 ? -1 : Math.round(supplies * 60 / pricedMinutes);
		r.overheadShare = fightSpan <= 0 ? -1 : round(1 - fight / fightSpan, 100);
		return r;
	}

	private static double round(double v, int scale)
	{
		if (v < 0 || Double.isNaN(v) || Double.isInfinite(v))
		{
			return -1;
		}
		return Math.round(v * scale) / (double) scale;
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	/** The same rates with the time left out, to tell whether anything changed since the last save. */
	public String signature(Gson gson)
	{
		long at = savedAt;
		savedAt = 0;
		String s = gson.toJson(this);
		savedAt = at;
		return s;
	}
}
