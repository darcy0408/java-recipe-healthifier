package com.healthifier.domain;

import java.net.URI;
import java.util.Objects;

public sealed interface ConvertInput {
    String appUserId();
    boolean preview();

    record Text(String text, String appUserId, boolean preview) implements ConvertInput {
        public Text {
            text = requireText(text, "text");
            appUserId = requireText(appUserId, "appUserId");
        }
    }

    record Url(String url, String appUserId, boolean preview) implements ConvertInput {
        public Url {
            url = requireText(url, "url");
            appUserId = requireText(appUserId, "appUserId");
            URI parsed = URI.create(url);
            if (!parsed.isAbsolute() || !("http".equalsIgnoreCase(parsed.getScheme())
                    || "https".equalsIgnoreCase(parsed.getScheme()))) {
                throw new IllegalArgumentException("url must be an absolute HTTP(S) URI");
            }
        }
    }

    record Image(String imageBase64, String imageMediaType, String appUserId, boolean preview) implements ConvertInput {
        public Image {
            imageBase64 = requireText(imageBase64, "imageBase64");
            imageMediaType = requireText(imageMediaType, "imageMediaType");
            appUserId = requireText(appUserId, "appUserId");
            if (!imageMediaType.startsWith("image/")) {
                throw new IllegalArgumentException("imageMediaType must be an image MIME type");
            }
        }
    }

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private String text;
        private String url;
        private String imageBase64;
        private String imageMediaType;
        private String appUserId;
        private boolean preview;

        private Builder() {}

        public Builder text(String text) { clearSource(); this.text = text; return this; }
        public Builder url(String url) { clearSource(); this.url = url; return this; }
        public Builder image(String base64, String mediaType) {
            clearSource(); this.imageBase64 = base64; this.imageMediaType = mediaType; return this;
        }
        public Builder appUserId(String appUserId) { this.appUserId = appUserId; return this; }
        public Builder preview(boolean preview) { this.preview = preview; return this; }

        public ConvertInput build() {
            if (text != null) return new Text(text, appUserId, preview);
            if (url != null) return new Url(url, appUserId, preview);
            if (imageBase64 != null) return new Image(imageBase64, imageMediaType, appUserId, preview);
            throw new IllegalStateException("A text, URL, or image source is required");
        }

        private void clearSource() {
            text = null; url = null; imageBase64 = null; imageMediaType = null;
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.strip();
    }
}
