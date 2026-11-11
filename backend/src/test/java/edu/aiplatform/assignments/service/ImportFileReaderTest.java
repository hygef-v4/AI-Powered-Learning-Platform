package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignments.service.ImportFileReader.CheckedFiles;
import edu.aiplatform.assignments.service.ImportFileReader.FileKind;
import edu.aiplatform.assignments.service.ImportFileReader.ImportFile;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ImportFileReaderTest {

    private final IArtifact artifacts = mock(IArtifact.class);
    private final ImportFileReader reader = new ImportFileReader(artifacts);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);

    private static ImportFile file(String name) {
        return new ImportFile(name, new byte[]{1, 2, 3});
    }

    private static String code(Runnable action) {
        try {
            action.run();
            return null;
        } catch (ApiException e) {
            return e.code();
        }
    }

    @Test
    void imagesAreCheckedThroughTheImportPurposeInTheChosenOrder() {
        when(artifacts.inspect(eq(teacher), eq(ArtifactPurpose.ASSIGNMENT_IMPORT), any(), any()))
                .thenReturn("image/png", "image/webp");

        CheckedFiles files = reader.readFiles(teacher, List.of(file("b.png"), file("a.webp")));

        assertThat(files.kind()).isEqualTo(FileKind.IMAGE);
        assertThat(files.files()).extracting(ImportFileReader.CheckedFile::fileName).containsExactly("b.png", "a.webp");
        assertThat(files.totalBytes()).isEqualTo(6);
    }

    @Test
    void onePdfIsAPdfImport() {
        when(artifacts.inspect(any(), any(), any(), any())).thenReturn("application/pdf");

        assertThat(reader.readFiles(teacher, List.of(file("de.pdf"))).kind()).isEqualTo(FileKind.PDF);
    }

    @Test
    void rejectsEmptyTooManyMixedAndMultiplePdfImports() {
        when(artifacts.inspect(any(), any(), any(), any())).thenReturn("application/pdf", "image/png");
        assertThat(code(() -> reader.readFiles(teacher, List.of()))).isEqualTo("ASSIGNMENT_IMPORT_FILE_INVALID");
        List<ImportFile> six = IntStream.range(0, 6).mapToObj(i -> file(i + ".png")).toList();
        assertThat(code(() -> reader.readFiles(teacher, six))).isEqualTo("ASSIGNMENT_IMPORT_TOO_MANY_FILES");
        assertThat(code(() -> reader.readFiles(teacher, List.of(file("a.pdf"), file("b.png")))))
                .isEqualTo("ASSIGNMENT_IMPORT_MIXED");

        when(artifacts.inspect(any(), any(), any(), any())).thenReturn("application/pdf");
        assertThat(code(() -> reader.readFiles(teacher, List.of(file("a.pdf"), file("b.pdf")))))
                .isEqualTo("ASSIGNMENT_IMPORT_TOO_MANY_FILES");
    }

    @Test
    void fileServiceRejectionsBecomeImportErrorsWithoutReadingMore() {
        when(artifacts.inspect(any(), any(), eq("a.gif"), any()))
                .thenThrow(ApiException.unsupportedMediaType("FILE_TYPE_NOT_ALLOWED", "x"));
        when(artifacts.inspect(any(), any(), eq("big.png"), any()))
                .thenThrow(ApiException.payloadTooLarge("FILE_TOO_LARGE", "Tệp vượt 10 MB"));
        when(artifacts.inspect(any(), any(), eq("staff.png"), any()))
                .thenThrow(ApiException.forbidden("FILE_UPLOAD_FORBIDDEN"));

        assertThat(code(() -> reader.readFiles(teacher, List.of(file("a.gif"), file("b.png")))))
                .isEqualTo("ASSIGNMENT_IMPORT_FILE_INVALID");
        verify(artifacts, never()).inspect(any(), any(), eq("b.png"), any());
        assertThat(code(() -> reader.readFiles(teacher, List.of(file("big.png")))))
                .isEqualTo("ASSIGNMENT_IMPORT_TOO_LARGE");
        assertThatThrownBy(() -> reader.readFiles(teacher, List.of(file("staff.png"))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("FILE_UPLOAD_FORBIDDEN"));
    }
}
