package com.discordscreenshot;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

public class DiscordScreenshotOverlay extends OverlayPanel
{
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final DiscordScreenshotConfig config;

	@Inject
	private DiscordScreenshotOverlay(DiscordScreenshotConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
	}

	static String utcTime()
	{
		return FORMAT.format(ZonedDateTime.now(ZoneOffset.UTC)) + " UTC";
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final String customText = config.customText() == null ? "" : config.customText().trim();
		if (!config.overlay() || customText.isEmpty())
		{
			return null;
		}

		final LineComponent.LineComponentBuilder line = LineComponent.builder()
			.left(customText)
			.leftColor(config.textColor());

		String measured = customText;
		if (config.dateTime())
		{
			final String time = utcTime();
			line.right(time).rightColor(config.dateTimeColor());
			measured = customText + " " + time;
		}

		panelComponent.getChildren().add(line.build());
		panelComponent.setPreferredSize(new Dimension(
			graphics.getFontMetrics().stringWidth(measured) + config.textWidth(), 0));

		return super.render(graphics);
	}
}
