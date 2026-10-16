package com.discordscreenshot;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.runelite.client.RuneLite.SCREENSHOT_DIR;

@PluginDescriptor(
	name = "Discord Screenshot",
	description = "Take a screenshot & send it to Discord!",
	tags = {"discord", "screenshot", "photo", "screen", "instant"}
)
public class DiscordScreenshotPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(DiscordScreenshotPlugin.class);

	private static final int SCREENSHOT_SOUND_ID = 2419;
	private static final long COOLDOWN_MS = 2000;
	private static final long UPLOAD_TIMEOUT_SECONDS = 60;
	/** If the frame we are waiting for never arrives, put the interface back after this long. */
	private static final long RESTORE_TIMEOUT_SECONDS = 5;
	/**
	 * Frames to let pass after hiding widgets, before the one we capture. Widgets are only
	 * re-drawn on the next draw, so the very first frame can still contain the old state.
	 */
	private static final int FRAMES_TO_SKIP = 1;
	/** Discord's webhook upload limit is 8 MB on the lowest tier, stay a bit under it. */
	private static final int MAX_UPLOAD_BYTES = 7_500_000;
	private static final MediaType PNG = MediaType.parse("image/png");
	private static final MediaType JPEG = MediaType.parse("image/jpeg");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DiscordScreenshotConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	/**
	 * RuneLite's shared client uses OkHttp's 10 second defaults. A big PNG plus a slow Discord
	 * response (or other plugins, e.g. Dink, uploading over the same connection) easily takes longer.
	 * newBuilder() keeps sharing RuneLite's connection pool and thread pool.
	 */
	private OkHttpClient uploadClient;

	@Inject
	private Gson gson;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DiscordScreenshotOverlay overlay;

	@Inject
	private ScheduledExecutorService executor;

	private final List<HotkeyListener> hotkeyListeners = new ArrayList<>();

	/** True from the moment a capture is requested until the interface has been restored. */
	private final AtomicBoolean busy = new AtomicBoolean();

	@Inject
	private WebhookStore webhookStore;

	private DiscordScreenshotPanel panel;

	/**
	 * Top toolbar icon: left-click sends to the default webhook, right-click lists the others.
	 * RuneLite copies a toolbar button's right-click menu once, when the button is added, so this
	 * button is re-created whenever the webhooks change. Swing thread only after startUp.
	 */
	private volatile NavigationButton toolbarButton;

	private BufferedImage icon;

	/** Sidebar tab with the webhook manager. */
	private NavigationButton sidebarButton;

	/**
	 * Right-click menu shared by both buttons. RuneLite reads it when the menu is opened, so it is
	 * refreshed in place instead of re-creating the buttons (which would close the panel mid-edit).
	 */
	private final Map<String, Runnable> popup = new LinkedHashMap<>();

	private long lastCaptureMs;

	/** Widgets this plugin hid and still has to show again. Only touched on the client thread. */
	private final List<Integer> hiddenByUs = new ArrayList<>();

	/** Bumped whenever a capture ends, so late callbacks of an old capture can tell they are stale. */
	private int generation;

	/** The every-frame listener of the capture in progress, if any. Client thread only. */
	private Runnable frameWaiter;

	@Provides
	DiscordScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordScreenshotConfig.class);
	}

	@Override
	protected void startUp()
	{
		webhookStore.migrateIfNeeded();

		uploadClient = okHttpClient.newBuilder()
			.writeTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();

		overlayManager.add(overlay);
		SCREENSHOT_DIR.mkdirs();
		rebuildHotkeys();

		panel = new DiscordScreenshotPanel(webhookStore, this::capture);
		icon = ImageUtil.loadImageResource(getClass(), "/discord-screenshot.png");

		final Map<String, Runnable> items = popupItems();
		popup.putAll(items);
		toolbarButton = buildToolbarButton(items);

		sidebarButton = NavigationButton.builder()
			.tooltip("Discord Screenshot")
			.icon(icon)
			.priority(9)
			.panel(panel)
			.popup(popup)
			.build();

		clientToolbar.addNavigation(toolbarButton);
		clientToolbar.addNavigation(sidebarButton);
	}

	@Override
	protected void shutDown()
	{
		// Clear the field first, so a pending menu refresh will not add a new toolbar button
		final NavigationButton toolbar = toolbarButton;
		toolbarButton = null;
		clientToolbar.removeNavigation(toolbar);
		clientToolbar.removeNavigation(sidebarButton);
		sidebarButton = null;
		panel = null;
		hotkeyListeners.forEach(keyManager::unregisterKeyListener);
		hotkeyListeners.clear();
		overlayManager.remove(overlay);
		clientThread.invoke(this::finishCapture);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// This event fires for every plugin's config, only react to ours
		if (WebhookStore.GROUP.equals(event.getGroup()))
		{
			rebuildHotkeys();
			refreshPopup();
		}
	}

	// ---------------------------------------------------------------- hotkeys & right-click menu

	private void rebuildHotkeys()
	{
		hotkeyListeners.forEach(keyManager::unregisterKeyListener);
		hotkeyListeners.clear();

		final HotkeyListener main = new HotkeyListener(config::hotkey)
		{
			@Override
			public void hotkeyPressed()
			{
				captureDefault();
			}
		};
		hotkeyListeners.add(main);
		keyManager.registerKeyListener(main);
		for (Webhook webhook : webhookStore.load())
		{
			addHotkey(webhook::getHotkey, webhook::getUrl);
		}
	}

	private void addHotkey(Supplier<Keybind> key, Supplier<String> url)
	{
		HotkeyListener listener = new HotkeyListener(key)
		{
			@Override
			public void hotkeyPressed()
			{
				capture(url.get());
			}
		};
		hotkeyListeners.add(listener);
		keyManager.registerKeyListener(listener);
	}

	private NavigationButton buildToolbarButton(Map<String, Runnable> items)
	{
		return NavigationButton.builder()
			// The webhook URL is a secret, never put it in a tooltip
			.tooltip("Send screenshot to Discord")
			.icon(icon)
			.onClick(this::captureDefault)
			// its own copy: the toolbar keeps the menu it was given
			.popup(new LinkedHashMap<>(items))
			.build();
	}

	private void refreshPopup()
	{
		final Map<String, Runnable> items = popupItems();

		// Applied on the Swing thread, where RuneLite reads the menus
		SwingUtilities.invokeLater(() ->
		{
			// The sidebar tab reads this map each time it is right-clicked
			popup.clear();
			popup.putAll(items);

			// The toolbar button copied its menu when it was added: swap it for a new one
			final NavigationButton old = toolbarButton;
			if (old == null)
			{
				return; // plugin is shutting down
			}
			toolbarButton = buildToolbarButton(items);
			clientToolbar.removeNavigation(old);
			clientToolbar.addNavigation(toolbarButton);
		});
	}

	private Map<String, Runnable> popupItems()
	{
		final Map<String, Runnable> items = new LinkedHashMap<>();
		final Set<String> usedNames = new HashSet<>();
		for (Webhook webhook : webhookStore.load())
		{
			if (Strings.isNullOrEmpty(webhook.getUrl()))
			{
				continue;
			}

			String name = webhook.getName().isEmpty() ? "Webhook" : webhook.getName();
			for (int n = 2; !usedNames.add(name); n++)
			{
				name = webhook.getName() + " (" + n + ")";
			}
			items.put(name, () -> capture(webhook.getUrl()));
		}
		items.put("Open screenshot folder", () -> LinkBrowser.open(SCREENSHOT_DIR.toString()));
		return items;
	}

	// ---------------------------------------------------------------- capture

	/** Left-click on the toolbar icon, or the default hotkey. Does nothing if no default is set. */
	private void captureDefault()
	{
		capture(config.webhook());
	}

	/** Entry point for the toolbar button and hotkeys. May be called from any thread. */
	private void capture(String webhook)
	{
		if (Strings.isNullOrEmpty(webhook))
		{
			return;
		}

		final HttpUrl url = HttpUrl.parse(webhook.trim());
		if (url == null)
		{
			clientThread.invoke(() -> gameMessage("Discord Screenshot: the webhook URL is not valid."));
			return;
		}

		final long now = System.currentTimeMillis();
		if (now - lastCaptureMs < COOLDOWN_MS || !busy.compareAndSet(false, true))
		{
			return;
		}
		lastCaptureMs = now;

		// Everything that touches the client has to run on the client thread
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGIN_SCREEN || client.getLocalPlayer() == null)
			{
				busy.set(false);
				return;
			}

			if (config.soundScreenshot())
			{
				client.playSoundEffect(SCREENSHOT_SOUND_ID, config.volumeSound());
			}

			final String playerName = client.getLocalPlayer().getName();
			final int gen = ++generation;
			hideWidgets();

			// Safety net: whatever happens, never leave the player's chat hidden
			executor.schedule(() -> clientThread.invoke(() ->
			{
				if (gen == generation)
				{
					finishCapture();
					gameMessage("Discord Screenshot: the game did not provide a frame to capture. "
						+ "Another plugin may be interfering, try turning off GPU or screenshot-related plugins.");
				}
			}), RESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

			awaitFrame(hiddenByUs.isEmpty() ? 0 : FRAMES_TO_SKIP, image ->
			{
				if (gen != generation)
				{
					return; // timed out in the meantime, the interface is visible again
				}
				// Show the interface again straight away, we have the pixels now
				finishCapture();
				executor.submit(() ->
				{
					try
					{
						process(url, playerName, (BufferedImage) image);
					}
					catch (RuntimeException e)
					{
						log.warn("Unexpected error while sending the screenshot", e);
						clientThread.invoke(() -> gameMessage("Discord Screenshot: something went wrong, see the RuneLite log."));
					}
				});
			});
		});
	}

	/** Runs {@code consumer} with the frame that is drawn after {@code skip} further frames. */
	private void awaitFrame(int skip, Consumer<Image> consumer)
	{
		final int[] remaining = {skip};
		final Runnable[] self = new Runnable[1];
		self[0] = () ->
		{
			if (remaining[0]-- > 0)
			{
				return;
			}
			drawManager.unregisterEveryFrameListener(self[0]);
			frameWaiter = null;
			// every-frame listeners run before the next-frame queue, so this is delivered this frame
			drawManager.requestNextFrameListener(consumer);
		};
		frameWaiter = self[0];
		drawManager.registerEveryFrameListener(self[0]);
	}

	// ---------------------------------------------------------------- hiding

	/** Client thread only. */
	private void hideWidgets()
	{
		if (config.hidePrivate())
		{
			hide(InterfaceID.PmChat.CONTAINER);
		}
		if (config.hideChat())
		{
			hide(InterfaceID.Chatbox.CHATAREA);
		}
	}

	private void hide(int widgetId)
	{
		Widget widget = client.getWidget(widgetId);
		// Only touch what is visible, so we never "restore" something that was hidden before us
		if (widget != null && !widget.isSelfHidden())
		{
			widget.setHidden(true);
			hiddenByUs.add(widgetId);
		}
	}

	/** Client thread only. Ends the current capture, if any: shows the interface again, frees the lock. */
	private void finishCapture()
	{
		generation++;
		if (frameWaiter != null)
		{
			drawManager.unregisterEveryFrameListener(frameWaiter);
			frameWaiter = null;
		}
		restoreWidgets();
	}

	/** Client thread only. Safe to call any number of times. */
	private void restoreWidgets()
	{
		for (int widgetId : hiddenByUs)
		{
			Widget widget = client.getWidget(widgetId);
			if (widget != null)
			{
				widget.setHidden(false);
			}
		}
		hiddenByUs.clear();
		busy.set(false);
	}

	// ---------------------------------------------------------------- encode & upload (off the client thread)

	private void process(HttpUrl url, String playerName, BufferedImage image)
	{
		byte[] data;
		String fileName = "image.png";
		MediaType mediaType = PNG;
		try
		{
			data = encode(image, "png");
			if (data.length > MAX_UPLOAD_BYTES)
			{
				// Large or stretched clients can exceed Discord's upload limit as PNG, which makes the
				// upload fail with HTTP 413. A JPEG of the same frame is far smaller.
				data = encode(toRgb(image), "jpg");
				fileName = "image.jpg";
				mediaType = JPEG;
			}
		}
		catch (IOException e)
		{
			log.warn("Could not encode the screenshot", e);
			clientThread.invoke(() -> gameMessage("Discord Screenshot: could not encode the screenshot."));
			return;
		}

		if (config.saveScreenshot())
		{
			imageCapture.saveScreenshot(image, "DiscordScreenshot", "DiscordScreenshot", false, false);
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("content", "Screenshot from " + (playerName == null ? "unknown" : playerName));

		RequestBody body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload_json", gson.toJson(payload))
			.addFormDataPart("file", fileName, RequestBody.create(mediaType, data))
			.build();

		uploadClient.newCall(new Request.Builder().url(url).post(body).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Error submitting to the webhook", e);
				clientThread.invoke(() -> gameMessage(e instanceof SocketTimeoutException
					// The upload may well have arrived, Discord just did not answer in time
					? "Discord Screenshot: Discord took too long to respond, check the channel before sending again."
					: "Discord Screenshot: could not reach Discord."));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					if (!r.isSuccessful())
					{
						log.warn("Discord rejected the screenshot: HTTP {}", r.code());
						clientThread.invoke(() -> gameMessage(r.code() == 429
							? "Discord Screenshot: rate limited by Discord, try again in a moment."
							: "Discord Screenshot: Discord rejected the screenshot (HTTP " + r.code() + ")."));
					}
				}
			}
		});
	}

	private static byte[] encode(BufferedImage image, String format) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (!ImageIO.write(image, format, out))
		{
			throw new IOException("No " + format + " writer available");
		}
		return out.toByteArray();
	}

	/** JPEG has no alpha channel, writing an image that has one fails. */
	private static BufferedImage toRgb(BufferedImage image)
	{
		BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return rgb;
	}

	private void gameMessage(String message)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
	}
}
