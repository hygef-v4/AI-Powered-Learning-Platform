package edu.aiplatform.assignments.contract.stub;

import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDiagramLinker;
import edu.aiplatform.assignments.contract.IDiagramResolver;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.IGroupAssignmentSupport;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.assignments.contract.RubricScore;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.stub.Stubs;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Bản tạm của interface U07 (và {@code IGroupAssignmentSupport} do U12 cài): bài mẫu đang mở, khung rỗng một phần,
 * chưa có rubric, mọi kiểm tra tài liệu đều hợp lệ; chưa có U12 thì bài nhóm báo {@code GROUP_SUPPORT_UNAVAILABLE}.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class AssignmentsContractStubs {

    static final DocumentModel EMPTY_DOCUMENT = new DocumentModel(List.of());
    static final DocumentPart WHOLE_DOCUMENT = new DocumentPart("DOCUMENT", null, List.of(), "Tài liệu");
    static final ValidationResult VALID = new ValidationResult(List.of(), List.of());

    static AssignmentView sampleAssignment(UUID assignmentId) {
        Instant now = Instant.now();
        return new AssignmentView(assignmentId, AssignmentScope.CLASS, Stubs.derivedId("class", assignmentId),
                Stubs.derivedId("subject", assignmentId), 1, "Bài mẫu", SubmissionMode.INDIVIDUAL, GradingMode.GRADED,
                false, AssignmentStatus.OPEN, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(7)), null, null,
                false, null, List.of());
    }

    @Bean
    @ConditionalOnMissingBean
    IAssignmentQuery assignmentQueryStub() {
        return Stubs.announce(IAssignmentQuery.class, new IAssignmentQuery() {
            @Override
            public Optional<AssignmentView> get(UUID assignmentId) {
                return Optional.of(sampleAssignment(assignmentId));
            }

            @Override
            public Optional<AssignmentContent> getContent(UUID assignmentId) {
                return Optional.of(new AssignmentContent(assignmentId, 1, "", EMPTY_DOCUMENT, List.of(WHOLE_DOCUMENT),
                        Map.of(), List.of()));
            }

            @Override
            public List<AssignmentView> listForClass(UUID classId) {
                return List.of();
            }

            @Override
            public SubmissionWindow submissionWindow(UUID assignmentId, Instant now) {
                return SubmissionWindow.ON_TIME;
            }

            @Override
            public List<AssignmentView> listClosingBetween(Instant from, Instant to) {
                return List.of();
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IRubric rubricStub() {
        return Stubs.announce(IRubric.class, new IRubric() {
            @Override
            public List<RubricView> listRubrics(UUID assignmentId) {
                return List.of();
            }

            @Override
            public RubricScore score(UUID rubricId, Set<UUID> checkedItemIds) {
                return new RubricScore(rubricId, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            @Override
            public BigDecimal maxPoints(UUID assignmentId) {
                return BigDecimal.ZERO;
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IDocumentModel documentModelStub() {
        return Stubs.announce(IDocumentModel.class, new IDocumentModel() {
            @Override
            public ValidationResult validateSkeleton(DocumentModel skeleton) {
                return VALID;
            }

            @Override
            public ValidationResult validateForSave(DocumentModel skeleton, DocumentModel document) {
                return VALID;
            }

            @Override
            public ValidationResult validateForSubmit(DocumentModel skeleton, DocumentModel document,
                                                      Map<DiagramType, Integer> requiredDiagrams) {
                return VALID;
            }

            @Override
            public List<DocumentPart> computeParts(DocumentModel skeleton) {
                return List.of(WHOLE_DOCUMENT);
            }

            @Override
            public String toPlainText(DocumentModel document) {
                return "";
            }

            @Override
            public String compactDiagramXml(String xml) {
                return xml;
            }

            @Override
            public DocumentModel sanitize(DocumentModel document) {
                return document;
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IDocx docxStub() {
        return Stubs.announce(IDocx.class, new IDocx() {
            @Override
            public byte[] export(DocumentModel document, String title, IDiagramLinker diagramLinker) {
                return new byte[0];
            }

            @Override
            public DocxImportPreview importPreview(Actor actor, byte[] docx, BlockOrigin origin,
                                                   boolean diagramEnabled, IDiagramResolver diagramResolver) {
                return new DocxImportPreview(EMPTY_DOCUMENT, List.of());
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IGroupAssignmentSupport groupAssignmentSupportStub() {
        return Stubs.announce(IGroupAssignmentSupport.class,
                classId -> new ReadinessResult(List.of(IGroupAssignmentSupport.UNAVAILABLE), List.of()));
    }
}
