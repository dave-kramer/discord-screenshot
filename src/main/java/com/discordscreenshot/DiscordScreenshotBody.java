package com.discordscreenshot;

import lombok.Data;

@Data
class DiscordScreenshotBody
{
    private String content;
    private Embed embed;

    @Data
    static class Embed
    {
        final UrlEmbed image;
    }

    @Data
    static class UrlEmbed
    {
        final String url;
    }
}