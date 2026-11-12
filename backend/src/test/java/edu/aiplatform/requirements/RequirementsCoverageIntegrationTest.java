package edu.aiplatform.requirements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.identity.integration.*;
import edu.aiplatform.identity.service.AccountAdminService;
import edu.aiplatform.jobs.service.IMessageSender;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.materials.service.*;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.subjects.service.SubjectStatusService;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Actual owner repositories/services, U01 JWT/current DB authorization, U02 audit and security filters. */
@SpringJUnitConfig(OwnerRequirementsTestConfig.class)
@WebAppConfiguration
@Testcontainers(disabledWithoutDocker = true)
class RequirementsCoverageIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withPassword(UUID.randomUUID().toString());
    static final Instant NOW = Instant.parse("2026-11-08T04:00:00Z");
    @Autowired DataSource dataSource;
    @Autowired WebApplicationContext context;
    @Autowired TransactionTemplate transaction;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwtService;
    @Autowired SessionStore sessionStore;
    @Autowired IArtifact artifact;
    @Autowired ISseHub sseHub;
    @Autowired IMessageSender messageSender;
    @Autowired SubjectStatusService subjectStatusService;
    @Autowired AccountAdminService accountAdminService;
    @Autowired IAuthorization authorization;
    @Autowired edu.aiplatform.subjects.service.SubjectAdminService subjectAdminService;
    @Autowired IMaterialRepository materialRepository;
    @Autowired IMaterialModuleRepository materialModuleRepository;
    @Autowired MaterialService materialService;
    @Autowired MaterialQueryService materialQueryService;
    @Autowired edu.aiplatform.ai.contract.IReferenceMaterial referenceMaterialService;
    @Autowired IAudit audit;
    @Autowired IMail mail;
    @Autowired edu.aiplatform.jobs.contract.IJob job;
    @Autowired edu.aiplatform.jobs.service.JobDispatcher jobDispatcher;
    @Autowired edu.aiplatform.notifications.service.EmailDeliveryService emailDeliveryService;
    @Autowired edu.aiplatform.notifications.repository.INotificationRepository notificationRepository;
    @Autowired edu.aiplatform.notifications.service.DeadlineReminderScanner reminderScanner;
    @Autowired edu.aiplatform.assignmentwork.contract.IGroupWorkStatus groupWorkStatus;
    @Autowired edu.aiplatform.assignments.contract.IAssignmentQuery assignmentQuery;
    @Autowired edu.aiplatform.notifications.service.NotificationEventService notificationEventService;
    @Autowired edu.aiplatform.quizzes.contract.IQuizQuery quizQuery;
    @Autowired edu.aiplatform.notifications.service.NotificationService notificationService;
    @Autowired edu.aiplatform.announcements.service.AnnouncementService announcementService;
    JdbcTemplate jdbc;
    MockMvc mvc;
    final Map<Role, UUID> accountIds = new EnumMap<>(Role.class);
    UUID subjectId, classId, otherClassId, foreignSubjectId, moduleId, materialId;

    @BeforeEach
    void seedOwners() {
        jdbc = new JdbcTemplate(dataSource);
        String tables = jdbc.queryForObject("select string_agg(quote_ident(tablename), ',') from pg_tables "
                + "where schemaname='public' and tablename <> 'flyway_schema_history'", String.class);
        jdbc.execute("truncate " + tables + " cascade");
        reset(sessionStore, artifact, sseHub, messageSender, quizQuery, mail, job, groupWorkStatus, assignmentQuery);
        for (Role role : Role.values()) {
            UUID accountId = UUID.randomUUID();
            accountIds.put(role, accountId);
            jdbc.update("""
                    insert into accounts(id,school_email,role,status,password_hash,display_name,created_at,updated_at,version)
                    values (?,?,?,'ACTIVE','fixture-password-hash',?,?,?,0)
                    """, accountId, accountId + "@fpt.edu.vn", role.name(), role.name(), at(NOW), at(NOW));
        }
        subjectId = seedSubject("SUBJECT", accountIds.get(Role.SUBJECT_MANAGER));
        foreignSubjectId = seedSubject("FOREIGN", null);
        classId = seedClass(subjectId, "OPEN", "OPEN");
        otherClassId = seedClass(subjectId, "OTHER", "OPEN");
        jdbc.update("insert into enrollments(class_id,account_id,status,source,enrolled_at) values (?,?,'ACTIVE','MANUAL',?)",
                classId, accountIds.get(Role.STUDENT), at(NOW));
        moduleId = seedModule(subjectId);
        materialId = seedMaterial(subjectId, null, moduleId, "Subject source", Material.Status.ACTIVE);
        when(artifact.issueDownloadToken(anyString(), any())).thenReturn(
                new DownloadToken("/api/v1/files/download/fixture", NOW.plusSeconds(300)));
        when(sseHub.subscribe(anyString())).thenAnswer(call -> new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    private Timestamp at(Instant time) {
        return Timestamp.from(time);
    }

    private UUID seedSubject(String code, UUID manager) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into subjects(id,code,name,status,manager_id,created_at,updated_at) values (?,?,?,'ACTIVE',?,?,?)",
                id, code, code, manager, at(NOW), at(NOW));
        return id;
    }

    private UUID seedClass(UUID subject, String code, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into course_classes(id,subject_id,code,name,status,teacher_id,created_at,updated_at)
                values (?,?,?,?,?,?,?,?)
                """, id, subject, code, code, status, accountIds.get(Role.TEACHER), at(NOW), at(NOW));
        return id;
    }

    private UUID seedModule(UUID subject) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into material_modules(id,subject_id,title,order_no,created_by,created_at,updated_at)
                values (?,?,'Module 1',0,?,?,?)
                """, id, subject, accountIds.get(Role.SUBJECT_MANAGER), at(NOW), at(NOW));
        return id;
    }

    private UUID seedMaterial(UUID subject, UUID course, UUID module, String title, Material.Status status) {
        UUID id = UUID.randomUUID();
        Material material = new Material(id, course == null ? Material.Scope.SUBJECT : Material.Scope.CLASS,
                subject, course, module, Material.Kind.FILE, title, "", 0, status, 1,
                accountIds.get(Role.SUBJECT_MANAGER), NOW, NOW, 0);
        transaction.executeWithoutResult(tx -> materialRepository.insert(material,
                new MaterialVersion(id, 1, "file-" + id, "source.pdf", "application/pdf", 100L,
                        null, null, MaterialVersion.TextStatus.EXTRACTED, "Original evidence",
                        accountIds.get(Role.SUBJECT_MANAGER), NOW)));
        return id;
    }

    private MockHttpServletRequestBuilder signedRequest(MockHttpServletRequestBuilder request, Role role) {
        UUID id = accountIds.get(role);
        String sid = "fixture-" + id;
        when(sessionStore.find(sid)).thenReturn(Optional.of(new SessionStore.SessionRecord(id, NOW, NOW, "hash")));
        String csrf = UUID.randomUUID().toString();
        return request.cookie(new Cookie("access_token", jwtService.issue(id, role, sid)),
                new Cookie("XSRF-TOKEN", csrf)).header("X-XSRF-TOKEN", csrf).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void realMaterialQueriesFilterForeignSubjectsClassesAndArchivedAttachments() {
        UUID own = seedMaterial(subjectId, classId, moduleId, "Own class", Material.Status.ACTIVE);
        UUID other = seedMaterial(subjectId, otherClassId, moduleId, "Other class", Material.Status.ACTIVE);
        UUID foreign = seedMaterial(foreignSubjectId, null, seedModule(foreignSubjectId), "Foreign", Material.Status.ACTIVE);
        UUID archived = seedMaterial(subjectId, classId, moduleId, "Archived", Material.Status.ARCHIVED);
        assertThat(materialRepository.list(subjectId, null)).extracting(Material::id).containsExactly(materialId);
        assertThat(materialRepository.list(subjectId, classId)).extracting(Material::id)
                .containsExactlyInAnyOrder(materialId, own, archived).doesNotContain(other, foreign);
        Actor student = new Actor(accountIds.get(Role.STUDENT), Role.STUDENT);
        assertThat(materialQueryService.classList(student, classId, true, List.of(own, other, foreign, archived)))
                .extracting(MaterialViews.Detail::id).containsExactly(own);
        assertThat(referenceMaterialService.listSelectable(student, ResourceRef.courseClass(classId)))
                .extracting(source -> source.materialId()).containsExactlyInAnyOrder(materialId, own);
    }

    @Test
    void materialHistoryAndConditionalTextCompletionSurviveMetadataReplaceArchiveAndStaleCas() {
        Actor manager = new Actor(accountIds.get(Role.SUBJECT_MANAGER), Role.SUBJECT_MANAGER);
        var changed = materialService.update(manager, materialId,
                new MaterialCommands.Update("Renamed", "Description", 1, 0L, null));
        assertThat(changed.currentVersionNo()).isEqualTo(1);
        assertThat(materialRepository.versions(materialId)).hasSize(1);
        when(artifact.attach(any(), eq(manager), eq(ArtifactPurpose.MATERIAL))).thenReturn(
                new StoredFileInfo("new-file", manager.accountId(), ArtifactPurpose.MATERIAL,
                        "new.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 200L, "sha"));
        materialService.replace(manager, materialId,
                new MaterialCommands.Replace(1L, new MaterialCommands.Source("signed", null)));
        transaction.executeWithoutResult(tx -> {
            assertThat(materialRepository.completeText(materialId, 2, MaterialVersion.TextStatus.EXTRACTED, "New evidence")).isTrue();
            assertThat(materialRepository.completeText(materialId, 2, MaterialVersion.TextStatus.FAILED, null)).isFalse();
            assertThat(materialRepository.completeText(materialId, 1, MaterialVersion.TextStatus.FAILED, null)).isFalse();
        });
        var text = referenceMaterialService.extractText(manager, List.of(materialId), 100).get(0);
        assertThat(text.versionNo()).isEqualTo(2);
        assertThat(text.text()).isEqualTo("New evidence");
        assertThatThrownBy(() -> materialService.archive(manager, materialId, 1))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        materialService.archive(manager, materialId, 2);
        assertThat(materialRepository.versions(materialId)).extracting(MaterialVersion::versionNo).containsExactly(2, 1);
        var history = materialQueryService.detail(manager, materialId, null);
        assertThat(history.versions()).hasSize(2);
        assertThat(history.canManage()).isFalse();
        materialQueryService.download(manager, materialId, null, 1);
        verify(artifact).issueDownloadToken("file-" + materialId, manager.accountId());
        assertThatThrownBy(() -> materialService.update(manager, materialId,
                new MaterialCommands.Update("Forbidden", "", 0, 3L, null)))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_ARCHIVED"));
    }

    @Test
    void composedArchiveAndReopenPreserveEnrollmentsAndLearningReferences() {
        UUID draft = seedClass(subjectId, "DRAFT", "DRAFT");
        UUID archived = seedClass(subjectId, "OLD", "ARCHIVED");
        UUID foreignClass = seedClass(foreignSubjectId, "FOREIGN", "OPEN");
        jdbc.update("""
                insert into enrollments(class_id,account_id,status,source,enrolled_at,removed_at)
                values (?,?,'REMOVED','MANUAL',?,?)
                """, otherClassId, accountIds.get(Role.STUDENT), at(NOW.minusSeconds(60)), at(NOW));
        var beforeEnrollments = jdbc.queryForList("select * from enrollments order by class_id,account_id");
        var beforeMaterials = jdbc.queryForList("select * from materials order by id");
        var beforeVersions = jdbc.queryForList("select * from material_versions order by material_id,version_no");
        Actor admin = new Actor(accountIds.get(Role.ADMIN), Role.ADMIN);
        subjectStatusService.changeStatus(admin, subjectId,
                new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L));
        assertThat(jdbc.queryForObject("select count(*) from course_classes where subject_id=? and status='ARCHIVED'",
                Integer.class, subjectId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select version from course_classes where id=?", Long.class, archived)).isZero();
        assertThat(jdbc.queryForObject("select status from course_classes where id=?", String.class, foreignClass)).isEqualTo("OPEN");
        assertThat(jdbc.queryForObject("select version from course_classes where id=?", Long.class, draft)).isEqualTo(1);
        assertThat(jdbc.queryForList("select * from enrollments order by class_id,account_id")).isEqualTo(beforeEnrollments);
        assertThat(jdbc.queryForList("select * from materials order by id")).isEqualTo(beforeMaterials);
        assertThat(jdbc.queryForList("select * from material_versions order by material_id,version_no")).isEqualTo(beforeVersions);
        assertThat(jdbc.queryForObject("select manager_id from subjects where id=?", UUID.class, subjectId)).isNull();
        assertThat(jdbc.queryForList("select action from audit_logs where object_id=? order by action", String.class,
                subjectId.toString())).containsExactly("SUBJECT_ARCHIVED", "SUBJECT_MANAGER_CHANGED");
        subjectStatusService.changeStatus(admin, subjectId,
                new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 2L));
        assertThat(jdbc.queryForObject("select count(*) from course_classes where subject_id=? and status='ARCHIVED'",
                Integer.class, subjectId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select manager_id from subjects where id=?", UUID.class, subjectId)).isNull();
        assertThat(jdbc.queryForList("select * from enrollments order by class_id,account_id")).isEqualTo(beforeEnrollments);
        assertThat(jdbc.queryForList("select * from materials order by id")).isEqualTo(beforeMaterials);
        assertThat(jdbc.queryForList("select * from material_versions order by material_id,version_no")).isEqualTo(beforeVersions);
    }

    private void rejectAudit(String action) {
        jdbc.execute("create sequence requirement_audit_reached");
        jdbc.execute("create or replace function reject_requirement_audit() returns trigger language plpgsql as $$ "
                + "begin if NEW.action='" + action + "' then perform nextval('requirement_audit_reached'); "
                + "raise exception 'fixture audit rejected'; end if; return NEW; end $$");
        jdbc.execute("create trigger reject_requirement_audit before insert on audit_logs for each row execute function reject_requirement_audit()");
    }

    private void clearAuditFailure() {
        jdbc.execute("drop trigger if exists reject_requirement_audit on audit_logs");
        jdbc.execute("drop function if exists reject_requirement_audit()");
        jdbc.execute("drop sequence if exists requirement_audit_reached");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"CLASS_STATUS_CHANGED", "SUBJECT_MANAGER_CHANGED"})
    void actualAuditFailureRollsBackSubjectClassAndManagerTogether(String action) {
        var subjectsBefore = jdbc.queryForList("select * from subjects order by id");
        var classesBefore = jdbc.queryForList("select * from course_classes order by id");
        var enrollmentsBefore = jdbc.queryForList("select * from enrollments order by class_id,account_id");
        rejectAudit(action);
        boolean failureReached;
        try {
            assertThatThrownBy(() -> subjectStatusService.changeStatus(new Actor(accountIds.get(Role.ADMIN), Role.ADMIN),
                    subjectId, new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L)))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            failureReached = jdbc.queryForObject("select is_called from requirement_audit_reached", Boolean.class);
            clearAuditFailure();
        }
        assertThat(failureReached).as("the rejecting audit actually ran after business writes").isTrue();
        assertThat(jdbc.queryForList("select * from subjects order by id")).isEqualTo(subjectsBefore);
        assertThat(jdbc.queryForList("select * from course_classes order by id")).isEqualTo(classesBefore);
        assertThat(jdbc.queryForList("select * from enrollments order by class_id,account_id")).isEqualTo(enrollmentsBefore);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    @Test
    void accountDisableReleasesAllSubjectsAndReopenDoesNotRestoreThem() {
        UUID manager = accountIds.get(Role.SUBJECT_MANAGER);
        UUID second = seedSubject("SECOND", manager);
        Actor admin = new Actor(accountIds.get(Role.ADMIN), Role.ADMIN);
        accountAdminService.changeStatus(admin, manager, AccountStatus.DISABLED, 0);
        assertThat(jdbc.queryForObject("select count(*) from subjects where manager_id=?", Integer.class, manager)).isZero();
        assertThat(jdbc.queryForObject("select version from subjects where id=?", Long.class, second)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='SUBJECT_MANAGER_CHANGED' and actor_id=?",
                Integer.class, admin.accountId())).isEqualTo(2);
        accountAdminService.changeStatus(admin, manager, AccountStatus.ACTIVE, 1);
        assertThat(jdbc.queryForObject("select count(*) from subjects where manager_id=?", Integer.class, manager)).isZero();
    }

    @Test
    void failedSecondManagerReleaseRollsBackAccountAndAllAssignmentsAndAudits() {
        UUID manager = accountIds.get(Role.SUBJECT_MANAGER);
        seedSubject("SECOND", manager);
        jdbc.execute("create sequence requirement_audit_reached");
        jdbc.execute("create function reject_requirement_audit() returns trigger language plpgsql as $$ begin "
                + "if NEW.action='SUBJECT_MANAGER_CHANGED' and exists(select 1 from audit_logs where action='SUBJECT_MANAGER_CHANGED') "
                + "then perform nextval('requirement_audit_reached'); raise exception 'second audit rejected'; end if; return NEW; end $$");
        jdbc.execute("create trigger reject_requirement_audit before insert on audit_logs for each row execute function reject_requirement_audit()");
        var before = jdbc.queryForList("select * from subjects order by id");
        boolean secondAuditReached;
        try {
            assertThatThrownBy(() -> accountAdminService.changeStatus(new Actor(accountIds.get(Role.ADMIN), Role.ADMIN),
                    manager, AccountStatus.DISABLED, 0)).isInstanceOf(RuntimeException.class);
        } finally {
            secondAuditReached = jdbc.queryForObject("select is_called from requirement_audit_reached", Boolean.class);
            clearAuditFailure();
        }
        assertThat(secondAuditReached).as("second manager audit reached after first manager release").isTrue();
        assertThat(jdbc.queryForObject("select status from accounts where id=?", String.class, manager)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForList("select * from subjects order by id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void realJwtCurrentRoleAndCsrfProtectSubjectEndpoints(Role role) throws Exception {
        String root = "/api/v1/admin/subjects";
        int expected = role == Role.ADMIN ? 200 : 403;
        mvc.perform(signedRequest(get(root), role)).andExpect(status().is(expected));
        mvc.perform(signedRequest(get(root + "/" + subjectId), role)).andExpect(status().is(expected));
        mvc.perform(signedRequest(get(root + "/" + subjectId + "/classes"), role)).andExpect(status().is(expected));
        List<java.util.function.Supplier<MockHttpServletRequestBuilder>> writes = List.of(
                () -> post(root).content("{\"code\":\"NEW\",\"name\":\"New\"}"),
                () -> patch(root + "/" + subjectId).content("{\"name\":\"Changed\",\"description\":\"\",\"version\":1}"),
                () -> put(root + "/" + subjectId + "/manager").content("{\"managerId\":null,\"version\":1}"),
                () -> put(root + "/" + subjectId + "/status").content("{\"status\":\"ARCHIVED\",\"version\":1}"));
        for (var write : writes) {
            for (boolean mismatch : List.of(false, true)) {
                mvc.perform(signedRequest(write.get(), role).with(http -> {
                    http.removeHeader("X-XSRF-TOKEN");
                    if (mismatch) http.addHeader("X-XSRF-TOKEN", "wrong");
                    return http;
                })).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
            }
            if (role != Role.ADMIN) {
                mvc.perform(signedRequest(write.get(), role)).andExpect(status().isForbidden());
            }
        }
        assertThat(jdbc.queryForObject("select version from subjects where id=?", Long.class, subjectId)).isZero();
        if (role != Role.ADMIN) {
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ACCESS_DENIED' and actor_id=?",
                    Integer.class, accountIds.get(role))).isEqualTo(7);
        }
        if (role == Role.ADMIN) {
            mvc.perform(signedRequest(writes.get(0).get(), role)).andExpect(status().isCreated());
            mvc.perform(signedRequest(patch(root + "/" + subjectId)
                    .content("{\"name\":\"Changed\",\"description\":\"\",\"version\":1}"), role))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
            mvc.perform(signedRequest(put(root + "/" + subjectId + "/manager")
                    .content("{\"managerId\":null,\"version\":2}"), role))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3));
            mvc.perform(signedRequest(put(root + "/" + subjectId + "/status")
                    .content("{\"status\":\"ARCHIVED\",\"version\":3}"), role))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(4));
        }
    }

    @Test
    void realManagerScopeChangesOnNextAuthorizationWithoutCachedGrant() {
        Actor manager = new Actor(accountIds.get(Role.SUBJECT_MANAGER), Role.SUBJECT_MANAGER);
        assertThat(authorization.authorize(manager, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(subjectId)).allowed()).isTrue();
        jdbc.update("update subjects set manager_id=null where id=?", subjectId);
        assertThat(authorization.authorize(manager, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(subjectId)).allowed()).isFalse();
        jdbc.update("update subjects set manager_id=? where id=?", manager.accountId(), subjectId);
        assertThat(authorization.authorize(manager, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(subjectId)).allowed()).isTrue();
        jdbc.update("update accounts set status='DISABLED' where id=?", manager.accountId());
        assertThat(authorization.authorize(manager, Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(subjectId)).allowed()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void notificationEndpointsUseRealCurrentPrincipalAndPreserveForeignUnreadState(Role role) throws Exception {
        UUID owner = accountIds.get(role);
        UUID foreign = UUID.randomUUID();
        notificationService.create(UUID.randomUUID(), edu.aiplatform.notifications.model.NotificationType.ENROLLED,
                List.of(owner, foreign), "Notice", "Open class", "/classes/fixture", null, null);
        UUID ownId = jdbc.queryForObject("select id from notifications where recipient_id=?", UUID.class, owner);
        UUID foreignId = jdbc.queryForObject("select id from notifications where recipient_id=?", UUID.class, foreign);
        mvc.perform(signedRequest(get("/api/v1/me/notifications").param("accountId", foreign.toString()), role))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(ownId.toString()));
        mvc.perform(signedRequest(get("/api/v1/me/notifications/unread-count"), role))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(signedRequest(get("/api/v1/me/notifications/events"), role))
                .andExpect(status().isOk()).andExpect(request().asyncStarted());
        verify(sseHub).subscribe("notifications:account:" + owner);
        mvc.perform(signedRequest(post("/api/v1/me/notifications/" + foreignId + ":read"), role))
                .andExpect(status().isNotFound());
        mvc.perform(signedRequest(post("/api/v1/me/notifications/" + ownId + ":read"), role))
                .andExpect(status().isNoContent());
        mvc.perform(signedRequest(post("/api/v1/me/notifications:read-all")
                .content("{\"accountId\":\"" + foreign + "\"}"), role)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select read_at from notifications where id=?", Timestamp.class, foreignId)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ACCESS_DENIED' and actor_id=?",
                Integer.class, owner)).isEqualTo(1);
        mvc.perform(signedRequest(get("/api/v1/me/notification-preferences"), role)).andExpect(status().isOk());
        String type = switch (role) {
            case ADMIN -> "BACKUP_FAILED";
            case STUDENT -> "ENROLLED";
            case TEACHER, SUBJECT_MANAGER -> "QUESTION_REVIEWED";
        };
        mvc.perform(signedRequest(put("/api/v1/me/notification-preferences")
                .content("{\"" + type + "\":false}"), role))
                .andExpect(status().is(role == Role.ADMIN ? 400 : 200));
        if (role != Role.ADMIN) {
            assertThat(jdbc.queryForObject("select email_enabled from notification_preferences where account_id=? and type=?",
                    Boolean.class, owner, type)).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void announcementEndpointsEnforceTeachingEnrollmentVersionAndSoftDelete(Role role) throws Exception {
        boolean staff = role == Role.TEACHER || role == Role.SUBJECT_MANAGER;
        if (staff) jdbc.update("update course_classes set teacher_id=? where id=?", accountIds.get(role), classId);
        Actor currentTeacher = new Actor(accountIds.get(staff ? role : Role.TEACHER), staff ? role : Role.TEACHER);
        var created = announcementService.create(currentTeacher, classId,
                new edu.aiplatform.announcements.service.AnnouncementCommands.Create("Original", "Body", false, false));
        int readStatus = role == Role.ADMIN ? 404 : 200;
        mvc.perform(signedRequest(get("/api/v1/classes/" + classId + "/announcements"), role))
                .andExpect(status().is(readStatus));
        mvc.perform(signedRequest(get("/api/v1/me/announcements").param("classId", classId.toString()), role))
                .andExpect(status().is(readStatus));
        mvc.perform(signedRequest(post("/api/v1/classes/" + classId + "/announcements")
                .content("{\"title\":\"New\",\"body\":\"**Body**\",\"important\":false,\"sendEmail\":false}"), role))
                .andExpect(status().is(staff ? 201 : 404));
        mvc.perform(signedRequest(patch("/api/v1/announcements/" + created.id())
                .content("{\"title\":\"Edited\",\"body\":\"Edited body\",\"version\":0}"), role))
                .andExpect(status().is(staff ? 200 : 404));
        mvc.perform(signedRequest(delete("/api/v1/announcements/" + created.id())
                .param("version", staff ? "1" : "0"), role)).andExpect(status().is(staff ? 204 : 404));
        assertThat(jdbc.queryForObject("select status from announcements where id=?", String.class, created.id()))
                .isEqualTo(staff ? "DELETED" : "ACTIVE");
        assertThat(jdbc.queryForObject("select created_by from announcements where id=?", UUID.class, created.id()))
                .isEqualTo(currentTeacher.accountId());
    }

    @Test
    void enrollmentRemovalAndTeacherReassignmentImmediatelyRevokeRealHttpRead() throws Exception {
        mvc.perform(signedRequest(get("/api/v1/materials/" + materialId), Role.STUDENT)).andExpect(status().isOk());
        jdbc.update("update enrollments set status='REMOVED',removed_at=? where class_id=?", at(NOW), classId);
        mvc.perform(signedRequest(get("/api/v1/materials/" + materialId), Role.STUDENT)).andExpect(status().isNotFound());
        jdbc.update("update course_classes set teacher_id=? where subject_id=?", accountIds.get(Role.SUBJECT_MANAGER), subjectId);
        mvc.perform(signedRequest(get("/api/v1/materials/" + materialId), Role.TEACHER)).andExpect(status().isNotFound());
    }

    @Test
    void localOwnerReadLatencyMeetsDeclaredP95ForTenMaterialsAndTwoHundredNotices() {
        for (int index = 0; index < 9; index++) {
            seedMaterial(subjectId, null, moduleId, "Source " + index, Material.Status.ACTIVE);
        }
        jdbc.update("""
                insert into notifications(id,recipient_id,type,source_event_id,title,summary,link,created_at)
                select gen_random_uuid(),?,'ENROLLED',gen_random_uuid(),'Notice','Open class','/classes/fixture',?
                from generate_series(1,200)
                """, accountIds.get(Role.STUDENT), at(NOW));
        jdbc.update("""
                insert into announcements(id,class_id,title,body,important,send_email,status,created_by,created_at,updated_by,updated_at)
                select gen_random_uuid(),?,'Announcement','Body',false,false,'ACTIVE',?,?,?,?
                from generate_series(1,20)
                """, classId, accountIds.get(Role.TEACHER), at(NOW), accountIds.get(Role.TEACHER), at(NOW));
        Actor admin = new Actor(accountIds.get(Role.ADMIN), Role.ADMIN);
        Actor manager = new Actor(accountIds.get(Role.SUBJECT_MANAGER), Role.SUBJECT_MANAGER);
        Actor student = new Actor(accountIds.get(Role.STUDENT), Role.STUDENT);
        var ids = materialRepository.list(subjectId, null).stream().map(Material::id).toList();
        assertThat(ids).hasSize(10);
        measureP95("U04_SCOPE", 20, () -> assertThat(authorization.authorize(manager,
                Actions.MATERIAL_SUBJECT_MANAGE, ResourceRef.subject(subjectId)).allowed()).isTrue());
        measureP95("U04_LIST", 300, () -> subjectAdminService.list(admin, null, null, 0, 20));
        measureP95("U04_DETAIL", 300, () -> subjectAdminService.detail(admin, subjectId));
        measureP95("U13_LIST", 300, () -> assertThat(materialQueryService.subjectList(manager, subjectId)).hasSize(10));
        measureP95("U13_EXTRACT_TEN", 1000,
                () -> assertThat(referenceMaterialService.extractText(manager, ids, 10000)).hasSize(10));
        measureP95("U14_NOTIFICATIONS", 300,
                () -> assertThat(notificationService.list(student, false, 1, 20).items()).hasSize(20));
        measureP95("U14_ANNOUNCEMENTS", 300,
                () -> assertThat(announcementService.list(student, classId, 1, 20).items()).hasSize(20));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    private void measureP95(String criterion, long maximumMillis, Runnable read) {
        for (int index = 0; index < 5; index++) read.run();
        long[] samples = new long[50];
        for (int index = 0; index < samples.length; index++) {
            long started = System.nanoTime();
            read.run();
            samples[index] = System.nanoTime() - started;
        }
        Arrays.sort(samples);
        long p95 = samples[(int) Math.ceil(samples.length * 0.95) - 1];
        double milliseconds = p95 / 1_000_000.0;
        System.out.println("LOCAL_P95 criterion=" + criterion + " samples=50 elapsedMs=" + milliseconds
                + " targetMs=" + maximumMillis);
        assertThat(milliseconds).as("local declared fixture " + criterion).isLessThanOrEqualTo(maximumMillis);
    }

    @Test
    void actualGroupLookupFailureRollsBackReminderMarkerAndRecoveryNotifiesExactlyOnce() {
        UUID assignmentId = UUID.randomUUID();
        var assignment = new edu.aiplatform.assignments.contract.AssignmentView(assignmentId,
                edu.aiplatform.assignments.contract.AssignmentScope.CLASS, classId, subjectId, 1,
                "Group assignment", edu.aiplatform.assignments.contract.SubmissionMode.GROUP,
                edu.aiplatform.assignments.contract.GradingMode.GRADED, false,
                edu.aiplatform.assignments.contract.AssignmentStatus.OPEN, NOW.minusSeconds(60),
                NOW.plusSeconds(86400), null, null, false, null, List.of());
        when(assignmentQuery.listClosingBetween(any(), any())).thenReturn(List.of(assignment));
        when(assignmentQuery.get(assignmentId)).thenReturn(Optional.of(assignment));
        when(groupWorkStatus.statusFor(accountIds.get(Role.STUDENT), List.of(assignmentId)))
                .thenThrow(new IllegalStateException("PRIVATE_GROUP_FAILURE"))
                .thenReturn(Map.of(assignmentId, new edu.aiplatform.assignmentwork.contract.GroupWorkStatus(
                        assignmentId, null, false, null, false)));
        reminderScanner.scan(NOW);
        for (String table : List.of("deadline_reminders", "notifications", "email_deliveries")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class)).as(table).isZero();
        }
        reminderScanner.scan(NOW);
        reminderScanner.scan(NOW);
        for (String table : List.of("deadline_reminders", "notifications", "email_deliveries")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Integer.class)).as(table).isEqualTo(1);
        }
        verify(groupWorkStatus, times(2)).statusFor(accountIds.get(Role.STUDENT), List.of(assignmentId));
    }

    @Test
    void classEventCreatesExactlyTwoHundredNoticesAndRecordsLocalFanoutMeasurement() {
        jdbc.update("""
                insert into accounts(id,school_email,role,status,password_hash,display_name,created_at,updated_at)
                select id,id::text||'@fpt.edu.vn','STUDENT','ACTIVE','fixture-hash','Student',?,?
                from (select gen_random_uuid() id from generate_series(1,199)) students
                """, at(NOW), at(NOW));
        jdbc.update("""
                insert into enrollments(class_id,account_id,status,source,enrolled_at)
                select ?,id,'ACTIVE','MANUAL',? from accounts where role='STUDENT'
                on conflict do nothing
                """, classId, at(NOW));
        var event = new edu.aiplatform.jobs.contract.DomainEvent(1, UUID.randomUUID(),
                edu.aiplatform.jobs.contract.EventTypes.CLASS_ANNOUNCEMENT_POSTED, "U14", NOW, null,
                Map.of("classId", classId.toString(), "actorId", accountIds.get(Role.TEACHER).toString(),
                        "announcementId", UUID.randomUUID().toString(), "important", false, "sendEmail", false));
        long started = System.nanoTime();
        notificationEventService.accept(event);
        long milliseconds = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertThat(jdbc.queryForObject("select count(*) from notifications", Integer.class)).isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(*) from email_deliveries", Integer.class)).isZero();
        notificationEventService.accept(event);
        assertThat(jdbc.queryForObject("select count(*) from notifications", Integer.class)).isEqualTo(200);
        System.out.println("LOCAL_NFR_U14_03 recipients=200 elapsedMs=" + milliseconds + " targetMs=5000");
        assertThat(milliseconds).isLessThanOrEqualTo(5000);
    }

    @Test
    void actualEmailJobPipelineRetriesBoundedlyPersistsFailureAndKeepsBusinessNotice() {
        UUID recipient = accountIds.get(Role.STUDENT);
        notificationService.create(UUID.randomUUID(), edu.aiplatform.notifications.model.NotificationType.ENROLLED,
                List.of(recipient), "Enrollment", "Open class", "/classes/fixture", "ENROLLED", null);
        UUID deliveryId = jdbc.queryForObject("select id from email_deliveries", UUID.class);
        doThrow(new IllegalStateException("PRIVATE_SMTP_RECIPIENT_SENTINEL")).when(mail).send(any());
        var current = new edu.aiplatform.jobs.contract.JobMessage(1, edu.aiplatform.jobs.contract.JobTypes.EMAIL_SEND,
                "email:" + deliveryId, Map.of("deliveryId", deliveryId.toString()), 0, "fixture-correlation");
        var retry = org.mockito.ArgumentCaptor.forClass(edu.aiplatform.jobs.contract.JobMessage.class);
        for (int attempt = 0; attempt < 5; attempt++) {
            clearInvocations(messageSender);
            assertThat(jobDispatcher.dispatch(current)).isEqualTo(edu.aiplatform.jobs.service.JobDispatcher.Outcome.RETRIED);
            verify(messageSender).sendRetry(retry.capture(), any());
            current = retry.getValue();
            assertThat(current.attempt()).isEqualTo(attempt + 1);
            assertThat(current.payload()).containsEntry("deliveryId", deliveryId.toString());
        }
        assertThat(jobDispatcher.dispatch(current)).isEqualTo(edu.aiplatform.jobs.service.JobDispatcher.Outcome.FAILED);
        assertThat(jdbc.queryForObject("select status from email_deliveries where id=?", String.class, deliveryId)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select attempts from email_deliveries where id=?", Integer.class, deliveryId)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select last_error from email_deliveries where id=?", String.class, deliveryId))
                .isEqualTo("MAIL_RETRIES_EXHAUSTED");
        assertThat(jdbc.queryForObject("select count(*) from notifications", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    @Test
    void concurrentDeliveryAndDuplicateJobsSendOnceAndSentRemainsTerminal() throws Exception {
        notificationService.create(UUID.randomUUID(), edu.aiplatform.notifications.model.NotificationType.ENROLLED,
                List.of(accountIds.get(Role.STUDENT)), "Enrollment", "Open class", "/classes/fixture", "ENROLLED", null);
        UUID deliveryId = jdbc.queryForObject("select id from email_deliveries", UUID.class);
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var futures = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
                start.await();
                emailDeliveryService.send(deliveryId);
                return null;
            })).toList();
            start.countDown();
            for (var future : futures) future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        var replay = new edu.aiplatform.jobs.contract.JobMessage(1, edu.aiplatform.jobs.contract.JobTypes.EMAIL_SEND,
                "email:" + deliveryId, Map.of("deliveryId", deliveryId.toString()), 0, "fixture");
        assertThat(jobDispatcher.dispatch(replay)).isEqualTo(edu.aiplatform.jobs.service.JobDispatcher.Outcome.DONE);
        emailDeliveryService.failed(deliveryId);
        verify(mail).send(any());
        assertThat(jdbc.queryForObject("select status from email_deliveries where id=?", String.class, deliveryId)).isEqualTo("SENT");
        assertThat(jdbc.queryForObject("select attempts from email_deliveries where id=?", Integer.class, deliveryId)).isEqualTo(1);
    }

    @Test
    void retentionBatchBoundAndNextBatchUseActualRepository() {
        jdbc.update("""
                insert into notifications(id,recipient_id,type,source_event_id,title,summary,link,created_at)
                select gen_random_uuid(),?,'ENROLLED',gen_random_uuid(),'Notice','Open class','/classes/fixture',?
                from generate_series(1,10001)
                """, accountIds.get(Role.STUDENT), at(NOW.minus(Duration.ofDays(181))));
        int first = transaction.execute(status -> notificationRepository.deleteOld(NOW.minus(Duration.ofDays(180))));
        int second = transaction.execute(status -> notificationRepository.deleteOld(NOW.minus(Duration.ofDays(180))));
        assertThat(first).isEqualTo(10000);
        assertThat(second).isEqualTo(1);
    }

    @Test
    void blockedClassWriteCannotOutliveLifecycleBudget() throws Exception {
        Actor admin = new Actor(accountIds.get(Role.ADMIN), Role.ADMIN);
        subjectStatusService.changeStatus(admin, subjectId,
                new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 1L));
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> archive = null;
        try (var blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (var lock = blocker.prepareStatement("select id from course_classes where id=? for update")) {
                lock.setObject(1, classId);
                lock.executeQuery().close();
            }
            archive = executor.submit(() -> subjectStatusService.changeStatus(admin, subjectId,
                    new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L)));
            final var completion = archive;
            try {
                assertThatThrownBy(() -> completion.get(2500, java.util.concurrent.TimeUnit.MILLISECONDS))
                        .isInstanceOf(java.util.concurrent.ExecutionException.class)
                        .hasCauseInstanceOf(edu.aiplatform.shared.error.ApiException.class);
            } finally {
                blocker.rollback();
            }
        } finally {
            if (archive != null) {
                try { archive.get(5, java.util.concurrent.TimeUnit.SECONDS); }
                catch (java.util.concurrent.ExecutionException expectedFailure) { }
            }
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select status from subjects where id=?", String.class, subjectId)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select status from course_classes where id=?", String.class, classId)).isEqualTo("OPEN");
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    @Test
    void actualAnnouncementCommitPublishesAfterCommitAndBrokerFailureCannotUndoSource() throws Exception {
        var published = new java.util.concurrent.LinkedBlockingQueue<edu.aiplatform.jobs.contract.DomainEvent>();
        doAnswer(call -> { published.add(call.getArgument(0)); return null; }).when(messageSender).sendEvent(any());
        Actor teacher = new Actor(accountIds.get(Role.TEACHER), Role.TEACHER);
        transaction.executeWithoutResult(status -> {
            announcementService.create(teacher, classId,
                    new edu.aiplatform.announcements.service.AnnouncementCommands.Create("Rolled back", "Body", false, false));
            assertThat(published).isEmpty();
            status.setRollbackOnly();
        });
        assertThat(published.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from announcements", Integer.class)).isZero();
        transaction.executeWithoutResult(status -> {
            announcementService.create(teacher, classId,
                    new edu.aiplatform.announcements.service.AnnouncementCommands.Create("Committed", "Body", false, false));
            assertThat(published).isEmpty();
        });
        assertThat(published.poll(3, java.util.concurrent.TimeUnit.SECONDS)).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from announcements", Integer.class)).isEqualTo(1);
        clearInvocations(messageSender);
        doThrow(new IllegalStateException("PRIVATE_PROVIDER_MESSAGE")).when(messageSender).sendEvent(any());
        announcementService.create(teacher, classId,
                new edu.aiplatform.announcements.service.AnnouncementCommands.Create("Survives failure", "Body", false, false));
        verify(messageSender, timeout(3000)).sendEvent(any());
        assertThat(jdbc.queryForObject("select count(*) from announcements", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ANNOUNCEMENT_CREATED'", Integer.class)).isEqualTo(2);
    }

    @Test
    void materialWriteRoutesRejectMissingAndMismatchedCsrfBeforeAnyOwnerWrite() throws Exception {
        var before = jdbc.queryForList("select * from materials order by id");
        for (HttpCase endpoint : materialRoutesList()) {
            if (endpoint.method().equals("GET")) continue;
            Role role = endpoint.allowed().iterator().next();
            for (boolean mismatch : List.of(false, true)) {
                var request = signedRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                        org.springframework.http.HttpMethod.valueOf(endpoint.method()), routePath(endpoint.path()))
                        .content(routeBody(endpoint.body())), role);
                mvc.perform(request.with(http -> {
                    http.removeHeader("X-XSRF-TOKEN");
                    if (mismatch) http.addHeader("X-XSRF-TOKEN", "mismatched");
                    return http;
                })).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
            }
        }
        assertThat(jdbc.queryForList("select * from materials order by id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    @Test
    void moduleDeleteMovesArchivedSourcesWithoutChangingTheirHistory() throws Exception {
        UUID destination = seedModule(subjectId);
        jdbc.update("update materials set status='ARCHIVED' where id=?", materialId);
        var versions = jdbc.queryForList("select * from material_versions order by material_id,version_no");
        mvc.perform(signedRequest(delete("/api/v1/material-modules/" + moduleId + "?version=0"),
                        Role.SUBJECT_MANAGER))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from material_modules where id=?", Integer.class, moduleId)).isZero();
        assertThat(jdbc.queryForObject("select module_id from materials where id=?", UUID.class, materialId)).isEqualTo(destination);
        assertThat(jdbc.queryForList("select * from material_versions order by material_id,version_no")).isEqualTo(versions);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='MATERIAL_MODULE_DELETED' and object_id=?",
                Integer.class, moduleId.toString())).isEqualTo(1);
    }

    @Test
    void moduleDeleteAuditFailureRollsBackDeleteAndArchivedSourceMove() throws Exception {
        seedModule(subjectId);
        jdbc.update("update materials set status='ARCHIVED' where id=?", materialId);
        var modules = jdbc.queryForList("select * from material_modules order by id");
        var materials = jdbc.queryForList("select * from materials order by id");
        var versions = jdbc.queryForList("select * from material_versions order by material_id,version_no");
        rejectAudit("MATERIAL_MODULE_DELETED");
        Boolean failureReached;
        try {
            mvc.perform(signedRequest(delete("/api/v1/material-modules/" + moduleId + "?version=0"),
                            Role.SUBJECT_MANAGER))
                    .andExpect(status().is5xxServerError());
            failureReached = jdbc.queryForObject("select is_called from requirement_audit_reached", Boolean.class);
        } finally {
            clearAuditFailure();
        }
        assertThat(failureReached).isTrue();
        assertThat(jdbc.queryForList("select * from material_modules order by id")).isEqualTo(modules);
        assertThat(jdbc.queryForList("select * from materials order by id")).isEqualTo(materials);
        assertThat(jdbc.queryForList("select * from material_versions order by material_id,version_no")).isEqualTo(versions);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs", Integer.class)).isZero();
    }

    @Test
    void moduleReorderRollsBackEarlierRowsWhenLaterCasLosesAndCanRetry() throws Exception {
        UUID second = seedModule(subjectId);
        var before = jdbc.queryForList("select * from material_modules order by id");
        String body = "{\"items\":[{\"id\":\"" + second + "\",\"version\":0},{\"id\":\""
                + moduleId + "\",\"version\":0}]}";
        jdbc.execute("create sequence requirement_module_updates");
        jdbc.execute("create function lose_second_module_update() returns trigger language plpgsql as $$ begin "
                + "if nextval('requirement_module_updates')=2 then return null; end if; return new; end $$");
        jdbc.execute("create trigger lose_second_module_update before update on material_modules "
                + "for each row execute function lose_second_module_update()");
        Long updatesReached;
        try {
            mvc.perform(signedRequest(put("/api/v1/subjects/" + subjectId + "/modules:reorder"),
                            Role.SUBJECT_MANAGER).content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("MATERIAL_VERSION_CONFLICT"));
            updatesReached = jdbc.queryForObject("select last_value from requirement_module_updates", Long.class);
        } finally {
            jdbc.execute("drop trigger lose_second_module_update on material_modules");
            jdbc.execute("drop function lose_second_module_update()");
            jdbc.execute("drop sequence requirement_module_updates");
        }
        assertThat(updatesReached).isEqualTo(2L);
        assertThat(jdbc.queryForList("select * from material_modules order by id")).isEqualTo(before);
        mvc.perform(signedRequest(put("/api/v1/subjects/" + subjectId + "/modules:reorder"),
                        Role.SUBJECT_MANAGER).content(body))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from material_modules where subject_id=? and version=1",
                Integer.class, subjectId)).isEqualTo(2);
        assertThat(jdbc.queryForList("select id from material_modules where subject_id=? order by order_no",
                UUID.class, subjectId)).containsExactly(second, moduleId);
    }

    record HttpCase(String method, String path, String body, Set<Role> allowed, int success) {}

    private static List<HttpCase> materialRoutesList() {
        Set<Role> manager = Set.of(Role.SUBJECT_MANAGER);
        Set<Role> teacher = Set.of(Role.TEACHER);
        Set<Role> student = Set.of(Role.STUDENT);
        Set<Role> readers = Set.of(Role.SUBJECT_MANAGER, Role.TEACHER, Role.STUDENT);
        String create = "{\"kind\":\"YOUTUBE_LINK\",\"title\":\"Video\",\"description\":\"\",\"orderNo\":0,"
                + "\"source\":{\"videoUrl\":\"https://youtu.be/abcdefghijk\"},\"moduleId\":\"{module}\"}";
        String position = "{\"items\":[{\"id\":\"{id}\",\"version\":0}]}";
        return List.of(
                new HttpCase("GET", "/subjects/{subject}/materials", "", manager, 200),
                new HttpCase("POST", "/subjects/{subject}/materials", create, manager, 201),
                new HttpCase("PUT", "/subjects/{subject}/materials:reorder", position, manager, 204),
                new HttpCase("GET", "/classes/{class}/materials", "", teacher, 200),
                new HttpCase("POST", "/classes/{class}/materials", create, teacher, 201),
                new HttpCase("PUT", "/classes/{class}/materials:reorder",
                        position.replace("{id}", "{classMaterial}"), teacher, 204),
                new HttpCase("GET", "/me/classes/{class}/materials", "", student, 200),
                new HttpCase("GET", "/materials/{id}", "", readers, 200),
                new HttpCase("GET", "/materials/{id}/view", "", readers, 200),
                new HttpCase("PATCH", "/materials/{id}",
                        "{\"title\":\"Renamed\",\"description\":\"\",\"orderNo\":0,\"version\":0}", manager, 200),
                new HttpCase("POST", "/materials/{id}/versions",
                        "{\"version\":0,\"source\":{\"fileRef\":\"signed\"}}", manager, 200),
                new HttpCase("POST", "/materials/{id}/archive", "{\"version\":0}", manager, 200),
                new HttpCase("POST", "/materials/{id}/download-token", "", readers, 200),
                new HttpCase("GET", "/subjects/{subject}/modules", "", manager, 200),
                new HttpCase("POST", "/subjects/{subject}/modules", "{\"title\":\"New module\"}", manager, 201),
                new HttpCase("PUT", "/subjects/{subject}/modules:reorder",
                        position.replace("{id}", "{module}"), manager, 204),
                new HttpCase("GET", "/classes/{class}/modules", "", teacher, 200),
                new HttpCase("GET", "/me/classes/{class}/modules", "", student, 200),
                new HttpCase("PATCH", "/material-modules/{module}", "{\"title\":\"Renamed\",\"version\":0}", manager, 204),
                new HttpCase("DELETE", "/material-modules/{module}?version=0", "", manager, 409),
                new HttpCase("POST", "/material-modules/{module}/quizzes", "{\"quizId\":\"{quiz}\"}", manager, 204),
                new HttpCase("DELETE", "/material-modules/{module}/quizzes/{quiz}", "", manager, 204));
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> materialRoutes() {
        return materialRoutesList().stream().flatMap(endpoint -> Arrays.stream(Role.values())
                .map(role -> org.junit.jupiter.params.provider.Arguments.of(endpoint, role)));
    }

    private String routePath(String value) {
        return "/api/v1" + routeBody(value);
    }

    private String routeBody(String value) {
        return value.replace("{subject}", subjectId.toString()).replace("{class}", classId.toString())
                .replace("{id}", materialId.toString()).replace("{module}", moduleId.toString())
                .replace("{classMaterial}", routeClassMaterial.toString()).replace("{quiz}", routeQuiz.toString());
    }

    UUID routeClassMaterial = UUID.randomUUID(), routeQuiz = UUID.randomUUID();

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("materialRoutes")
    void materialEndpointsEnforceAllFourRolesWithRealJwtDatabaseScopesAndFilters(HttpCase endpoint, Role role) throws Exception {
        routeClassMaterial = seedMaterial(subjectId, classId, moduleId, "Private class", Material.Status.ACTIVE);
        jdbc.update("""
                insert into quizzes(id,scope,subject_id,title,settings,created_by,created_at,updated_at)
                values (?,'SUBJECT',?,'Quiz','{}'::jsonb,?,?,?)
                """, routeQuiz, subjectId, accountIds.get(Role.SUBJECT_MANAGER), at(NOW), at(NOW));
        when(quizQuery.get(routeQuiz)).thenReturn(Optional.of(new edu.aiplatform.quizzes.contract.QuizView(
                routeQuiz, edu.aiplatform.quizzes.contract.QuizScope.SUBJECT, null, subjectId, 1,
                "Quiz", edu.aiplatform.quizzes.contract.QuizStatus.DRAFT, null, false, null, java.math.BigDecimal.TEN, null)));
        if (endpoint.method().equals("DELETE") && endpoint.path().contains("/quizzes/")) {
            jdbc.update("insert into material_module_quizzes(module_id,quiz_id,order_no,created_at) values (?,?,0,?)",
                    moduleId, routeQuiz, at(NOW));
        }
        when(artifact.attach(any(), any(), eq(ArtifactPurpose.MATERIAL))).thenAnswer(call -> {
            Actor actor = call.getArgument(1);
            return new StoredFileInfo("new-file", actor.accountId(), ArtifactPurpose.MATERIAL,
                    "new.pdf", "application/pdf", 100L, "sha");
        });
        int expected = endpoint.allowed().contains(role) ? endpoint.success() : 404;
        var before = jdbc.queryForList("select * from materials order by id");
        mvc.perform(signedRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(endpoint.method()), routePath(endpoint.path()))
                .content(routeBody(endpoint.body())), role)).andExpect(status().is(expected));
        if (!endpoint.allowed().contains(role)) {
            assertThat(jdbc.queryForList("select * from materials order by id")).isEqualTo(before);
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ACCESS_DENIED'", Integer.class)).isEqualTo(1);
        }
    }

    @Test
    void currentDatabaseRoleAndAccountStatusOverrideSignedJwtClaims() throws Exception {
        var request = signedRequest(get("/api/v1/admin/subjects"), Role.ADMIN);
        jdbc.update("update accounts set role='STUDENT' where id=?", accountIds.get(Role.ADMIN));
        mvc.perform(request).andExpect(status().isForbidden());
        jdbc.update("update accounts set status='DISABLED' where id=?", accountIds.get(Role.ADMIN));
        mvc.perform(signedRequest(get("/api/v1/me/notifications"), Role.ADMIN)).andExpect(status().isUnauthorized());
    }
}
