package com.discordscreenshot;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;

/**
 * Reads and writes the webhooks. The default webhook (left-click) keeps using the original
 * "webhook" / "hotkey" config keys. All other webhooks are one JSON list under "webhookList".
 */
@Singleton
class WebhookStore
{
	static final String GROUP = "discord-screenshot";
	private static final String LIST_KEY = "webhookList";
	private static final String MAIN_URL_KEY = "webhook";
	private static final String MAIN_HOTKEY_KEY = "hotkey";
	private static final Type LIST_TYPE = new TypeToken<List<Webhook>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	WebhookStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	// ---------------------------------------------------------------- main webhook

	String getMainUrl()
	{
		return Strings.nullToEmpty(configManager.getConfiguration(GROUP, MAIN_URL_KEY));
	}

	void setMainUrl(String url)
	{
		configManager.setConfiguration(GROUP, MAIN_URL_KEY, url);
	}

	Keybind getMainHotkey()
	{
		Keybind k = configManager.getConfiguration(GROUP, MAIN_HOTKEY_KEY, Keybind.class);
		return k == null ? Keybind.NOT_SET : k;
	}

	void setMainHotkey(Keybind hotkey)
	{
		configManager.setConfiguration(GROUP, MAIN_HOTKEY_KEY, hotkey);
	}

	// ---------------------------------------------------------------- extra webhooks

	List<Webhook> load()
	{
		String json = configManager.getConfiguration(GROUP, LIST_KEY);
		if (Strings.isNullOrEmpty(json))
		{
			return new ArrayList<>();
		}
		try
		{
			List<Webhook> list = gson.fromJson(json, LIST_TYPE);
			return list == null ? new ArrayList<>() : new ArrayList<>(list);
		}
		catch (JsonParseException e)
		{
			return new ArrayList<>();
		}
	}

	void save(List<Webhook> webhooks)
	{
		configManager.setConfiguration(GROUP, LIST_KEY, gson.toJson(webhooks, LIST_TYPE));
	}

	/**
	 * Earlier versions had five fixed slots. The first time this version runs, webhooks 2 to 5
	 * (name, URL, hotkey) are copied into the list. The old keys are left untouched.
	 */
	void migrateIfNeeded()
	{
		if (configManager.getConfiguration(GROUP, LIST_KEY) != null)
		{
			return;
		}

		List<Webhook> migrated = new ArrayList<>();
		for (int slot = 2; slot <= 5; slot++)
		{
			String url = configManager.getConfiguration(GROUP, "webhook" + slot);
			if (Strings.isNullOrEmpty(url))
			{
				continue;
			}
			String name = configManager.getConfiguration(GROUP, "webhook " + slot);
			Webhook webhook = new Webhook(Strings.isNullOrEmpty(name) ? "Webhook " + slot : name, url);
			webhook.setHotkey(configManager.getConfiguration(GROUP, "hotkey" + slot, Keybind.class));
			migrated.add(webhook);
		}
		save(migrated);
	}
}
