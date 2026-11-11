package com.codex.bosspilot.core;

import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.WorldType;

/**
 * Whether Boss Pilot learns from trips on this world. Trips on Last Man Standing, PvP Arena, beta,
 * tournament, Fresh Start, no-save, quest speedrunning, seasonal and Deadman worlds are not the
 * account's own bossing, so they are not logged and never reach the rates other Codex apps read.
 */
public final class WorldMode
{
	static final Set<WorldType> NOT_LEARNED = EnumSet.of(WorldType.LAST_MAN_STANDING, WorldType.PVP_ARENA, WorldType.BETA_WORLD,
		WorldType.TOURNAMENT_WORLD, WorldType.FRESH_START_WORLD, WorldType.NOSAVE_MODE, WorldType.QUEST_SPEEDRUNNING,
		WorldType.SEASONAL, WorldType.DEADMAN);

	private WorldMode()
	{
	}

	/** Whether trips on a world of these types are the account's own, to log and learn from. */
	public static boolean learns(Set<WorldType> types)
	{
		if (types == null)
		{
			return true;
		}
		for (WorldType t : types)
		{
			if (NOT_LEARNED.contains(t))
			{
				return false;
			}
		}
		return true;
	}
}
