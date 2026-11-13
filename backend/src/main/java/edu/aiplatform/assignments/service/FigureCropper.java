package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Cắt hình trong đề theo vùng AI trả rồi lưu thành ảnh tài liệu như ảnh nhập DOCX (BR-U07-96). Lỗi cắt hay lưu trả
 * rỗng để nơi gọi thay bằng dòng giữ chỗ; phần còn lại của đề vẫn nhập.
 */
@Component
public class FigureCropper {

    static final double MIN_SIDE = 0.02;
    static final double PADDING = 0.01;
    static final int MAX_PNG_BYTES = 5 * 1024 * 1024;
    static final int MAX_WIDTH = 600;

    private final IArtifact artifacts;

    public FigureCropper(IArtifact artifacts) {
        this.artifacts = artifacts;
    }

    /** Vùng theo tỉ lệ 0–1 của trang, gốc ở góc trên trái. */
    public record Box(double x, double y, double w, double h) {
    }

    public record CroppedFigure(String fileRef, int width) {
    }

    public Optional<CroppedFigure> cropFigure(Actor actor, ImportPage page, Box box) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(page.bytes()));
            if (source == null) {
                return Optional.empty();
            }
            int[] area = pixelArea(box, source.getWidth(), source.getHeight());
            BufferedImage cropped = source.getSubimage(area[0], area[1], area[2], area[3]);
            byte[] png = encode(cropped, "png");
            byte[] bytes = png.length <= MAX_PNG_BYTES ? png : ImportPageNormalizer.encodeJpeg(cropped);
            String name = png.length <= MAX_PNG_BYTES ? "hinh-trang-" + page.pageNo() + ".png"
                    : "hinh-trang-" + page.pageNo() + ".jpg";
            FileRef ref = artifacts.store(actor, ArtifactPurpose.DOCUMENT_IMAGE,
                    new FileUpload(name, bytes.length, new ByteArrayInputStream(bytes)));
            return Optional.of(new CroppedFigure(ref.value(), Math.max(16, Math.min(MAX_WIDTH, area[2]))));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Kẹp vùng vào [0, 1], mỗi chiều tối thiểu 2%, nới 1% mỗi phía; trả {x, y, rộng, cao} theo pixel. */
    static int[] pixelArea(Box box, int width, int height) {
        double w = Math.max(MIN_SIDE, clamp(box.w()));
        double h = Math.max(MIN_SIDE, clamp(box.h()));
        double left = clamp(Math.min(clamp(box.x()), 1 - w) - PADDING);
        double top = clamp(Math.min(clamp(box.y()), 1 - h) - PADDING);
        double right = clamp(Math.min(clamp(box.x()), 1 - w) + w + PADDING);
        double bottom = clamp(Math.min(clamp(box.y()), 1 - h) + h + PADDING);
        int x = (int) Math.floor(left * width);
        int y = (int) Math.floor(top * height);
        int cw = Math.max(1, Math.min(width - x, (int) Math.ceil(right * width) - x));
        int ch = Math.max(1, Math.min(height - y, (int) Math.ceil(bottom * height) - y));
        return new int[]{x, y, cw, ch};
    }

    private static double clamp(double value) {
        return Double.isNaN(value) ? 0 : Math.max(0, Math.min(1, value));
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, out)) {
            throw new IOException("Không mã hóa được " + format);
        }
        return out.toByteArray();
    }
}
