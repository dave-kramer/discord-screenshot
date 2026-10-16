package com.discordscreenshot;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Keybind;

/** One extra webhook, as stored (as JSON) in the plugin's config. */
class Webhook
{
	private String name;
	private String url;
	private int keyCode = KeyEvent.VK_UNDEFINED;
	private int modifiers;

	Webhook(String name, String url)
	{
		this.name = name;
		this.url = url;
	}

	String getName()
	{
		return name == null ? "" : name;
	}

	void setName(String name)
	{
		this.name = name;
	}

	String getUrl()
	{
		return url == null ? "" : url;
	}

	void setUrl(String url)
	{
		this.url = url;
	}

	Keybind getHotkey()
	{
		return new Keybind(keyCode, modifiers);
	}

	void setHotkey(Keybind hotkey)
	{
		Keybind k = hotkey == null ? Keybind.NOT_SET : hotkey;
		this.keyCode = k.getKeyCode();
		this.modifiers = k.getModifiers();
	}
}
