package com.codex.bosspilot;

import com.codex.bosspilot.combat.Fighter;
import com.codex.bosspilot.core.Aim;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(CodexBossPilotConfig.GROUP)
public interface CodexBossPilotConfig extends Config
{
	String GROUP = "codexbosspilot";
	String AIM_KEY = "aim";

	@ConfigSection(name = "Planning", description = "How the next boss is chosen", position = 0)
	String planning = "planning";

	@ConfigSection(name = "On a trip", description = "What shows while you are at a boss", position = 1)
	String trip = "trip";

	@ConfigItem(keyName = AIM_KEY, name = "Aim", position = 0, section = planning,
		description = "What the next boss should serve: moving your account on, the best loot an hour from your own trips, or the fastest kills")
	default Aim aim()
	{
		return Aim.PROGRESS;
	}

	@ConfigItem(keyName = "wilderness", name = "Include Wilderness bosses", position = 1, section = planning,
		description = "List bosses in the Wilderness, where other players can attack you")
	default boolean wilderness()
	{
		return false;
	}

	@ConfigItem(keyName = "boost", name = "Assume boosts", position = 2, section = planning,
		description = "The potions to assume when working out your best setup and kill times")
	default Fighter.Boost boost()
	{
		return Fighter.Boost.POTIONS;
	}

	@ConfigItem(keyName = "bankPlaque", name = "Kit at the bank", position = 3, section = planning,
		description = "While the bank is open, show what to take out for the next boss")
	default boolean bankPlaque()
	{
		return true;
	}

	@ConfigItem(keyName = "tripPlaque", name = "Trip plaque", position = 0, section = trip,
		description = "Show kills, kill times and how long your supplies will last while at a boss")
	default boolean tripPlaque()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyLow", name = "Notify when supplies run low", position = 1, section = trip,
		description = "Send a notification once when your supplies will not last another kill")
	default boolean notifyLow()
	{
		return true;
	}
}
