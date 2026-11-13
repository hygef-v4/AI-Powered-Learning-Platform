package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.service.FigureCropper.Box;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import java.awt.image.BufferedImage;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FigureCropperTest {

    private final IArtifact artifacts = mock(IArtifact.class);
    private final FigureCropper cropper = new FigureCropper(artifacts);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);

    private static ImportPage page(int width, int height) {
        return new ImportPage(2, "image/jpeg", width, height, ImportTestImages.jpeg(width, height), null);
    }

    /** So từng phần tử, lệch tối đa 1 pixel do làm tròn số thực. */
    private static void assertArea(int[] actual, int... expected) {
        assertThat(actual).hasSize(4);
        for (int i = 0; i < 4; i++) {
            assertThat(actual[i]).isCloseTo(expected[i], org.assertj.core.data.Offset.offset(1));
        }
    }

    @Test
    void areaIsPaddedAndClampedInsideThePage() {
        assertArea(FigureCropper.pixelArea(new Box(0.25, 0.5, 0.5, 0.25), 1000, 2000), 240, 980, 520, 540);
        // Vùng tràn ra ngoài được kéo vào trong trang.
        assertArea(FigureCropper.pixelArea(new Box(0.9, 0.95, 0.5, 0.5), 1000, 1000), 490, 490, 510, 510);
        // Tối thiểu 2% mỗi chiều.
        assertArea(FigureCropper.pixelArea(new Box(0.5, 0.5, 0, 0), 1000, 1000), 490, 490, 40, 40);
        assertThat(FigureCropper.pixelArea(new Box(Double.NaN, -3, 7, 0.1), 100, 100)[0]).isZero();
    }

    @Test
    void croppedFigureIsStoredAsADocumentImage() throws Exception {
        when(artifacts.store(eq(teacher), eq(ArtifactPurpose.DOCUMENT_IMAGE), any())).thenReturn(new FileRef("ref-1"));

        var figure = cropper.cropFigure(teacher, page(1000, 800), new Box(0.1, 0.1, 0.8, 0.5));

        assertThat(figure).hasValueSatisfying(f -> {
            assertThat(f.fileRef()).isEqualTo("ref-1");
            assertThat(f.width()).isEqualTo(600);
        });
        ArgumentCaptor<FileUpload> upload = ArgumentCaptor.forClass(FileUpload.class);
        verify(artifacts).store(eq(teacher), eq(ArtifactPurpose.DOCUMENT_IMAGE), upload.capture());
        assertThat(upload.getValue().originalFileName()).isEqualTo("hinh-trang-2.png");
        BufferedImage stored = ImageIO.read(upload.getValue().content());
        assertThat(stored.getWidth()).isCloseTo(820, org.assertj.core.data.Offset.offset(1));
        assertThat(stored.getHeight()).isCloseTo(416, org.assertj.core.data.Offset.offset(1));
    }

    @Test
    void storageFailureOrBrokenPageGivesNoFigure() {
        when(artifacts.store(any(), any(), any())).thenThrow(new IllegalStateException("drive"));

        assertThat(cropper.cropFigure(teacher, page(200, 200), new Box(0, 0, 1, 1))).isEmpty();
        assertThat(cropper.cropFigure(teacher, new ImportPage(1, "image/jpeg", 1, 1, new byte[]{1}, null),
                new Box(0, 0, 1, 1))).isEmpty();
    }
}
