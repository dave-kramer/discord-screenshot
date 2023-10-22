package com.discordscreenshot;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.events.ConfigChanged;
import static net.runelite.client.RuneLite.SCREENSHOT_DIR;
import okhttp3.*;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@PluginDescriptor(
		name = "Discord Screenshot",
		description = "Take a screenshot & send it to Discord!",
		tags = {"discord", "screenshot", "photo", "screen", "instant"}
)
@Slf4j

public class DiscordScreenshotPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private DiscordScreenshotConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	private NavigationButton discordScreenshotBtn;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DiscordScreenshotOverlay overlay;

	@Inject
	private ConfigManager configManager;

	private long lastButtonClickTime = 0;

	private static final long coolDownMS = 2000;

	private String selectedWebhookUrl;
	@Provides
	DiscordScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordScreenshotConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(hotkeyListener);
		keyManager.registerKeyListener(hotkeyListener2);
		keyManager.registerKeyListener(hotkeyListener3);
		keyManager.registerKeyListener(hotkeyListener4);
		keyManager.registerKeyListener(hotkeyListener5);
		overlayManager.add(overlay);
		SCREENSHOT_DIR.mkdirs();
		updateDiscordScreenshotButton();
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(discordScreenshotBtn);
		keyManager.registerKeyListener(hotkeyListener);
		keyManager.registerKeyListener(hotkeyListener2);
		keyManager.registerKeyListener(hotkeyListener3);
		keyManager.registerKeyListener(hotkeyListener4);
		keyManager.registerKeyListener(hotkeyListener5);
		overlayManager.remove(overlay);
	}

	private void updateDiscordScreenshotButton() {
		if (discordScreenshotBtn != null) {
			clientToolbar.removeNavigation(discordScreenshotBtn);
			discordScreenshotBtn = null;
		}

		checkWebhookNames();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/discord-screenshot.png");

		discordScreenshotBtn = NavigationButton.builder()
				.tab(false)
				.tooltip("Send screenshot to " + config.webhook())
				.icon(icon)
				.onClick(() -> setSelectedWebhookUrl(config.webhook()))
				.popup(ImmutableMap
						.<String, Runnable>builder()
						.put(config.webhookName2(), () -> setSelectedWebhookUrl(config.webhook2()))
						.put(config.webhookName3(), () -> setSelectedWebhookUrl(config.webhook3()))
						.put(config.webhookName4(), () -> setSelectedWebhookUrl(config.webhook4()))
						.put(config.webhookName5(), () -> setSelectedWebhookUrl(config.webhook5()))
						.put("Open screenshot folder", () ->
						{
							LinkBrowser.open(SCREENSHOT_DIR.toString());
						})
						.build())
				.build();

		clientToolbar.addNavigation(discordScreenshotBtn);
	}

	private void setSelectedWebhookUrl(String webhookUrl) {
		this.selectedWebhookUrl = webhookUrl;
		sendMessage();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		updateDiscordScreenshotButton();
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			setSelectedWebhookUrl(config.webhook());
		}
	};
	private final HotkeyListener hotkeyListener2 = new HotkeyListener(() -> config.hotkey2())
	{
		@Override
		public void hotkeyPressed()
		{
			setSelectedWebhookUrl(config.webhook2());
		}
	};
	private final HotkeyListener hotkeyListener3 = new HotkeyListener(() -> config.hotkey3())
	{
		@Override
		public void hotkeyPressed()
		{
			setSelectedWebhookUrl(config.webhook3());
		}
	};
	private final HotkeyListener hotkeyListener4 = new HotkeyListener(() -> config.hotkey4())
	{
		@Override
		public void hotkeyPressed()
		{
			setSelectedWebhookUrl(config.webhook4());
		}
	};

	private final HotkeyListener hotkeyListener5 = new HotkeyListener(() -> config.hotkey5())
	{
		@Override
		public void hotkeyPressed()
		{
			setSelectedWebhookUrl(config.webhook5());
		}
	};

	private void sendMessage() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastButtonClickTime < coolDownMS) { return; }
		lastButtonClickTime = currentTime;

		if (client.getGameState() == GameState.LOGIN_SCREEN || client.getLocalPlayer() == null) { return; }
		if (config.soundScreenshot()) { clientThread.invoke(() -> client.playSoundEffect(2419, config.volumeSound())); }

		String discordString = "Screenshot from " + client.getLocalPlayer().getName();
		DiscordScreenshotBody discordScreenshotBody = new DiscordScreenshotBody();
		discordScreenshotBody.setContent(discordString);
		sendWebhook(discordScreenshotBody);
	}

	private void sendWebhook(DiscordScreenshotBody discordScreenshotBody)
	{
		String discordUrl = selectedWebhookUrl;
		if (Strings.isNullOrEmpty(discordUrl)) { return; }
		if (config.hidePrivate()) { client.getWidget(10682368).setHidden(true); }
		if (config.hideChat()) { client.getWidget(10616866).setHidden(true); }

		HttpUrl url = HttpUrl.parse(discordUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(discordScreenshotBody));

		screenshotConvert(url, requestBodyBuilder);
	}

	private void screenshotConvert(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = (BufferedImage) image;
			byte[] imageBytes;
			try
			{
				imageBytes = convertImageToByteArray(bufferedImage);
			}
			catch (IOException e)
			{
				log.warn("Error converting the image to byte array", e);
				return;
			}

			if (config.saveScreenshot()) { imageCapture.takeScreenshot(bufferedImage, "DiscordScreenshot", "DiscordScreenshot",  false, null); }

			requestBodyBuilder.addFormDataPart("file", "image.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes));
			buildRequestAndSend(url, requestBodyBuilder);
		});
	}

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting to the webhook", e);
				client.getWidget(10682368).setHidden(false);
				client.getWidget(10616866).setHidden(false);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
				client.getWidget(10682368).setHidden(false);
				client.getWidget(10616866).setHidden(false);
			}
		});
	}

	public void checkWebhookNames() {
		String[] webhookNames = {
				config.webhookName2(),
				config.webhookName3(),
				config.webhookName4(),
				config.webhookName5()
		};

		String changedWebhookName = null;

		for (int i = 0; i < webhookNames.length; i++) {
			for (int j = i + 1; j < webhookNames.length; j++) {
				if (webhookNames[i].equals(webhookNames[j])) {
					changedWebhookName = webhookNames[i];
					break;
				}
			}
			if (changedWebhookName != null) {
				break;
			}
		}

		if (changedWebhookName != null) {
			String defaultValue = generateUniqueValue(webhookNames, "DefaultValue");
			int index = Arrays.asList(webhookNames).indexOf(changedWebhookName) + 2;
			configManager.setConfiguration("discord-screenshot", "webhook " + index, defaultValue);
		}
	}

	private String generateUniqueValue(String[] existingValues, String defaultValue) {
		String uniqueValue = defaultValue;
		int counter = 1;

		while (Arrays.asList(existingValues).contains(uniqueValue)) {
			uniqueValue = defaultValue + counter;
			counter++;
		}

		return uniqueValue;
	}
}