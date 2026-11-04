package com.codex.bosspilot.ui;

import com.codex.bosspilot.core.Aim;
import com.codex.bosspilot.core.Boss;
import com.codex.bosspilot.core.Format;
import com.codex.bosspilot.core.KitCheck;
import com.codex.bosspilot.core.Logbook;
import com.codex.bosspilot.core.Ranker;
import com.codex.bosspilot.core.Readiness;
import com.codex.bosspilot.core.Reminders;
import com.codex.bosspilot.core.Review;
import com.codex.bosspilot.core.Setup;
import com.codex.bosspilot.core.Supply;
import com.codex.bosspilot.core.Trip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

/**
 * The Boss Pilot side panel, in four tabs: Next (which boss to do now and why, and every other
 * boss with what stands in the way), Prepare (requirements, the best setup you own, the kit and
 * notes for one boss), Trip (the trip under way) and Log (what your trips have made).
 */
public class BossPanel extends PluginPanel
{
	static final int WIDTH = 200;
	static final int TEXT = WIDTH - 30;
	static final String[] TABS = {"Next", "Prepare", "Trip", "Log"};
	static final int NEXT = 0;
	static final int PREPARE = 1;
	static final int TRIP = 2;
	static final int LOG = 3;

	private final Actions actions;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(false, TABS);
	private final JScrollPane scroll = new JScrollPane();
	private View view = View.empty();
	private boolean tripSeen;
	/** The note box, kept across rebuilds so typing is never lost. */
	private final JTextArea noteArea = new JTextArea(3, 0);
	/** The boss whose note is in the box. */
	private String noteBoss;

	public BossPanel(Actions actions)
	{
		super(false);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);
		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setOpaque(false);
		top.add(crest, BorderLayout.NORTH);
		JPanel tabRow = new JPanel(new BorderLayout());
		tabRow.setOpaque(false);
		tabRow.setBorder(new javax.swing.border.EmptyBorder(0, 8, 0, 8));
		tabRow.add(tabs, BorderLayout.CENTER);
		top.add(tabRow, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		add(scroll, BorderLayout.CENTER);
		tabs.onChange(i -> rebuild(false));
		noteArea.setLineWrap(true);
		noteArea.setWrapStyleWord(true);
		noteArea.setFont(Theme.small());
		noteArea.setForeground(Theme.TEXT);
		noteArea.setCaretColor(Theme.GOLD);
		noteArea.setBackground(Theme.INK_DEEP);
		noteArea.setBorder(new javax.swing.border.EmptyBorder(4, 5, 4, 5));
		noteArea.addFocusListener(new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusLost(java.awt.event.FocusEvent e)
			{
				if (noteBoss != null)
				{
					actions.note(noteBoss, noteArea.getText());
				}
			}
		});
		rebuild(false);
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		// A trip starting brings its tab forward once; after that the player chooses.
		boolean onTrip = view.trip != null;
		if (onTrip && !tripSeen)
		{
			tabs.setSelected(TRIP, false);
		}
		tripSeen = onTrip;
		if (noteArea.isFocusOwner() && tabs.getSelected() == PREPARE)
		{
			// Rebuilding would take the box away mid-sentence; the crest can still move.
			updateCrest();
			return;
		}
		rebuild(true);
	}

	/** Shows a tab, as when a boss is picked from the list. */
	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		rebuild(false);
	}

	private void rebuild(boolean keepScroll)
	{
		updateCrest();
		int y = keepScroll ? scroll.getVerticalScrollBar().getValue() : 0;
		StackPanel body;
		switch (tabs.getSelected())
		{
			case PREPARE:
				body = prepare();
				break;
			case TRIP:
				body = trip();
				break;
			case LOG:
				body = log();
				break;
			default:
				body = next();
				break;
		}
		scroll.setViewportView(body);
		scroll.getVerticalScrollBar().setValue(y);
		revalidate();
		repaint();
	}

	private void updateCrest()
	{
		if (!view.loggedIn)
		{
			crest.update("BOSS PILOT", null, "boss", "bosses", "Log in to plan your bossing", null, Theme.TEXT_MUTED, Theme.GOLD);
			return;
		}
		Trip t = view.trip;
		if (t != null)
		{
			crest.update("ON A TRIP: " + t.getBoss().getName().toUpperCase(), t.getKills(), "kill", "kills", view.outlook.text(),
				view.outlook.shortText(), outlookColor(), Theme.GOLD);
			return;
		}
		int open = (int) view.openCount();
		Ranker.Pick next = view.next;
		String status = next == null ? "No bosses to plan" : "Next: " + next.getBoss().getName();
		crest.update("OPEN TO YOU NOW", open, "boss", "bosses", status, null,
			next == null ? Theme.TEXT_MUTED : Theme.status(next.getStatus()), Theme.GOLD);
	}

	// ---- Next ----

	private StackPanel next()
	{
		StackPanel list = new StackPanel(6, 8);
		if (!view.loggedIn)
		{
			list.add(note("Log in and Boss Pilot reads your levels, quests, slayer task and bank, then picks the next boss for "
				+ "your account, the best setup you own for it and the kit to bring."));
			list.add(rules());
			return list;
		}
		list.add(aimBar());
		Ranker.Pick next = view.next;
		if (next != null)
		{
			list.add(new SectionHeader(next.isPinned() ? "Your pick" : "Next boss", next.getBoss().getTier().toString()));
			list.add(hero(next));
		}
		group(list, "Open now", p -> p != next && p.getStatus().isOpen());
		group(list, "A task or kit away", p -> p != next && (p.getStatus() == Readiness.Status.NEEDS_TASK
			|| p.getStatus() == Readiness.Status.NEEDS_KIT));
		group(list, "Locked", p -> p != next && p.getStatus() == Readiness.Status.LOCKED);
		list.add(rules());
		return list;
	}

	private interface Filter
	{
		boolean test(Ranker.Pick p);
	}

	private void group(StackPanel list, String title, Filter filter)
	{
		List<Ranker.Pick> rows = new ArrayList<>();
		for (Ranker.Pick p : view.picks)
		{
			if (filter.test(p))
			{
				rows.add(p);
			}
		}
		if (rows.isEmpty())
		{
			return;
		}
		list.space(2);
		list.add(new SectionHeader(title, String.valueOf(rows.size())));
		for (Ranker.Pick p : rows)
		{
			list.add(row(p));
		}
	}

	private JComponent aimBar()
	{
		JPanel bar = new JPanel(new GridLayout(1, 0, 4, 0));
		bar.setOpaque(false);
		bar.add(new CodexButton("Move on", view.aim == Aim.PROGRESS, 18, () -> actions.aim(Aim.PROGRESS)));
		bar.add(new CodexButton("Loot", view.aim == Aim.LOOT, 18, () -> actions.aim(Aim.LOOT)));
		bar.add(new CodexButton("Quick", view.aim == Aim.QUICK, 18, () -> actions.aim(Aim.QUICK)));
		return bar;
	}

	/** The next boss, large: where, why, how ready, and the way to prepare for it. */
	private JComponent hero(Ranker.Pick p)
	{
		Boss b = p.getBoss();
		Color tint = Theme.status(p.getStatus());
		Card card = new Card(new BorderLayout(0, 6)).accent(tint).highlighted(true);
		StackPanel body = new StackPanel(3, 0);
		body.add(Ui.wrapped(b.getName(), Theme.title(), Theme.GOLD_BRIGHT, TEXT));
		body.add(Ui.wrapped(b.getWhere(), Theme.small(), Theme.TEXT_MUTED, TEXT));
		body.add(Ui.wrapped(p.getWhy(), Theme.small(), Theme.TEXT, TEXT));
		body.add(chips(p));
		card.add(body, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		buttons.add(new CodexButton("Prepare", true, 20, () -> select(b)));
		buttons.add(javax.swing.Box.createHorizontalStrut(6));
		buttons.add(p.isPinned()
			? new CodexButton("Unpin", false, 20, () -> actions.pin(null))
			: new CodexButton("Pin", false, 20, () -> actions.pin(b.getKey())));
		card.add(buttons, BorderLayout.SOUTH);
		return card;
	}

	/** Small status line: readiness, kill time and kit. */
	private JComponent chips(Ranker.Pick p)
	{
		List<String> bits = new ArrayList<>();
		bits.add(p.getStatus().toString());
		if (p.getKillSeconds() > 0)
		{
			bits.add("~" + Format.clock(p.getKillSeconds()) + " a kill");
		}
		KitCheck kit = p.getKit();
		bits.add(kit.isPacked() ? "kit packed" : kit.isOwned() ? "kit in bank" : "kit short");
		return Ui.wrapped(String.join(" · ", bits), Theme.small(), Theme.status(p.getStatus()), TEXT);
	}

	private JComponent row(Ranker.Pick p)
	{
		Boss b = p.getBoss();
		Color tint = Theme.status(p.getStatus());
		Card card = new Card(new BorderLayout(6, 2)).accent(tint);
		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		head.add(Ui.label(b.getName(), Theme.body(), p.getStatus() == Readiness.Status.LOCKED ? Theme.TEXT_MUTED : Theme.TEXT),
			BorderLayout.CENTER);
		String right = p.getKillSeconds() > 0 && p.getStatus().isOpen() ? "~" + Format.clock(p.getKillSeconds()) : p.getStatus().toString();
		JLabel r = Ui.label(right, Theme.small(), tint);
		r.setHorizontalAlignment(SwingConstants.RIGHT);
		head.add(r, BorderLayout.EAST);
		card.add(head, BorderLayout.NORTH);
		card.add(Ui.wrapped(p.getWhy(), Theme.small(), Theme.TEXT_MUTED, TEXT), BorderLayout.CENTER);
		card.onClick(() -> select(b));
		return card;
	}

	private void select(Boss b)
	{
		actions.select(b.getKey());
		showTab(PREPARE);
	}

	// ---- Prepare ----

	private StackPanel prepare()
	{
		StackPanel list = new StackPanel(6, 8);
		Ranker.Pick p = view.selected;
		if (!view.loggedIn || p == null)
		{
			list.add(note("Pick a boss on the Next tab to see what it takes, the best setup you own and the kit to bring."));
			return list;
		}
		Boss b = p.getBoss();
		Color tint = Theme.status(p.getStatus());
		Card head = new Card(new BorderLayout(0, 6)).accent(tint).highlighted(true);
		StackPanel hb = new StackPanel(3, 0);
		hb.add(Ui.wrapped(b.getName(), Theme.title(), Theme.GOLD_BRIGHT, TEXT));
		hb.add(Ui.wrapped(b.getWhere(), Theme.small(), Theme.TEXT_MUTED, TEXT));
		hb.add(Ui.wrapped("Getting there: " + b.getTravel(), Theme.small(), Theme.TEXT, TEXT));
		hb.add(chips(p));
		head.add(hb, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		buttons.add(p.isPinned()
			? new CodexButton("Unpin", false, 20, () -> actions.pin(null))
			: new CodexButton("Pin as next", false, 20, () -> actions.pin(b.getKey())));
		head.add(buttons, BorderLayout.SOUTH);
		list.add(head);

		list.space(2);
		list.add(new SectionHeader("Requirements"));
		list.add(requirements(p));

		list.space(2);
		list.add(new SectionHeader("Your best setup", view.setups.isEmpty() ? null : view.setups.get(0).getKind()));
		if (view.setupsPending)
		{
			list.add(note("Working out the best setup you own..."));
		}
		else if (view.setups.isEmpty())
		{
			list.add(note(b.getTargets().isEmpty()
				? "No fight data for " + b.getName() + " yet, so there is no setup to rate. Bring your strongest gear for the styles below."
				: "Open your bank once so Boss Pilot knows what gear you own."));
		}
		else
		{
			for (Setup s : view.setups)
			{
				list.add(setup(s));
			}
		}

		list.space(2);
		KitCheck kit = p.getKit();
		list.add(new SectionHeader("Kit", kit.isPacked() ? "packed" : view.bankSeenAt > 0 ? "bank seen" : "open your bank"));
		for (KitCheck.Line l : kit.getLines())
		{
			list.add(kitLine(l));
		}

		if (!b.getBriefing().isEmpty())
		{
			list.space(2);
			list.add(new SectionHeader("Briefing", "read before you go"));
			list.add(briefing(b));
		}

		list.space(2);
		list.add(new SectionHeader("Your notes", "markers, plans"));
		list.add(noteCard(b));

		list.space(2);
		list.add(new SectionHeader("Before you go"));
		Card notes = new Card(new BorderLayout());
		StackPanel nb = new StackPanel(4, 0);
		nb.add(Ui.wrapped("Attacks: " + b.getAttacks() + ".", Theme.small(), Theme.TEXT, TEXT));
		for (String n : b.getNotes())
		{
			nb.add(Ui.wrapped(n, Theme.small(), Theme.TEXT_MUTED, TEXT));
		}
		notes.add(nb, BorderLayout.CENTER);
		list.add(notes);
		return list;
	}

	private static JComponent briefing(Boss b)
	{
		Card card = new Card(new BorderLayout()).accent(Theme.TEAL);
		StackPanel body = new StackPanel(6, 0);
		for (Boss.Brief brief : b.getBriefing())
		{
			StackPanel one = new StackPanel(1, 0);
			one.add(Ui.label(brief.getMechanic(), Theme.body(), Theme.GOLD));
			one.add(Ui.wrapped(brief.getAnswer(), Theme.small(), Theme.TEXT, TEXT));
			body.add(one);
		}
		body.add(Ui.wrapped("A guide's summary. Nothing here reacts during the fight.", Theme.small(), Theme.TEXT_FAINT, TEXT));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private JComponent noteCard(Boss b)
	{
		if (!b.getKey().equals(noteBoss))
		{
			noteBoss = b.getKey();
			noteArea.setText(view.note);
			noteArea.setCaretPosition(0);
		}
		Card card = new Card(new BorderLayout(0, 4));
		card.add(noteArea, BorderLayout.CENTER);
		card.add(Ui.wrapped("Your own plan, such as the Ground Markers set to load. It shows on the trip plaque until the first kill.",
			Theme.small(), Theme.TEXT_FAINT, TEXT), BorderLayout.SOUTH);
		return card;
	}

	private JComponent requirements(Ranker.Pick p)
	{
		Boss b = p.getBoss();
		Card card = new Card(new BorderLayout());
		StackPanel body = new StackPanel(3, 0);
		List<String> missing = p.getReadiness().getMissing();
		boolean any = false;
		for (String q : b.getQuests())
		{
			String text = "Quest: " + Readiness.questName(q);
			body.add(check(text, !missing.contains(text)));
			any = true;
		}
		for (Map.Entry<String, Integer> e : b.getSkills().entrySet())
		{
			boolean met = true;
			for (String m : missing)
			{
				if (m.startsWith(e.getValue() + " " + e.getKey()))
				{
					met = false;
					break;
				}
			}
			body.add(check(e.getValue() + " " + e.getKey(), met));
			any = true;
		}
		if (b.needsTask())
		{
			body.add(check("A " + b.getTasks().get(0) + " task", p.getStatus() != Readiness.Status.NEEDS_TASK
				&& p.getStatus() != Readiness.Status.LOCKED));
			any = true;
		}
		for (KitCheck.Line l : p.getKit().getLines())
		{
			if (l.getSupply().getNeed() == Supply.Need.MUST)
			{
				body.add(check(l.getSupply().getLabel(), l.isOwned()));
				any = true;
			}
		}
		if (!any)
		{
			body.add(Ui.wrapped("None: anyone can go.", Theme.small(), Theme.GREEN, TEXT));
		}
		body.add(Ui.wrapped("Combat " + b.getSuggestedCombat() + " is comfortable"
			+ (p.getStatus() == Readiness.Status.STRETCH ? " (a stretch for you)" : "") + ".", Theme.small(), Theme.TEXT_FAINT, TEXT));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private static JComponent check(String text, boolean met)
	{
		JPanel line = new JPanel(new BorderLayout(6, 0));
		line.setOpaque(false);
		line.add(Ui.label(met ? "+" : "x", Theme.small(), met ? Theme.GREEN : Theme.BLOCKED), BorderLayout.WEST);
		line.add(Ui.wrapped(text, Theme.small(), met ? Theme.TEXT : Theme.BLOCKED, TEXT - 14), BorderLayout.CENTER);
		return line;
	}

	private JComponent setup(Setup s)
	{
		Card card = new Card(new BorderLayout(0, 4)).accent(kindColor(s.getKind()));
		StackPanel body = new StackPanel(2, 0);
		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		String title = s.getKind() + (s.getTarget() == null ? "" : " vs " + s.getTarget());
		head.add(Ui.label(title, Theme.body(), Theme.GOLD_BRIGHT), BorderLayout.CENTER);
		JLabel kill = Ui.label(s.getKillSeconds() > 0 ? "~" + Format.clock(s.getKillSeconds()) : "-", Theme.small(), Theme.TEAL);
		head.add(kill, BorderLayout.EAST);
		body.add(head);
		body.add(Ui.label(String.format(java.util.Locale.ROOT, "%.2f dps, max hit %d", s.getDps(), s.getMaxHit()), Theme.small(),
			Theme.TEXT_MUTED));
		for (String g : s.getGear())
		{
			body.add(Ui.wrapped(g, Theme.small(), Theme.TEXT, TEXT));
		}
		List<String> how = new ArrayList<>();
		if (s.getStyle() != null)
		{
			how.add(s.getStyle());
		}
		if (s.getSpell() != null)
		{
			how.add(s.getSpell());
		}
		if (s.getPrayer() != null)
		{
			how.add(s.getPrayer());
		}
		if (!how.isEmpty())
		{
			body.add(Ui.wrapped(String.join(", ", how), Theme.small(), Theme.TEXT_FAINT, TEXT));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	static Color kindColor(String kind)
	{
		if ("Ranged".equals(kind))
		{
			return Theme.GREEN;
		}
		if ("Magic".equals(kind))
		{
			return Theme.VIOLET;
		}
		return Theme.EMBER;
	}

	private JComponent kitLine(KitCheck.Line l)
	{
		Supply s = l.getSupply();
		Color c = l.isPacked() ? Theme.GREEN : l.isOwned() ? Theme.GOLD : s.getNeed() == Supply.Need.OPTIONAL ? Theme.TEXT_FAINT
			: s.getNeed() == Supply.Need.MUST ? Theme.BLOOD : Theme.EMBER;
		Card card = new Card(new BorderLayout(6, 0)).accent(c);
		String label = s.getLabel() + (s.getNeed() == Supply.Need.MUST ? " (must)" : s.getNeed() == Supply.Need.OPTIONAL ? " (optional)" : "");
		card.add(Ui.wrapped(label, Theme.small(), Theme.TEXT, TEXT - 60), BorderLayout.CENTER);
		String have;
		if (l.isPacked())
		{
			have = "packed";
		}
		else if (l.isOwned())
		{
			have = "take " + amount(s, l.toWithdraw());
		}
		else
		{
			have = amount(s, l.getOwned()) + "/" + s.amountText();
		}
		JLabel right = Ui.label(have, Theme.small(), c);
		right.setHorizontalAlignment(SwingConstants.RIGHT);
		card.add(right, BorderLayout.EAST);
		return card;
	}

	private static String amount(Supply s, long n)
	{
		switch (s.getKind())
		{
			case FOOD:
				return n + " hp";
			case DOSES:
				return n + (n == 1 ? " dose" : " doses");
			default:
				return String.valueOf(n);
		}
	}

	// ---- Trip ----

	private StackPanel trip()
	{
		StackPanel list = new StackPanel(6, 8);
		Trip t = view.trip;
		if (t == null)
		{
			Ranker.Pick next = view.next;
			list.add(note(next == null ? "Not on a trip. Head to a boss and the trip starts by itself."
				: "Not on a trip. Head to " + next.getBoss().getName() + " (" + next.getBoss().getTravel()
				+ ") and the trip starts by itself."));
			if (view.review != null)
			{
				list.space(2);
				list.add(new SectionHeader("Last trip", view.review.getBoss().getName()));
				list.add(review(view.review));
			}
			return list;
		}
		list.add(new SectionHeader(t.getBoss().getName(), Format.clock(t.elapsed(view.now) / 1000.0)));
		for (Reminders.Reminder r : view.reminders)
		{
			Card rc = new Card(new BorderLayout()).accent(r.isUrgent() ? Theme.EMBER : Theme.GOLD);
			rc.add(Ui.wrapped(r.getText(), Theme.body(), r.isUrgent() ? Theme.EMBER : Theme.GOLD, TEXT), BorderLayout.CENTER);
			list.add(rc);
		}
		Card outlook = new Card(new BorderLayout()).accent(outlookColor()).highlighted(true);
		outlook.add(Ui.wrapped(view.outlook.text(), Theme.body(), Theme.TEXT, TEXT), BorderLayout.CENTER);
		list.add(outlook);
		JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
		grid.setOpaque(false);
		Logbook.Entry e = view.logbook.peek(t.getBoss());
		double best = t.bestKill();
		if (e != null && e.best > 0)
		{
			best = best <= 0 ? e.best : Math.min(best, e.best);
		}
		grid.add(new StatTile("Kills", String.valueOf(t.getKills()), true));
		grid.add(new StatTile("Loot", Format.number(t.getLoot()), t.getLoot() > 0));
		grid.add(new StatTile("Average", t.getKillSeconds().isEmpty() ? "-" : Format.clock(t.averageKill()), !t.getKillSeconds().isEmpty()));
		grid.add(new StatTile("Best", best > 0 ? Format.clock(best) : "-", best > 0));
		grid.add(new StatTile("Food", t.getFoodNow() < 0 ? "-" : t.getFoodNow() + " hp", t.getFoodNow() > 0));
		grid.add(new StatTile("Prayer", t.getDosesNow() < 0 ? "-" : t.getDosesNow() + " doses", t.getDosesNow() > 0));
		list.add(grid);
		if (!t.getKillSeconds().isEmpty())
		{
			list.space(2);
			list.add(new SectionHeader("Kill times"));
			list.add(killTimes(t.getKillSeconds()));
		}
		if (t.getDamage() > 0)
		{
			list.add(note("Damage taken: " + t.getDamage() + ", biggest hit " + t.getBiggestHit() + "."));
		}
		if (t.getDeaths() > 0)
		{
			list.add(note(t.getDeaths() == 1 ? "You died once on this trip." : "You died " + t.getDeaths() + " times on this trip."));
		}
		return list;
	}

	private Color outlookColor()
	{
		switch (view.outlook.getLevel())
		{
			case PLENTY:
				return Theme.GREEN;
			case FEW:
				return Theme.GOLD;
			case LAST:
				return Theme.EMBER;
			default:
				return Theme.TEAL;
		}
	}

	private static JComponent killTimes(List<Double> times)
	{
		Card card = new Card(new BorderLayout());
		JPanel grid = new JPanel(new GridLayout(0, 4, 4, 2));
		grid.setOpaque(false);
		double best = Double.MAX_VALUE;
		for (double s : times)
		{
			best = Math.min(best, s);
		}
		int from = Math.max(0, times.size() - 12);
		for (int i = from; i < times.size(); i++)
		{
			double s = times.get(i);
			grid.add(Ui.label(Format.clock(s), Theme.small(), s == best ? Theme.GREEN : Theme.TEXT));
		}
		card.add(grid, BorderLayout.CENTER);
		return card;
	}

	private static JComponent review(Review r)
	{
		Logbook.TripRecord t = r.getTrip();
		Card card = new Card(new BorderLayout(0, 6)).accent(Theme.GOLD);
		StackPanel body = new StackPanel(3, 0);
		body.add(Ui.wrapped(Format.plural(t.kills, "kill", "kills") + " in " + Format.clock(t.minutes() * 60)
			+ (t.loot > 0 ? ", " + Format.number(t.loot) + " loot" : ""), Theme.body(), Theme.TEXT, TEXT));
		if (t.kills > 0 && t.minutes() > 0)
		{
			body.add(Ui.wrapped(String.format(java.util.Locale.ROOT, "%.1f kills an hour", r.killsPerHour())
				+ (t.loot > 0 ? ", " + Format.number(r.lootPerHour()) + " loot an hour" : ""), Theme.small(), Theme.TEXT_MUTED, TEXT));
		}
		if (r.damageText() != null)
		{
			body.add(Ui.wrapped(r.damageText(), Theme.small(), Theme.TEXT_MUTED, TEXT));
		}
		body.add(Ui.wrapped(r.getAdvice(), Theme.small(), Theme.TEAL, TEXT));
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	// ---- Log ----

	private StackPanel log()
	{
		StackPanel list = new StackPanel(6, 8);
		if (view.review != null)
		{
			list.add(new SectionHeader("Last trip", view.review.getBoss().getName()));
			list.add(review(view.review));
			list.space(2);
		}
		List<Boss> seen = new ArrayList<>();
		for (Ranker.Pick p : view.picks)
		{
			if (view.logbook.visited(p.getBoss()))
			{
				seen.add(p.getBoss());
			}
		}
		if (seen.isEmpty())
		{
			list.add(note("Your trips show here: kill counts, best kills, kills and loot an hour. Every trip is kept on this computer only."));
			return list;
		}
		list.add(new SectionHeader("Your bosses", String.valueOf(seen.size())));
		for (Boss b : seen)
		{
			list.add(logRow(b));
		}
		return list;
	}

	private JComponent logRow(Boss b)
	{
		Logbook.Entry e = view.logbook.peek(b);
		Logbook.Rates rates = view.logbook.rates(b);
		Card card = new Card(new BorderLayout(6, 2)).accent(Theme.GOLD_DIM);
		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		head.add(Ui.label(b.getName(), Theme.body(), Theme.TEXT), BorderLayout.CENTER);
		head.add(Ui.label(e != null && e.kc > 0 ? "KC " + Format.number(e.kc) : "", Theme.small(), Theme.GOLD), BorderLayout.EAST);
		card.add(head, BorderLayout.NORTH);
		List<String> bits = new ArrayList<>();
		if (e != null && e.best > 0)
		{
			bits.add("best " + Format.clock(e.best));
		}
		if (rates.isKnown())
		{
			bits.add(String.format(java.util.Locale.ROOT, "%.0f an hour", rates.killsPerHour()));
			if (rates.loot > 0)
			{
				bits.add(Format.number(rates.lootPerHour()) + " loot an hour");
			}
		}
		if (rates.trips > 0)
		{
			bits.add(Format.plural(rates.trips, "trip", "trips"));
		}
		card.add(Ui.wrapped(bits.isEmpty() ? "Seen, no trips timed yet" : String.join(", ", bits), Theme.small(), Theme.TEXT_MUTED, TEXT),
			BorderLayout.CENTER);
		card.onClick(() -> select(b));
		return card;
	}

	// ---- Shared ----

	/** What Boss Pilot will and will not do, so nobody expects a fight helper. */
	private static JComponent rules()
	{
		Card card = new Card(new BorderLayout());
		card.add(Ui.wrapped("Boss Pilot plans your trips and watches your own hitpoints, prayer and supplies. It never reacts to what a boss is doing.",
			Theme.small(), Theme.TEXT_FAINT, TEXT), BorderLayout.CENTER);
		return card;
	}

	private static JComponent note(String text)
	{
		Card card = new Card(new BorderLayout());
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, TEXT), BorderLayout.CENTER);
		return card;
	}
}
