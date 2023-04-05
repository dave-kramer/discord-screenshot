package com.discordscreenshot;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.input.KeyManager;
import okhttp3.*;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

	@Provides
	DiscordScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordScreenshotConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		keyManager.registerKeyListener(hotkeyListener);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/discord-screenshot.png");

		discordScreenshotBtn = NavigationButton.builder()
				.tab(false)
				.tooltip("Send screenshot to Discord")
				.icon(icon)
				.onClick(this::sendMessage)
				.build();

		clientToolbar.addNavigation(discordScreenshotBtn);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(discordScreenshotBtn);
		keyManager.unregisterKeyListener(hotkeyListener);
	}

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			sendMessage();
		}
	};

	private void sendMessage()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		String discordString = "Screenshot from " + client.getLocalPlayer().getName();

		DiscordScreenshotBody discordScreenshotBody = new DiscordScreenshotBody();
		discordScreenshotBody.setContent(discordString);
		sendWebhook(discordScreenshotBody);
	}

	private void sendWebhook(DiscordScreenshotBody discordScreenshotBody)
	{
		String discordUrl = config.webhook();
		if (Strings.isNullOrEmpty(discordUrl)) { return; }

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
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}
}
