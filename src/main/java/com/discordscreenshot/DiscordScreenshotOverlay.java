package com.discordscreenshot;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import java.awt.*;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.List;

public class DiscordScreenshotOverlay extends OverlayPanel {

    @Inject
    private DiscordScreenshotConfig config;

    @Inject
    private DiscordScreenshotOverlay() { setPosition(OverlayPosition.TOP_CENTER); }

    public static String localTime()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(date) + " UTC";
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        String customText = config.customText();
        Color textColor = config.textColor();
        Color dateTimeColor = config.dateTimeColor();

        if (!customText.equals(" ") && !customText.equals("  ") && config.overlay())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(customText)
                    .leftColor(textColor)
                    .build());

            if (config.dateTime())
            {
                customText = customText + " " + localTime();
                List<LayoutableRenderableEntity> e = panelComponent.getChildren();

                ((LineComponent) e.get(0)).setRight(localTime());
                ((LineComponent) e.get(0)).setRightColor(dateTimeColor);
            }

            panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(customText) + config.textWidth(), 0));
        }

        return super.render(graphics);
    }
}
