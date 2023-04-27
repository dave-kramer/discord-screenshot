package com.discordscreenshot;

import net.runelite.client.config.*;
import java.awt.*;

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

	@ConfigSection(
			name = "Overlay Settings",
			description = "Custom Overlay",
			position = 3,
			closedByDefault = false
	)
	String OverlayConfig = "OverlayConfig";

	@ConfigItem(
			keyName = "overlay",
			name = "Display Overlay",
			description = "Displays the custom overlay on your game screen.",
			section = OverlayConfig,
			position = 1
	)
	default boolean overlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "dateTime",
			name = "Date & Time",
			description = "Adds the date and time to the custom overlay.",
			section = OverlayConfig,
			position = 2
	)
	default boolean dateTime()
	{
		return true;
	}

	@ConfigItem(
			keyName = "customText",
			name = "Custom Text",
			description = "Adds the text to the custom overlay.",
			section = OverlayConfig,
			position = 3
	)
	default String customText()
	{
		return "";
	}

	@Range(min = 10, max = 100)
	@ConfigItem(
			keyName = "textWidth",
			name = "Text Width",
			description = "Space width between custom text and date & time.",
			section = OverlayConfig,
			position = 4
	)
	default int textWidth() { return 10; }

	@ConfigItem(
			keyName = "TextColor",
			name = "Text Color",
			description = "Configure the color of the custom overlay text.",
			section = OverlayConfig,
			position = 5
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
			keyName = "dateTimeColor",
			name = "Date & time Color",
			description = "Configure the color of date & time",
			section = OverlayConfig,
			position = 6
	)
	default Color dateTimeColor()
	{
		return Color.YELLOW;
	}
}
