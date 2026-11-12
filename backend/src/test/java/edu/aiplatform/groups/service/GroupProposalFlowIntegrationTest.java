package edu.aiplatform.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.service.GroupDocumentService;
import edu.aiplatform.groupdocs.service.IGroupDocStreams;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.service.ClassGroupService.SaveCommand;
import edu.aiplatform.groups.service.GroupFormationService.UpdateFormationCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveResult;
import edu.aiplatform.groups.service.GroupProposalService.CreateProposalCommand;
import edu.aiplatform.groups.service.GroupProposalService.InviteMembersCommand;
import edu.aiplatform.groups.service.GroupProposalService.MyProposalView;
import edu.aiplatform.groups.service.GroupProposalService.VersionCommand;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Luồng tự chia nhóm với repository và SQL thật trên PostgreSQL 16 (role {@code app}, migration Flyway thật): bật đợt
 * → tạo → mời → chấp nhận → gửi → duyệt thành nhóm chính thức; lưu bộ nhóm dọn đề xuất; hai lần chấp nhận đồng thời.
 * Quyền, ghi danh và sự kiện là mock; database dùng riêng cho test và bị hủy sau test.
 */
@Testcontainers
class GroupProposalFlowIntegrationTest {

    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource ownerDataSource;
    private static JdbcClient owner;
    private static JdbcClient app;

    private UUID classId;
    private Actor teacher;
    private List<Actor> students;
    private GroupRepository groupRepository;
    private GroupProposalRepository proposalRepository;
    private ClassGroupService classGroupService;
    private GroupFormationService formationService;
    private GroupProposalService proposalService;
    private GroupProposalReviewService reviewService;

    @BeforeAll
    static void setUpDatabase() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        ownerDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        owner = JdbcClient.create(ownerDataSource);
        app = JdbcClient.create(appDataSource);
    }

    @AfterAll
    static void close() {
        if (appDataSource != null) {
            appDataSource.close();
        }
        if (ownerDataSource != null) {
            ownerDataSource.close();
        }
    }

    @BeforeEach
    void seed() {
        owner.sql("truncate accounts, subjects cascade").update();
        UUID subject = UUID.randomUUID();
        classId = UUID.randomUUID();
        teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        students = IntStream.range(0, 6).mapToObj(i -> new Actor(UUID.randomUUID(), Role.STUDENT)).toList();
        insertAccount(teacher.accountId(), "TEACHER");
        students.forEach(s -> insertAccount(s.accountId(), "STUDENT"));
        owner.sql("insert into subjects (id, code, name, created_at, updated_at) values (?, ?, 'Môn', now(), now())")
                .params(subject, "SWP391").update();
        owner.sql("insert into course_classes (id, subject_id, code, name, status, teacher_id, created_at, updated_at) "
                + "values (?, ?, 'SE1', 'SE1', 'OPEN', ?, now(), now())")
                .params(classId, subject, teacher.accountId()).update();

        IAuthorization authorization = mock(IAuthorization.class);
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        IClassAccess classes = mock(IClassAccess.class);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId, subject, "SE1", "SE1",
                ClassStatus.OPEN, teacher.accountId(), false)));
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.listActiveStudents(classId)).thenReturn(students.stream().map(Actor::accountId).toList());
        IAudit audit = mock(IAudit.class);
        IEventPublisher events = mock(IEventPublisher.class);
        IAccountLookup accounts = mock(IAccountLookup.class);
        Clock clock = Clock.systemUTC();
        GroupAccess access = new GroupAccess(authorization, classes, audit, clock);

        groupRepository = new GroupRepository(app);
        proposalRepository = new GroupProposalRepository(app);
        GroupProposalCleanup cleanup = new GroupProposalCleanup(proposalRepository);
        classGroupService = new ClassGroupService(groupRepository,
                new GroupDocumentRepository(app, JsonMapper.builder().findAndAddModules().build()),
                mock(GroupDocumentService.class), mock(IGroupDocStreams.class), access, classes, accounts, audit,
                events, clock, cleanup);
        formationService = new GroupFormationService(proposalRepository, access, audit, clock);
        proposalService = new GroupProposalService(proposalRepository, groupRepository, formationService, cleanup,
                access, classes, accounts, audit, events, clock);
        reviewService = new GroupProposalReviewService(proposalRepository, groupRepository, classGroupService,
                formationService, cleanup, access, classes, accounts, audit, events, clock);
        formationService.updateFormation(teacher, classId, new UpdateFormationCommand(true, 2, 3, null, 0L));
    }

    @Test
    void submittedProposalIsApprovedIntoAnOfficialGroup() {
        Actor leader = students.get(0);
        MyProposalView created = proposalService.createProposal(leader, classId, new CreateProposalCommand("Nhóm Mây"));
        UUID proposalId = created.proposal().id();
        MyProposalView invited = proposalService.inviteMembers(leader, proposalId,
                new InviteMembersCommand(List.of(student(1), student(2)), created.proposal().version()));
        assertThat(invited.proposal().invitations()).hasSize(2);
        assertThat(proposalService.loadMyProposal(students.get(1), classId).invitations()).singleElement()
                .satisfies(i -> assertThat(i.canAccept()).isTrue());

        proposalService.acceptInvitation(students.get(1), proposalId);
        proposalService.declineInvitation(students.get(2), proposalId);
        long version = proposalService.loadMyProposal(leader, classId).proposal().version();
        MyProposalView submitted = proposalService.submitProposal(leader, proposalId, new VersionCommand(version));
        assertThat(submitted.proposal().status()).isEqualTo("SUBMITTED");
        assertThat(reviewService.listClassProposals(teacher, classId).proposals()).singleElement()
                .satisfies(p -> assertThat(p.members()).hasSize(2));

        long setVersion = groupRepository.setVersion(classId);
        ApproveResult result = reviewService.approveProposals(teacher, classId,
                new ApproveCommand(List.of(proposalId)));

        assertThat(result.failed()).isEmpty();
        assertThat(result.groupSetVersion()).isEqualTo(setVersion + 1);
        ClassGroup group = groupRepository.findGroup(result.approved().get(0).groupId()).orElseThrow();
        assertThat(group.name()).isEqualTo("Nhóm Mây");
        assertThat(group.leaderId()).isEqualTo(student(0));
        assertThat(group.memberIds()).containsExactlyInAnyOrder(student(0), student(1));
        assertThat(proposalService.loadMyProposal(leader, classId).grouped()).isTrue();
        assertThat(proposalRepository.findActiveProposalOf(classId, student(1))).isEmpty();
        assertThat(app.sql("select status from group_proposals where id = ?").param(proposalId)
                .query(String.class).single()).isEqualTo("APPROVED");
    }

    @Test
    void batchApprovalSkipsTheIncompleteProposalAndManualSaveCleansProposals() {
        UUID first = submittedProposal(students.get(0), "Nhóm Mây", students.get(1));
        UUID second = submittedProposal(students.get(2), "Nhóm Sao", students.get(3));

        // Giảng viên xếp tay sinh viên 3 vào nhóm chính thức: đề xuất thứ hai còn 1 người, về nháp.
        long setVersion = groupRepository.setVersion(classId);
        classGroupService.save(teacher, classId, new SaveCommand(setVersion, List.of(
                new Draft("manual", null, "Nhóm Tay", student(3), List.of(student(3), student(4))))));
        assertThat(app.sql("select status from group_proposals where id = ?").param(second)
                .query(String.class).single()).isEqualTo("DRAFT");
        assertThat(app.sql("select status from group_proposal_members where proposal_id = ? and account_id = ?")
                .params(second, student(3)).query(String.class).single()).isEqualTo("LEFT");

        ApproveResult result = reviewService.approveProposals(teacher, classId,
                new ApproveCommand(List.of(first, second)));

        assertThat(result.approved()).extracting(GroupProposalReviewService.Approved::proposalId)
                .containsExactly(first);
        assertThat(result.failed()).singleElement().satisfies(f -> {
            assertThat(f.proposalId()).isEqualTo(second);
            assertThat(f.code()).isEqualTo("PROPOSAL_NOT_SUBMITTED");
        });
        assertThat(groupRepository.listGroups(classId)).extracting(ClassGroup::name)
                .containsExactlyInAnyOrder("Nhóm Tay", "Nhóm Mây");
    }

    @Test
    void twoSimultaneousAcceptsLeaveTheStudentInExactlyOneProposal() throws Exception {
        UUID first = draftProposal(students.get(0), "Nhóm Mây");
        UUID second = draftProposal(students.get(1), "Nhóm Sao");
        invite(students.get(0), first, student(2));
        invite(students.get(1), second, student(2));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<String>> results = new ArrayList<>();
        for (UUID proposalId : List.of(first, second)) {
            Callable<String> accept = () -> {
                start.await();
                try {
                    proposalService.acceptInvitation(students.get(2), proposalId);
                    return "OK";
                } catch (ApiException e) {
                    return e.code();
                }
            };
            results.add(pool.submit(accept));
        }
        start.countDown();
        List<String> outcomes = new ArrayList<>();
        for (Future<String> result : results) {
            outcomes.add(result.get());
        }
        pool.shutdown();

        // Bên thua thấy đề xuất kia đã có mình, hoặc lời mời của mình đã bị thu hồi, tùy thời điểm chạy.
        assertThat(outcomes).containsOnlyOnce("OK");
        assertThat(outcomes).filteredOn(o -> !"OK".equals(o)).singleElement()
                .isIn("STUDENT_IN_PROPOSAL", "GROUP_PROPOSAL_NOT_FOUND");
        assertThat(app.sql("select count(*) from group_proposal_members where account_id = ? and active")
                .param(student(2)).query(Long.class).single()).isEqualTo(1);
    }

    private UUID submittedProposal(Actor leader, String name, Actor member) {
        UUID proposalId = draftProposal(leader, name);
        invite(leader, proposalId, member.accountId());
        proposalService.acceptInvitation(member, proposalId);
        long version = proposalService.loadMyProposal(leader, classId).proposal().version();
        proposalService.submitProposal(leader, proposalId, new VersionCommand(version));
        return proposalId;
    }

    private UUID draftProposal(Actor leader, String name) {
        return proposalService.createProposal(leader, classId, new CreateProposalCommand(name)).proposal().id();
    }

    private void invite(Actor leader, UUID proposalId, UUID invitee) {
        long version = proposalService.loadMyProposal(leader, classId).proposal().version();
        proposalService.inviteMembers(leader, proposalId, new InviteMembersCommand(List.of(invitee), version));
    }

    private UUID student(int index) {
        return students.get(index).accountId();
    }

    private void insertAccount(UUID id, String role) {
        owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                + "(?, ?, ?, 'ACTIVE', 'Người dùng', 'x')").params(id, id + "@fpt.edu.vn", role).update();
    }

    private static Transferable rolesScript() {
        try {
            String script = Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"));
            return Transferable.of(script.replace("\r", ""), 0755);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HikariDataSource dataSource(String user, String password) {
        HikariDataSource source = new HikariDataSource();
        source.setJdbcUrl(POSTGRES.getJdbcUrl());
        source.setUsername(user);
        source.setPassword(password);
        source.setMaximumPoolSize(4);
        return source;
    }
}
