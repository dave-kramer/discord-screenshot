package com.discordscreenshot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;

/** Sidebar tab for managing webhooks, laid out like RuneLite's Screen Markers panel. */
class DiscordScreenshotPanel extends PluginPanel
{
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;

	static
	{
		final BufferedImage addIcon = ImageUtil.loadImageResource(DiscordScreenshotPanel.class, "add_icon.png");
		ADD_ICON = new ImageIcon(addIcon);
		ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
	}

	private final WebhookStore store;
	private final Consumer<String> sender;

	private final JPanel listView = new JPanel();
	private final PluginErrorPanel emptyPanel = new PluginErrorPanel();

	DiscordScreenshotPanel(WebhookStore store, Consumer<String> sender)
	{
		this.store = store;
		this.sender = sender;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		// ---- title + add button
		JLabel title = new JLabel("Discord Screenshot");
		title.setForeground(Color.WHITE);

		JLabel add = new JLabel(ADD_ICON);
		add.setToolTipText("Add a webhook");
		add.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				addWebhook();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				add.setIcon(ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				add.setIcon(ADD_ICON);
			}
		});

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(1, 3, 8, 7));
		titlePanel.add(title, BorderLayout.WEST);
		titlePanel.add(add, BorderLayout.EAST);

		// ---- short how-to
		// Explicit line breaks: Swing's HTML "width" is not reliable with this font and cuts words off
		JLabel help = new JLabel("<html>Click the <font color='#dc8a00'>Discord icon</font> in the<br>"
			+ "top bar to send a screenshot.<br><br>"
			+ "<font color='#ffffff'>Left-click</font>: default webhook<br>"
			+ "<font color='#ffffff'>Right-click</font>: other webhooks<br><br>"
			+ "Sidebar button can be turned<br>"
			+ "off in settings.</html>");
		help.setFont(FontManager.getRunescapeSmallFont());
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel helpPanel = new JPanel(new BorderLayout());
		helpPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		helpPanel.setBorder(new EmptyBorder(7, 8, 7, 8));
		helpPanel.add(help, BorderLayout.CENTER);

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
		northPanel.add(titlePanel, BorderLayout.NORTH);
		northPanel.add(helpPanel, BorderLayout.CENTER);
		add(northPanel, BorderLayout.NORTH);

		// ---- list
		listView.setLayout(new BoxLayout(listView, BoxLayout.Y_AXIS));
		listView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		emptyPanel.setContent("No extra webhooks",
			"Click the + to add one. Right-click the Discord icon to send to it.");

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.add(listView, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		rebuild();
	}

	/** Recreates all cards from the saved config. Swing thread. */
	void rebuild()
	{
		listView.removeAll();

		// Default webhook: same card, but saved to the original config keys
		final Webhook main = new Webhook("Default webhook", store.getMainUrl());
		main.setHotkey(store.getMainHotkey());
		listView.add(new WebhookPanel(main, true, new WebhookPanel.Actions()
		{
			@Override
			public void save()
			{
				store.setMainUrl(main.getUrl());
				store.setMainHotkey(main.getHotkey());
			}

			@Override
			public void delete()
			{
			}

			@Override
			public void send(String url)
			{
				sender.accept(url);
			}
		}));

		final List<Webhook> webhooks = store.load();
		for (Webhook webhook : webhooks)
		{
			listView.add(Box.createVerticalStrut(10));
			listView.add(new WebhookPanel(webhook, false, new WebhookPanel.Actions()
			{
				@Override
				public void save()
				{
					store.save(webhooks);
				}

				@Override
				public void delete()
				{
					webhooks.remove(webhook);
					store.save(webhooks);
					rebuild();
				}

				@Override
				public void send(String url)
				{
					sender.accept(url);
				}
			}));
		}

		if (webhooks.isEmpty())
		{
			listView.add(Box.createVerticalStrut(10));
			listView.add(emptyPanel);
		}

		listView.revalidate();
		listView.repaint();
	}

	private void addWebhook()
	{
		List<Webhook> webhooks = store.load();
		webhooks.add(new Webhook("Webhook " + (webhooks.size() + 2), ""));
		store.save(webhooks);
		rebuild();
	}
}
