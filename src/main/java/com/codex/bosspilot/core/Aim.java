package com.codex.bosspilot.core;

/** What "the next boss" should serve. */
public enum Aim
{
	/** New and harder bosses the account is ready for: moving the account on. */
	PROGRESS("Move my account on"),
	/** The bosses whose trips have paid best, from the player's own logbook. */
	LOOT("Best loot an hour"),
	/** The fastest kills with the gear the player owns. */
	QUICK("Fastest kills");

	private final String label;

	Aim(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
