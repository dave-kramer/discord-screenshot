package com.discordscreenshot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("discord-screenshot")
public interface DiscordScreenshotConfig extends Config
{
	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL",
			description = "The Discord Webhook URL to send messages to."
	)
	String webhook();

	@ConfigItem(
			keyName = "hotkey",
			name = "Set Discord Screenshot Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord."
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

}
