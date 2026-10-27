package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One boss as Boss Pilot plans for it: what it takes to get in, what a comfortable account looks
 * like, the kit to bring, how the game names it in chat and loot, and short notes to read before
 * going. Nothing here describes what the boss does next in a fight; the notes are the kind of
 * thing a wiki page says up front.
 */
public final class Boss
{
	/** Where a boss sits in an account's progress. */
	public enum Tier
	{
		EARLY("Early"),
		MID("Mid"),
		SLAYER("Slayer"),
		GOD_WARS("God Wars"),
		WILDERNESS("Wilderness"),
		LATE("Late");

		private final String label;

		Tier(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final String key;
	private final String name;
	private final Tier tier;
	private final String where;
	private final String travel;
	private final String attacks;
	private final List<String> notes;
	private final List<String> npcNames;
	private final List<String> kcNames;
	private final List<String> targets;
	private final List<String> quests;
	private final Map<String, Integer> skills;
	private final List<String> tasks;
	private final int suggestedCombat;
	private final List<Supply> kit;
	private final boolean members;
	private final boolean wilderness;
	private final String lootEvent;

	private Boss(Builder b)
	{
		key = b.key;
		name = b.name;
		tier = b.tier;
		where = b.where;
		travel = b.travel;
		attacks = b.attacks;
		notes = Collections.unmodifiableList(new ArrayList<>(b.notes));
		npcNames = Collections.unmodifiableList(new ArrayList<>(b.npcNames));
		kcNames = Collections.unmodifiableList(new ArrayList<>(b.kcNames));
		targets = Collections.unmodifiableList(new ArrayList<>(b.targets));
		quests = Collections.unmodifiableList(new ArrayList<>(b.quests));
		skills = Collections.unmodifiableMap(new LinkedHashMap<>(b.skills));
		tasks = Collections.unmodifiableList(new ArrayList<>(b.tasks));
		suggestedCombat = b.suggestedCombat;
		kit = Collections.unmodifiableList(new ArrayList<>(b.kit));
		members = b.members;
		wilderness = b.wilderness;
		lootEvent = b.lootEvent;
	}

	public static Builder builder(String key, String name, Tier tier)
	{
		return new Builder(key, name, tier);
	}

	/** A stable id for saved data; never shown. */
	public String getKey()
	{
		return key;
	}

	public String getName()
	{
		return name;
	}

	public Tier getTier()
	{
		return tier;
	}

	public String getWhere()
	{
		return where;
	}

	public String getTravel()
	{
		return travel;
	}

	/** The kinds of attack it uses, as a wiki infobox lists them. */
	public String getAttacks()
	{
		return attacks;
	}

	public List<String> getNotes()
	{
		return notes;
	}

	/** The NPC names whose sighting, loot and death belong to this boss. */
	public List<String> getNpcNames()
	{
		return npcNames;
	}

	/** The names the game uses in "Your ... kill count is" messages. */
	public List<String> getKcNames()
	{
		return kcNames;
	}

	/** The monsters in the combat data that the best setup is worked out against. */
	public List<String> getTargets()
	{
		return targets;
	}

	/** Quests that must be finished, by RuneLite's quest names such as "REGICIDE". */
	public List<String> getQuests()
	{
		return quests;
	}

	/** Levels that must be met, by skill name such as "Slayer". */
	public Map<String, Integer> getSkills()
	{
		return skills;
	}

	/** Slayer tasks that let the player in, by words in the task name; empty when no task is needed. */
	public List<String> getTasks()
	{
		return tasks;
	}

	public boolean needsTask()
	{
		return !tasks.isEmpty();
	}

	/** The combat level most players are comfortable at; a guide, never a lock. */
	public int getSuggestedCombat()
	{
		return suggestedCombat;
	}

	public List<Supply> getKit()
	{
		return kit;
	}

	public boolean isMembers()
	{
		return members;
	}

	public boolean isWilderness()
	{
		return wilderness;
	}

	/** The loot event name for bosses whose loot comes from a chest, such as "Barrows", or null. */
	public String getLootEvent()
	{
		return lootEvent;
	}

	public boolean isNpc(String npcName)
	{
		return npcName != null && contains(npcNames, npcName);
	}

	public boolean isKc(String kcName)
	{
		return kcName != null && contains(kcNames, kcName);
	}

	/** Whether this task name lets the player fight the boss. */
	public boolean taskCovers(String taskName)
	{
		if (taskName == null)
		{
			return false;
		}
		String t = taskName.toLowerCase(Locale.ROOT);
		for (String want : tasks)
		{
			if (t.contains(want.toLowerCase(Locale.ROOT)))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean contains(List<String> list, String value)
	{
		for (String s : list)
		{
			if (s.equalsIgnoreCase(value.trim()))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static final class Builder
	{
		private final String key;
		private final String name;
		private final Tier tier;
		private String where = "";
		private String travel = "";
		private String attacks = "";
		private final List<String> notes = new ArrayList<>();
		private final List<String> npcNames = new ArrayList<>();
		private final List<String> kcNames = new ArrayList<>();
		private final List<String> targets = new ArrayList<>();
		private final List<String> quests = new ArrayList<>();
		private final Map<String, Integer> skills = new LinkedHashMap<>();
		private final List<String> tasks = new ArrayList<>();
		private int suggestedCombat;
		private final List<Supply> kit = new ArrayList<>();
		private boolean members = true;
		private boolean wilderness;
		private String lootEvent;

		private Builder(String key, String name, Tier tier)
		{
			this.key = key;
			this.name = name;
			this.tier = tier;
			npcNames.add(name);
			kcNames.add(name);
		}

		public Builder where(String where, String travel)
		{
			this.where = where;
			this.travel = travel;
			return this;
		}

		public Builder attacks(String attacks)
		{
			this.attacks = attacks;
			return this;
		}

		public Builder notes(String... notes)
		{
			this.notes.addAll(Arrays.asList(notes));
			return this;
		}

		/** The NPC names, replacing the boss's own name. */
		public Builder npcs(String... names)
		{
			npcNames.clear();
			npcNames.addAll(Arrays.asList(names));
			return this;
		}

		/** The kill count names, replacing the boss's own name. */
		public Builder kc(String... names)
		{
			kcNames.clear();
			kcNames.addAll(Arrays.asList(names));
			return this;
		}

		public Builder targets(String... names)
		{
			targets.addAll(Arrays.asList(names));
			return this;
		}

		public Builder quest(String quest)
		{
			quests.add(quest);
			return this;
		}

		public Builder skill(String skill, int level)
		{
			skills.put(skill, level);
			return this;
		}

		public Builder task(String... words)
		{
			tasks.addAll(Arrays.asList(words));
			return this;
		}

		public Builder suggested(int combat)
		{
			suggestedCombat = combat;
			return this;
		}

		public Builder kit(Supply... lines)
		{
			kit.addAll(Arrays.asList(lines));
			return this;
		}

		public Builder free()
		{
			members = false;
			return this;
		}

		public Builder wilderness()
		{
			wilderness = true;
			return this;
		}

		public Builder lootEvent(String name)
		{
			lootEvent = name;
			return this;
		}

		public Boss build()
		{
			return new Boss(this);
		}
	}
}
