package com.discordscreenshot;

import net.runelite.client.config.*;

@ConfigGroup("discord-screenshot")
public interface DiscordScreenshotConfig extends Config
{
	@ConfigSection(
			name = "Discord Screenshot Settings",
			description = "General settings.",
			position = 0,
			closedByDefault = false
	)
	String DiscordConfig = "DiscordConfig";

	@ConfigItem(
			keyName = "screenshot",
			name = "Save Screenshot",
			description = "Saves screenshot locally.",
			section = DiscordConfig,
			position = 1
	)
	default boolean saveScreenshot() { return true; }

	@ConfigItem(
			keyName = "hideChat",
			name = "Hide Chat",
			description = "Hides the chatbox from screenshot",
			section = DiscordConfig,
			position = 2
	)
	default boolean hideChat() { return false; }

	@ConfigItem(
			keyName = "hidePrivate",
			name = "Hide PMs",
			description = "Hides private messages from screenshot",
			section = DiscordConfig,
			position = 3
	)
	default boolean hidePrivate() { return true; }


	@ConfigItem(
			keyName = "soundScreenshot",
			name = "Make Sound",
			description = "Make sound when screenshot taken.",
			section = DiscordConfig,
			position = 4
	)
	default boolean soundScreenshot() { return true; }

	@Range(min = 1, max = 100)
	@ConfigItem(
			keyName = "volumeSound",
			name = "Volume",
			description = "Screenshot sound effect volume",
			section = DiscordConfig,
			position = 5
	)
	default int volumeSound() { return 70; }

	@ConfigItem(
			keyName = "hotkey",
			name = "Screenshot Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord.",
			section = DiscordConfig,
			position = 6
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigSection(
			name = "Discord Webhook Settings",
			description = "The config for webhook content notifications",
			position = 3,
			closedByDefault = false
	)
	String DiscordHookConfig = "DiscordHookConfig";

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL",
			description = "The Discord Webhook URL to send messages to.",
			section = DiscordHookConfig,
			position = 4
	)
	String webhook();
}
