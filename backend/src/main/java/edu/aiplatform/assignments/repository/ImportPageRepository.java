package edu.aiplatform.assignments.repository;

import edu.aiplatform.assignments.model.ImportPage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng tạm {@code assignment_import_pages} (BR-U07-91): ghi khi gửi, đọc khi AI chạy và khi cắt hình, xóa sau đó. */
@Repository
public class ImportPageRepository {

    private final JdbcClient jdbc;

    public ImportPageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertPages(UUID draftId, List<ImportPage> pages, Instant now) {
        for (ImportPage page : pages) {
            jdbc.sql("insert into assignment_import_pages (draft_id, page_no, media_type, width, height, bytes, "
                            + "text_hint, created_at) values (:draftId, :pageNo, :mediaType, :width, :height, :bytes, "
                            + ":textHint, :createdAt)")
                    .param("draftId", draftId).param("pageNo", page.pageNo()).param("mediaType", page.mediaType())
                    .param("width", page.width()).param("height", page.height()).param("bytes", page.bytes())
                    .param("textHint", page.textHint()).param("createdAt", AssignmentRepository.ts(now))
                    .update();
        }
    }

    public Optional<ImportPage> loadPage(UUID draftId, int pageNo) {
        return jdbc.sql("select * from assignment_import_pages where draft_id = :draftId and page_no = :pageNo")
                .param("draftId", draftId).param("pageNo", pageNo).query(this::map).optional();
    }

    public List<ImportPage> listPages(UUID draftId) {
        return jdbc.sql("select * from assignment_import_pages where draft_id = :draftId order by page_no")
                .param("draftId", draftId).query(this::map).list();
    }

    public int deletePages(UUID draftId) {
        return jdbc.sql("delete from assignment_import_pages where draft_id = :draftId")
                .param("draftId", draftId).update();
    }

    public int deletePagesOlderThan(Instant cutoff) {
        return jdbc.sql("delete from assignment_import_pages where created_at < :cutoff")
                .param("cutoff", AssignmentRepository.ts(cutoff)).update();
    }

    private ImportPage map(ResultSet rs, int row) throws SQLException {
        return new ImportPage(rs.getInt("page_no"), rs.getString("media_type"), rs.getInt("width"),
                rs.getInt("height"), rs.getBytes("bytes"), rs.getString("text_hint"));
    }
}
