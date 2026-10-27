package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Aim;

/** What the panel asks the plugin to do. */
public interface Actions
{
	/** Shows a boss on the Prepare tab. */
	void select(String bossKey);

	/** Makes a boss the next one whatever the ranking says; null lets the ranking choose again. */
	void pin(String bossKey);

	void aim(Aim aim);
}
