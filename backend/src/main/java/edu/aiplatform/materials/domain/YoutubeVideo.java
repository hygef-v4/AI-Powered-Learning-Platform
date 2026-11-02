package edu.aiplatform.materials.domain;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record YoutubeVideo(String id) {
    public static YoutubeVideo parse(String value) {
        try {
            URI uri = URI.create(value.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1)
                throw new IllegalArgumentException();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(java.util.Locale.ROOT);
            Map<String, String> query = new HashMap<>();
            if (uri.getRawQuery() != null) for (String part : uri.getRawQuery().split("&")) {
                String[] pair = part.split("=", 2);
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                if (query.putIfAbsent(key, pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "") != null)
                    throw new IllegalArgumentException();
            }
            if (query.containsKey("list") || query.containsKey("index")) throw new IllegalArgumentException();
            String id;
            if (host.equals("youtu.be")) id = uri.getPath().substring(1);
            else if ((host.equals("youtube.com") || host.equals("www.youtube.com")) && uri.getPath().equals("/watch"))
                id = query.get("v");
            else throw new IllegalArgumentException();
            if (id == null || !id.matches("[A-Za-z0-9_-]{11}")) throw new IllegalArgumentException();
            return new YoutubeVideo(id);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Chỉ nhận đường dẫn HTTPS của một video YouTube, không nhận playlist");
        }
    }
    public String url() { return "https://www.youtube.com/watch?v=" + id; }
}
