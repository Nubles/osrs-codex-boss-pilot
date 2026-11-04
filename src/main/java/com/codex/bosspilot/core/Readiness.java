package com.codex.bosspilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Whether the account can fight a boss now, and if not, the first things in the way. Hard
 * requirements (quests, levels, a slayer task, a must-have item) lock a boss; being under the
 * suggested combat level only makes it a stretch.
 */
public final class Readiness
{
	public enum Status
	{
		/** Everything is met and the must-have kit is owned. */
		READY("Ready"),
		/** Open, but below the combat level most players are comfortable at. */
		STRETCH("A stretch"),
		/** Open apart from the must-have kit, such as a key or a dragonfire shield. */
		NEEDS_KIT("Needs kit"),
		/** Open, but only on a slayer task the player does not have. */
		NEEDS_TASK("Needs a task"),
		/** A quest or level is missing. */
		LOCKED("Locked");

		private final String label;

		Status(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}

		/** Whether the player can go and fight it now. */
		public boolean isOpen()
		{
			return this == READY || this == STRETCH;
		}
	}

	private final Boss boss;
	private final Status status;
	private final List<String> missing;

	private Readiness(Boss boss, Status status, List<String> missing)
	{
		this.boss = boss;
		this.status = status;
		this.missing = Collections.unmodifiableList(missing);
	}

	public static Readiness of(Boss boss, Account account, KitCheck kit)
	{
		List<String> locks = new ArrayList<>();
		if (boss.isMembers() && !account.isMembers())
		{
			locks.add("A members world");
		}
		for (String quest : boss.getQuests())
		{
			if (!account.finished(quest))
			{
				locks.add("Quest: " + questName(quest));
			}
		}
		for (Map.Entry<String, Integer> e : boss.getSkills().entrySet())
		{
			int have = account.level(e.getKey());
			if (have < e.getValue())
			{
				locks.add(e.getValue() + " " + e.getKey() + " (you have " + have + ")");
			}
		}
		if (!locks.isEmpty())
		{
			return new Readiness(boss, Status.LOCKED, locks);
		}
		if (boss.needsTask() && !boss.taskCovers(account.getTask()))
		{
			return new Readiness(boss, Status.NEEDS_TASK, Collections.singletonList("A " + taskWord(boss) + " task"));
		}
		if (kit != null)
		{
			List<String> kitMissing = new ArrayList<>();
			for (KitCheck.Line l : kit.missingMust())
			{
				kitMissing.add(l.getSupply().getLabel());
			}
			if (!kitMissing.isEmpty())
			{
				return new Readiness(boss, Status.NEEDS_KIT, kitMissing);
			}
		}
		if (account.getCombat() < boss.getSuggestedCombat())
		{
			return new Readiness(boss, Status.STRETCH,
				Collections.singletonList("Combat " + boss.getSuggestedCombat() + " suggested (you are " + account.getCombat() + ")"));
		}
		return new Readiness(boss, Status.READY, Collections.emptyList());
	}

	public Boss getBoss()
	{
		return boss;
	}

	public Status getStatus()
	{
		return status;
	}

	/** What is in the way, first thing first; empty when ready. */
	public List<String> getMissing()
	{
		return missing;
	}

	/** "DRAGON_SLAYER_II" reads "Dragon Slayer II". */
	public static String questName(String quest)
	{
		StringBuilder out = new StringBuilder();
		for (String word : quest.toLowerCase(Locale.ROOT).split("_"))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (out.length() > 0)
			{
				out.append(' ');
			}
			if (word.matches("i+|iv|v|vi"))
			{
				out.append(word.toUpperCase(Locale.ROOT));
			}
			else if (out.length() > 0 && (word.equals("in") || word.equals("of") || word.equals("the")))
			{
				out.append(word);
			}
			else
			{
				out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}
		return out.toString();
	}

	private static String taskWord(Boss boss)
	{
		return boss.getTasks().get(0);
	}
}
