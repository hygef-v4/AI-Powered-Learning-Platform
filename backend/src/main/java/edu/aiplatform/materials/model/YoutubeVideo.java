package edu.aiplatform.materials.model;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Một video YouTube lấy từ link người dùng dán. Nhận các dạng link thường gặp (watch, youtu.be, Shorts, embed, live, bản di
 * động, thiếu "https://" hay dùng http), bỏ qua phần danh sách phát hay thời điểm phát; chỉ từ chối link không chỉ ra một video
 * (ví dụ chỉ là danh sách phát) hay không phải của YouTube. Luôn lưu lại dạng chuẩn {@link #url()}.
 */
public record YoutubeVideo(String id) {
    private static final Set<String> WATCH_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com",
            "youtube-nocookie.com", "www.youtube-nocookie.com");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern PATH_ID = Pattern.compile("^/(?:shorts|embed|live|v)/([A-Za-z0-9_-]{11})/?$");

    public static YoutubeVideo parse(String value) {
        try {
            String text = value.strip();
            if (!text.contains("://")) text = "https://" + text;
            URI uri = URI.create(text);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("https") && !scheme.equals("http") || uri.getUserInfo() != null || uri.getPort() != -1)
                throw new IllegalArgumentException();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            Map<String, String> query = new HashMap<>();
            if (uri.getRawQuery() != null) for (String part : uri.getRawQuery().split("&")) {
                String[] pair = part.split("=", 2);
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                // Hai mã video trong cùng một link thì không biết lấy video nào.
                if (query.putIfAbsent(key, pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "") != null && key.equals("v"))
                    throw new IllegalArgumentException();
            }
            String id = null;
            if (host.equals("youtu.be")) id = path.replaceFirst("^/", "").replaceFirst("/$", "");
            else if (WATCH_HOSTS.contains(host)) {
                Matcher matcher = PATH_ID.matcher(path);
                if (path.equals("/watch") || path.equals("/watch/")) id = query.get("v");
                else if (matcher.matches()) id = matcher.group(1);
            }
            if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException();
            return new YoutubeVideo(id);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Chỉ nhận link của một video YouTube");
        }
    }
    public String url() { return "https://www.youtube.com/watch?v=" + id; }
}
