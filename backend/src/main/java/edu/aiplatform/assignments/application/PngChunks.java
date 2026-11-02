package edu.aiplatform.assignments.application;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * PNG mức byte, không dùng AWT (image Alpine không có font): dựng ảnh đại diện cho sơ đồ khi xuất DOCX, ghi và đọc
 * chunk {@code tEXt mxfile} theo cách của Draw.io (XML mã hóa kiểu {@code encodeURIComponent}), đọc kích thước ảnh.
 */
final class PngChunks {

    static final String MXFILE = "mxfile";
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int MAX_TEXT_BYTES = 12 * 1024 * 1024;

    private PngChunks() {
    }

    /** Ảnh đại diện 480×270: khung, ba hình chữ nhật nối nhau; XML sơ đồ ghi vào chunk {@code tEXt mxfile}. */
    static byte[] diagramPlaceholder(String xml) {
        int width = 480;
        int height = 270;
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, 0xF8FAFC);
        rect(pixels, width, 0, 0, width, height, 0xCBD5E1, false);
        box(pixels, width, 40, 100, 110, 70);
        box(pixels, width, 185, 100, 110, 70);
        box(pixels, width, 330, 100, 110, 70);
        rect(pixels, width, 150, 133, 35, 4, 0x475569, true);
        rect(pixels, width, 295, 133, 35, 4, 0x475569, true);
        byte[] png = encode(pixels, width, height);
        return xml == null ? png : withText(png, MXFILE, encodeUri(xml));
    }

    /** Chèn chunk {@code tEXt} ngay sau IHDR. */
    static byte[] withText(byte[] png, String keyword, String latin1Text) {
        byte[] key = keyword.getBytes(StandardCharsets.ISO_8859_1);
        byte[] text = latin1Text.getBytes(StandardCharsets.ISO_8859_1);
        byte[] data = new byte[key.length + 1 + text.length];
        System.arraycopy(key, 0, data, 0, key.length);
        System.arraycopy(text, 0, data, key.length + 1, text.length);
        int afterHeader = SIGNATURE.length + 8 + readInt(png, SIGNATURE.length) + 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(png.length + data.length + 12);
        out.write(png, 0, afterHeader);
        writeChunk(out, "tEXt", data);
        out.write(png, afterHeader, png.length - afterHeader);
        return out.toByteArray();
    }

    /** XML trong chunk {@code tEXt}, {@code zTXt} hoặc {@code iTXt} có khóa {@code mxfile}. */
    static Optional<String> readMxfile(byte[] png) {
        if (png == null || png.length < SIGNATURE.length || !Arrays.equals(Arrays.copyOf(png, 8), SIGNATURE)) {
            return Optional.empty();
        }
        int offset = SIGNATURE.length;
        while (offset + 8 <= png.length) {
            int length = readInt(png, offset);
            String type = new String(png, offset + 4, 4, StandardCharsets.ISO_8859_1);
            int dataStart = offset + 8;
            if (length < 0 || dataStart + length > png.length) {
                return Optional.empty();
            }
            if (type.equals("IEND")) {
                break;
            }
            if (length <= MAX_TEXT_BYTES && (type.equals("tEXt") || type.equals("zTXt") || type.equals("iTXt"))) {
                Optional<String> text = text(type, Arrays.copyOfRange(png, dataStart, dataStart + length));
                if (text.isPresent()) {
                    return text;
                }
            }
            offset = dataStart + length + 4;
        }
        return Optional.empty();
    }

    /** Kích thước điểm ảnh của PNG, GIF, JPEG; không đọc được thì rỗng. */
    static Optional<int[]> size(byte[] image) {
        if (image == null || image.length < 24) {
            return Optional.empty();
        }
        if (Arrays.equals(Arrays.copyOf(image, 8), SIGNATURE)) {
            return Optional.of(new int[]{readInt(image, 16), readInt(image, 20)});
        }
        if (image[0] == 'G' && image[1] == 'I' && image[2] == 'F') {
            return Optional.of(new int[]{(image[6] & 0xFF) | (image[7] & 0xFF) << 8,
                    (image[8] & 0xFF) | (image[9] & 0xFF) << 8});
        }
        if ((image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8) {
            int i = 2;
            while (i + 9 < image.length) {
                if ((image[i] & 0xFF) != 0xFF) {
                    i++;
                    continue;
                }
                int marker = image[i + 1] & 0xFF;
                int segment = (image[i + 2] & 0xFF) << 8 | (image[i + 3] & 0xFF);
                if (marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                    int height = (image[i + 5] & 0xFF) << 8 | (image[i + 6] & 0xFF);
                    int width = (image[i + 7] & 0xFF) << 8 | (image[i + 8] & 0xFF);
                    return Optional.of(new int[]{width, height});
                }
                i += 2 + segment;
            }
        }
        return Optional.empty();
    }

    static boolean isPng(byte[] data) {
        return data != null && data.length > 8 && Arrays.equals(Arrays.copyOf(data, 8), SIGNATURE);
    }

    /** Như {@code encodeURIComponent}: dấu cách thành {@code %20}, không dùng {@code +}. */
    static String encodeUri(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Optional<String> text(String type, byte[] data) {
        int zero = indexOf(data, 0, 0);
        if (zero < 0) {
            return Optional.empty();
        }
        String keyword = new String(data, 0, zero, StandardCharsets.ISO_8859_1);
        if (!keyword.equals(MXFILE)) {
            return Optional.empty();
        }
        try {
            String raw;
            switch (type) {
                case "tEXt" -> raw = new String(data, zero + 1, data.length - zero - 1, StandardCharsets.ISO_8859_1);
                case "zTXt" -> raw = new String(inflate(Arrays.copyOfRange(data, zero + 2, data.length)),
                        StandardCharsets.ISO_8859_1);
                default -> {
                    boolean compressed = data[zero + 1] == 1;
                    int language = indexOf(data, zero + 3, 0);
                    int translated = language < 0 ? -1 : indexOf(data, language + 1, 0);
                    if (translated < 0) {
                        return Optional.empty();
                    }
                    byte[] body = Arrays.copyOfRange(data, translated + 1, data.length);
                    raw = new String(compressed ? inflate(body) : body, StandardCharsets.UTF_8);
                }
            }
            String xml = raw.stripLeading().startsWith("<") ? raw : URLDecoder.decode(raw, StandardCharsets.UTF_8);
            return xml.isBlank() ? Optional.empty() : Optional.of(xml);
        } catch (IllegalArgumentException | DataFormatException e) {
            return Optional.empty();
        }
    }

    private static byte[] inflate(byte[] data) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                out.write(buffer, 0, n);
                if (out.size() > MAX_TEXT_BYTES) {
                    throw new DataFormatException("Chunk quá lớn");
                }
            }
            return out.toByteArray();
        } finally {
            inflater.end();
        }
    }

    private static byte[] encode(int[] pixels, int width, int height) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(height * (width * 3 + 1));
        for (int y = 0; y < height; y++) {
            raw.write(0);
            for (int x = 0; x < width; x++) {
                int rgb = pixels[y * width + x];
                raw.write(rgb >> 16 & 0xFF);
                raw.write(rgb >> 8 & 0xFF);
                raw.write(rgb & 0xFF);
            }
        }
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(raw.toByteArray());
        deflater.finish();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            compressed.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(SIGNATURE);
        writeChunk(out, "IHDR", ByteBuffer.allocate(13).putInt(width).putInt(height)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0).array());
        writeChunk(out, "IDAT", compressed.toByteArray());
        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void box(int[] pixels, int width, int x, int y, int w, int h) {
        rect(pixels, width, x, y, w, h, 0xDBEAFE, true);
        rect(pixels, width, x, y, w, h, 0x2563EB, false);
    }

    private static void rect(int[] pixels, int width, int x, int y, int w, int h, int color, boolean fill) {
        int height = pixels.length / width;
        for (int j = y; j < y + h && j < height; j++) {
            for (int i = x; i < x + w && i < width; i++) {
                boolean edge = j < y + 2 || j >= y + h - 2 || i < x + 2 || i >= x + w - 2;
                if (fill || edge) {
                    pixels[j * width + i] = color;
                }
            }
        }
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.ISO_8859_1);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) << 24 | (data[offset + 1] & 0xFF) << 16 | (data[offset + 2] & 0xFF) << 8
                | (data[offset + 3] & 0xFF);
    }

    private static int indexOf(byte[] data, int from, int value) {
        for (int i = from; i < data.length; i++) {
            if (data[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
