package com.discordscreenshot;

import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Flat hotkey field: click it, then press a key combination. Clicking again clears it.
 * (RuneLite's own HotkeyButton is not public, so plugins need their own.)
 */
class HotkeyButton extends JButton
{
	private Keybind value;

	HotkeyButton(Keybind initial, Consumer<Keybind> onChange)
	{
		value = initial == null ? Keybind.NOT_SET : initial;

		// Allow Tab etc. to be bound
		setFocusTraversalKeysEnabled(false);
		setFocusPainted(false);
		setFont(FontManager.getRunescapeSmallFont());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(0, 5, 0, 5));
		setHorizontalAlignment(LEFT);
		setPreferredSize(new Dimension(0, 24));
		setToolTipText("Click, then press a key. Click again to clear.");
		showValue();

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					value = Keybind.NOT_SET;
					onChange.accept(value);
					setText("Press a key...");
					setForeground(ColorScheme.BRAND_ORANGE);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARK_GRAY_COLOR);
			}
		});

		addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				value = new Keybind(e);
				onChange.accept(value);
				showValue();
			}
		});

		addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				showValue();
			}
		});
	}

	private void showValue()
	{
		boolean set = !Keybind.NOT_SET.equals(value);
		setText(set ? value.toString() : "Not set");
		setForeground(set ? ColorScheme.TEXT_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
	}
}
