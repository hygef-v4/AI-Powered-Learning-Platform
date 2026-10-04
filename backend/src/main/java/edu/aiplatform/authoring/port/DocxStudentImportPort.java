package edu.aiplatform.authoring.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.io.InputStream;
import java.util.UUID;

/** U11: xem trước DOCX của người học, block gắn `origin = STUDENT` (BR-U09-45…48). */
public interface DocxStudentImportPort {
    DocxImportPreview preview(ActorRef student, UUID attemptId, InputStream docx);
}
