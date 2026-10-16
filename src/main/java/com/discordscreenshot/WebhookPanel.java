package com.discordscreenshot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.ImageUtil;

/** One webhook card in the sidebar, styled like RuneLite's Screen Markers panel. */
class WebhookPanel extends JPanel
{
	private static final Border NAME_BOTTOM_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));

	private static final Border FIELD_BORDER = new EmptyBorder(0, 5, 0, 5);
	private static final Border FIELD_ERROR_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.PROGRESS_ERROR_COLOR),
		new EmptyBorder(0, 5, 0, 5));

	private static final String[] URL_PREFIXES = {
		"https://discord.com/api/webhooks/",
		"https://discordapp.com/api/webhooks/",
		"https://canary.discord.com/api/webhooks/",
		"https://ptb.discord.com/api/webhooks/"
	};

	private static final ImageIcon DELETE_ICON;
	private static final ImageIcon DELETE_HOVER_ICON;
	private static final ImageIcon SHOW_ICON;
	private static final ImageIcon SHOW_HOVER_ICON;
	private static final ImageIcon HIDE_ICON;
	private static final ImageIcon HIDE_HOVER_ICON;

	static
	{
		final BufferedImage deleteImg = ImageUtil.loadImageResource(WebhookPanel.class, "delete_icon.png");
		DELETE_ICON = new ImageIcon(deleteImg);
		DELETE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(deleteImg, -100));

		final BufferedImage showImg = ImageUtil.loadImageResource(WebhookPanel.class, "visible_icon.png");
		SHOW_ICON = new ImageIcon(showImg);
		SHOW_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(showImg, -100));

		final BufferedImage hideImg = ImageUtil.loadImageResource(WebhookPanel.class, "invisible_icon.png");
		HIDE_ICON = new ImageIcon(hideImg);
		HIDE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(hideImg, -100));
	}

	/** What the card can ask its owner to do. */
	interface Actions
	{
		void save();

		void delete();

		void send(String url);
	}

	private final Webhook webhook;
	private final Actions actions;

	private final FlatTextField nameInput = new FlatTextField();
	private final JLabel rename = textAction("Rename", ColorScheme.LIGHT_GRAY_COLOR.darker());
	private final JLabel send = textAction("Send", ColorScheme.PROGRESS_COMPLETE_COLOR);
	private final JLabel save = textAction("Save", ColorScheme.PROGRESS_COMPLETE_COLOR);
	private final JLabel cancel = textAction("Cancel", ColorScheme.PROGRESS_ERROR_COLOR);
	private final JLabel delete = new JLabel(DELETE_ICON);
	private final JPasswordField urlField = new JPasswordField();
	private final char echoChar = urlField.getEchoChar();
	private boolean urlRevealed;

	/**
	 * @param main the default (left-click) webhook: fixed name, cannot be renamed or deleted
	 */
	WebhookPanel(Webhook webhook, boolean main, Actions actions)
	{
		this.webhook = webhook;
		this.actions = actions;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		add(nameRow(main), BorderLayout.NORTH);

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		body.setBorder(new EmptyBorder(6, 8, 8, 8));
		body.add(row("URL", urlRow()));
		body.add(javax.swing.Box.createVerticalStrut(6));
		body.add(row("Hotkey", new HotkeyButton(webhook.getHotkey(), key ->
		{
			webhook.setHotkey(key);
			actions.save();
		})));
		add(body, BorderLayout.CENTER);
	}

	// ---------------------------------------------------------------- name row

	private JPanel nameRow(boolean main)
	{
		nameInput.setText(webhook.getName());
		nameInput.setBorder(null);
		nameInput.setEditable(false);
		nameInput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameInput.setPreferredSize(new Dimension(0, 24));
		nameInput.getTextField().setForeground(Color.WHITE);
		nameInput.getTextField().setBorder(new EmptyBorder(0, 8, 0, 0));
		nameInput.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					saveName();
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					cancelRename();
				}
			}
		});

		JPanel nameActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
		nameActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		send.setToolTipText("Take a screenshot and send it to this webhook");
		onClick(send, () -> actions.send(webhook.getUrl()));

		if (main)
		{
			JLabel tag = new JLabel("LEFT-CLICK");
			tag.setFont(FontManager.getRunescapeSmallFont());
			tag.setForeground(ColorScheme.BRAND_ORANGE);
			tag.setToolTipText("Left-clicking the Discord icon in the top bar sends to this webhook");
			tag.setBorder(new EmptyBorder(3, 0, 0, 2));
			nameActions.add(tag);
			nameActions.add(send);
		}
		else
		{
			rename.setToolTipText("Rename this webhook");
			onClick(rename, () ->
			{
				nameInput.setEditable(true);
				nameInput.getTextField().requestFocusInWindow();
				nameInput.getTextField().selectAll();
				setRenaming(true);
			});
			onClick(save, this::saveName);
			onClick(cancel, this::cancelRename);

			delete.setToolTipText("Delete this webhook");
			delete.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					int confirm = JOptionPane.showConfirmDialog(WebhookPanel.this,
						"Are you sure you want to delete \"" + webhook.getName() + "\"?",
						"Delete webhook", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (confirm == JOptionPane.YES_OPTION)
					{
						actions.delete();
					}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					delete.setIcon(DELETE_HOVER_ICON);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					delete.setIcon(DELETE_ICON);
				}
			});

			nameActions.add(rename);
			nameActions.add(cancel);
			nameActions.add(save);
			nameActions.add(send);
			nameActions.add(delete);
			setRenaming(false);
		}

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		wrapper.setBorder(NAME_BOTTOM_BORDER);
		wrapper.add(nameInput, BorderLayout.CENTER);
		wrapper.add(nameActions, BorderLayout.EAST);
		return wrapper;
	}

	private void setRenaming(boolean renaming)
	{
		save.setVisible(renaming);
		cancel.setVisible(renaming);
		rename.setVisible(!renaming);
		send.setVisible(!renaming);
		delete.setVisible(!renaming);
	}

	private void saveName()
	{
		String name = nameInput.getText().trim();
		if (!name.isEmpty())
		{
			webhook.setName(name);
			actions.save();
		}
		nameInput.setText(webhook.getName());
		nameInput.setEditable(false);
		setRenaming(false);
		requestFocusInWindow();
	}

	private void cancelRename()
	{
		nameInput.setText(webhook.getName());
		nameInput.setEditable(false);
		setRenaming(false);
		requestFocusInWindow();
	}

	// ---------------------------------------------------------------- URL row

	private JPanel urlRow()
	{
		urlField.setText(webhook.getUrl());
		urlField.setBackground(ColorScheme.DARK_GRAY_COLOR);
		urlField.setForeground(ColorScheme.TEXT_COLOR);
		urlField.setCaretColor(Color.WHITE);
		urlField.setPreferredSize(new Dimension(0, 24));
		validateUrl();

		final String[] last = {webhook.getUrl()};
		final Runnable commit = () ->
		{
			String url = new String(urlField.getPassword()).trim();
			if (!url.equals(last[0]))
			{
				last[0] = url;
				webhook.setUrl(url);
				actions.save();
			}
			validateUrl();
		};
		urlField.addActionListener(e -> commit.run());
		urlField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				commit.run();
			}
		});

		final JLabel reveal = new JLabel(SHOW_ICON);
		reveal.setToolTipText("Show / hide the URL");
		reveal.setBorder(new EmptyBorder(0, 6, 0, 0));
		reveal.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				urlRevealed = !urlRevealed;
				urlField.setEchoChar(urlRevealed ? (char) 0 : echoChar);
				reveal.setIcon(urlRevealed ? HIDE_HOVER_ICON : SHOW_HOVER_ICON);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				reveal.setIcon(urlRevealed ? HIDE_HOVER_ICON : SHOW_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				reveal.setIcon(urlRevealed ? HIDE_ICON : SHOW_ICON);
			}
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.add(urlField, BorderLayout.CENTER);
		panel.add(reveal, BorderLayout.EAST);
		return panel;
	}

	/** Red underline when the URL is filled in but does not look like a Discord webhook. */
	private void validateUrl()
	{
		String url = new String(urlField.getPassword()).trim();
		boolean ok = url.isEmpty();
		for (String prefix : URL_PREFIXES)
		{
			ok |= url.startsWith(prefix);
		}
		urlField.setBorder(ok ? FIELD_BORDER : FIELD_ERROR_BORDER);
		urlField.setToolTipText(ok
			? (url.isEmpty() ? "Paste a Discord webhook URL here" : "Discord webhook URL")
			: "This does not look like a Discord webhook URL");
		send.setEnabled(!url.isEmpty());
	}

	// ---------------------------------------------------------------- helpers

	private static JPanel row(String label, JComponent field)
	{
		JLabel l = new JLabel(label);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		l.setPreferredSize(new Dimension(42, 24));

		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.add(l, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	/** Small RuneScape-font text button, like "Rename" / "Save" in Screen Markers. */
	private static JLabel textAction(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setBorder(new EmptyBorder(3, 0, 0, 2));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (label.isEnabled())
				{
					label.setForeground(color.darker());
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				label.setForeground(color);
			}
		});
		return label;
	}

	private static void onClick(JLabel label, Runnable action)
	{
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (label.isEnabled() && e.getButton() == MouseEvent.BUTTON1)
				{
					action.run();
				}
			}
		});
	}
}
