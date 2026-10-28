package com.codex.bosspilot.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What the game says about the player that decides which bosses are open: real skill levels,
 * finished quests, combat level, the slayer task and whether the world is a members world.
 */
public final class Account
{
	public static final Account UNKNOWN = new Account(Collections.emptyMap(), Collections.emptySet(), 0, null, true, false);

	private final Map<String, Integer> levels;
	private final Set<String> quests;
	private final int combat;
	private final String task;
	private final boolean members;
	private final boolean known;

	private Account(Map<String, Integer> levels, Set<String> quests, int combat, String task, boolean members, boolean known)
	{
		this.levels = levels;
		this.quests = quests;
		this.combat = combat;
		this.task = task;
		this.members = members;
		this.known = known;
	}

	/**
	 * @param levels real levels by skill name, such as "Slayer"
	 * @param quests finished quests by RuneLite's quest names, such as "REGICIDE"
	 * @param task the current slayer task's name, or null
	 */
	public static Account of(Map<String, Integer> levels, Set<String> quests, int combat, String task, boolean members)
	{
		return new Account(Collections.unmodifiableMap(new HashMap<>(levels)), Collections.unmodifiableSet(new HashSet<>(quests)),
			combat, task, members, true);
	}

	public int level(String skill)
	{
		return levels.getOrDefault(skill, 1);
	}

	public Map<String, Integer> getLevels()
	{
		return levels;
	}

	public boolean finished(String quest)
	{
		return quests.contains(quest);
	}

	public int getCombat()
	{
		return combat;
	}

	public String getTask()
	{
		return task;
	}

	public boolean isMembers()
	{
		return members;
	}

	public boolean isKnown()
	{
		return known;
	}
}
