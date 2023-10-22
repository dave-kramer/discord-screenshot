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
			description = "Hides the chat box from screenshot",
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
			name = "Main Screenshot Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord.",
			section = DiscordConfig,
			position = 6
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigSection(
			name = "Main Webhook",
			description = "The config for webhook content notifications",
			position = 0,
			closedByDefault = false
	)
	String DiscordHookConfig = "DiscordHookConfig";

	@ConfigItem(
			keyName = "disclaimer",
			name = "Hover here for more info",
			description = "Left-clicking the Discord Screenshot icon will sent to this webhook.",
			section = DiscordHookConfig,
			position = 1
	)
	default void disclaimer() {}

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL",
			description = "The Discord Webhook URL to send messages to.",
			section = DiscordHookConfig,
			position = 2
	)
	String webhook();

	@ConfigSection(
			name = "Overlay Settings",
			description = "Custom Overlay",
			position = 0,
			closedByDefault = true
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

	@ConfigSection(
			name = "Webhook 2",
			description = "An extra webhook",
			position = 0,
			closedByDefault = true
	)
	String Webhook2Config = "Webhook2Config";

	@ConfigItem(
			keyName = "disclaimer2",
			name = "Hover here for more info",
			description = "Right-click the Discord Screenshot icon will sent to this webhook.",
			section = Webhook2Config,
			position = 1
	)
	default void disclaimer2() {}

	@ConfigItem(
			keyName = "webhook 2",
			name = "Webhook 2 Name",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook2Config,
			position = 2
	)
	default String webhookName2() { return "Webhook 2"; }

	@ConfigItem(
			keyName = "webhook2",
			name = "Webhook 2 URL",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook2Config,
			position = 3
	)
	String webhook2();

	@ConfigItem(
			keyName = "hotkey2",
			name = "Webhook 2 Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord.",
			section = Webhook2Config,
			position = 4
	)
	default Keybind hotkey2()
	{
		return Keybind.NOT_SET;
	}

	@ConfigSection(
			name = "Webhook 3",
			description = "An extra webhook",
			position = 0,
			closedByDefault = true
	)
	String Webhook3Config = "Webhook3Config";

	@ConfigItem(
			keyName = "webhook 3",
			name = "Webhook 3 Name",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook3Config,
			position = 1
	)
	default String webhookName3()  { return "Webhook 3"; }

	@ConfigItem(
			keyName = "webhook3",
			name = "Webhook 3 URL",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook3Config,
			position = 2
	)
	String webhook3();

	@ConfigItem(
			keyName = "hotkey3",
			name = "Webhook 3 Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord.",
			section = Webhook3Config,
			position = 3
	)
	default Keybind hotkey3()
	{
		return Keybind.NOT_SET;
	}

	@ConfigSection(
			name = "Webhook 4",
			description = "An extra webhook",
			position = 0,
			closedByDefault = true
	)
	String Webhook4Config = "Webhook4Config";

	@ConfigItem(
			keyName = "webhook 4",
			name = "Webhook 4 Name",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook4Config,
			position = 1
	)
	default String webhookName4()  { return "Webhook 4"; }

	@ConfigItem(
			keyName = "webhook4",
			name = "Webhook 4 URL",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook4Config,
			position = 2
	)
	String webhook4();

	@ConfigItem(
			keyName = "hotkey4",
			name = "Webhook 4 Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord.",
			section = Webhook4Config,
			position = 3
	)
	default Keybind hotkey4()
	{
		return Keybind.NOT_SET;
	}

	@ConfigSection(
			name = "Webhook 5",
			description = "An extra webhook",
			position = 0,
			closedByDefault = true
	)
	String Webhook5Config = "Webhook5Config";

	@ConfigItem(
			keyName = "webhook 5",
			name = "Webhook 5 Name",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook5Config,
			position = 1
	)
	default String webhookName5()  { return "Webhook 5"; }

	@ConfigItem(
			keyName = "webhook5",
			name = "Webhook 5 URL",
			description = "The Discord Webhook URL to send messages to.",
			section = Webhook5Config,
			position = 2
	)
	String webhook5();

	@ConfigItem(
			keyName = "hotkey5",
			name = "Webhook 5 Hotkey",
			description = "Set a hotkey for taking a screenshot & sending to discord.",
			section = Webhook5Config,
			position = 3
	)
	default Keybind hotkey5()
	{
		return Keybind.NOT_SET;
	}
}


