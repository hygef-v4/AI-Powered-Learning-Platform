package edu.aiplatform.academics.service;


import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.academics.repository.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import static edu.aiplatform.academics.dto.AcademicsDtos.*;

@Service
public class AcademicsService {
    private final ICourseClassRepository courseClassRepository;
    private final IEnrollmentRepository enrollmentRepository;
    private final IAcademicsReadRepository academicsReadRepository;
    /** Khóa dòng môn của U04 tới cuối transaction: U04 lưu trữ môn và U05 ghi lớp, ghi danh trong cùng môn chạy lần lượt. */
    private final ISubjectMutationGuard subjectMutationGuard;
    private static final Duration SUBJECT_LOCK_WAIT = Duration.ofSeconds(1);
    /** Thao tác nhận id môn (không phải id lớp). */
    private static final Set<String> SUBJECT_ID_ACTIONS = Set.of("MANAGED_SUBJECT_VIEW", "CLASS_CREATED");
    private final IAccountLookup accountLookup;
    private final ISubjectQuery subjectQuery;
    private final ISubjectScope scope;
    private final IAuthorization authorization;
    private final IAudit audit;
    private final IEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate lineTransactionTemplate;
    private final Clock clock;

    // Thiết lập giao dịch cho lệnh lớp và giao dịch riêng cho từng dòng ghi danh.
    public AcademicsService(ICourseClassRepository courseClassRepository,
            IEnrollmentRepository enrollmentRepository,
            IAcademicsReadRepository academicsReadRepository,
            ISubjectMutationGuard subjectMutationGuard, IAccountLookup accountLookup,
            ISubjectQuery subjectQuery, ISubjectScope scope, IAuthorization authorization,
            IAudit audit, IEventPublisher eventPublisher, PlatformTransactionManager transactions,
            Clock clock) {
        this.courseClassRepository = courseClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicsReadRepository = academicsReadRepository;
        this.subjectMutationGuard = subjectMutationGuard;
        this.accountLookup = accountLookup;
        this.subjectQuery = subjectQuery;
        this.scope = scope;
        this.authorization = authorization;
        this.audit = audit;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactions);
        this.lineTransactionTemplate = new TransactionTemplate(transactions);
        this.lineTransactionTemplate
                .setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Lists course classes filtered by audience perspective (student, teacher, manager), query, and status.
     */
    public List<ClassView> listClasses(Actor actor, String audience, UUID subjectId, String query,
            ClassStatus status, String term, int page, int size) {
        return executeGuarded(actor, "CLASS_LIST", null, describeRequest(audience, subjectId), () -> {
            requireAudience(actor, audience);
            validatePage(page, size);
            validateQuery(query);
            String termFilter = ClassTermPolicy.parseFilter(term);
            List<CourseClass> values;
            int offset = (page - 1) * size;
            if (audience.equals("manager")) {
                var ids = scope.listManagedSubjects(actor.accountId());
                if (subjectId != null) {
                    requireManager(actor, subjectId);
                    ids = List.of(subjectId);
                }
                values = ids.isEmpty() ? List.of()
                        : academicsReadRepository.listForManager(ids, query, status, termFilter,
                                size, offset);
            } else if (audience.equals("teacher"))
                values = academicsReadRepository.listForTeacher(actor.accountId(), query, status,
                        termFilter, size, offset);
            else
                values = academicsReadRepository.listForStudent(actor.accountId(), query, status,
                        termFilter, size, offset);
            return mapClassViews(values);
        });
    }

    /**
     * Loads a single class details view after checking caller audience authorization.
     */
    public ClassView loadClass(Actor actor, String audience, UUID id) {
        return executeGuarded(actor, "CLASS_VIEW", id, describeRequest(audience, null), () -> {
            requireAudience(actor, audience);
            return mapClassView(requireClassAccess(actor, audience, id));
        });
    }

    /**
     * Lists paginated students enrolled in a class for teachers or subject managers.
     */
    public List<Student> listStudents(Actor actor, String audience, UUID id, int page, int size) {
        return executeGuarded(actor, "CLASS_STUDENTS_VIEW", id, describeRequest(audience, null), () -> {
            requireAudience(actor, audience);
            if (audience.equals("student"))
                throw ApiException.createForbidden("ROLE_DENIED");
            requireClassAccess(actor, audience, id);
            validatePage(page, size);
            return enrollmentRepository
                    .listByClass(id, !audience.equals("manager"), size, (page - 1) * size).stream()
                    .map(this::mapStudentView).toList();
        });
    }

    /**
     * Loads details of an active enrolled student within a class for the teacher.
     */
    public Student loadStudent(Actor actor, UUID id, UUID studentId) {
        return executeGuarded(actor, "CLASS_STUDENT_VIEW", id, () -> {
            requireAudience(actor, "teacher");
            requireClassAccess(actor, "teacher", id);
            Enrollment enrollment = enrollmentRepository.findEnrollment(id, studentId)
                    .filter(e -> e.status() == EnrollmentStatus.ACTIVE)
                    .orElseThrow(() -> ApiException.createNotFound("STUDENT_NOT_FOUND"));
            return mapStudentView(enrollment);
        });
    }

    /**
     * Lists all subjects managed by the authenticated subject manager.
     */
    public List<SubjectSummary> listManagedSubjects(Actor actor) {
        return executeGuarded(actor, "MANAGED_SUBJECTS_VIEW", null, () -> {
            requireAudience(actor, "manager");
            return subjectQuery.listManagedSubjects(actor.accountId()).stream()
                    .filter(s -> scope.isSubjectManager(actor.accountId(), s.subjectId()))
                    .map(this::mapSubjectSummary).toList();
        });
    }

    /**
     * Loads summary details of a specific subject assigned to the subject manager.
     */
    public SubjectSummary loadManagedSubject(Actor actor, UUID id) {
        return executeGuarded(actor, "MANAGED_SUBJECT_VIEW", id, () -> {
            requireAudience(actor, "manager");
            requireManager(actor, id);
            return mapSubjectSummary(loadSubject(id));
        });
    }

    /**
     * Searches eligible teacher accounts for class assignment.
     */
    public List<Person> listTeacherCandidates(Actor actor, String query) {
        return executeGuarded(actor, "TEACHER_CANDIDATES_VIEW", null, () -> {
            requireAudience(actor, "manager");
            validateQuery(query);
            return accountLookup.findAccounts(query, Set.of(Role.TEACHER, Role.SUBJECT_MANAGER), 20)
                    .stream().filter(a -> isEligibleTeacher(a)).limit(20).map(this::mapPerson)
                    .toList();
        });
    }

    /**
     * Searches active student accounts eligible for enrollment in a class.
     */
    public List<Person> listStudentCandidates(Actor actor, UUID id, String query) {
        return executeGuarded(actor, "STUDENT_CANDIDATES_VIEW", id, () -> {
            requireAudience(actor, "manager");
            requireClassAccess(actor, "manager", id);
            validateQuery(query);
            return accountLookup.findStudents(query, 200).stream()
                    .filter(a -> a.role() == Role.STUDENT && a.status() != AccountStatus.DISABLED)
                    .limit(200).map(this::mapPerson).toList();
        });
    }

    /**
     * Creates a new class within a subject managed by the caller.
     */
    public ClassView createClass(Actor actor, UUID subjectId, String code, String name,
            String description, LocalDate startsOn, UUID teacherId) {
        return executeCommand(actor, "CLASS_CREATED", subjectId, () -> {
            requireAudience(actor, "manager");
            requireManager(actor, subjectId);
            ClassTermPolicy.requireValidStart(startsOn);
            // Khóa tài khoản giảng viên trước dòng môn: U01 vô hiệu hóa tài khoản khóa tài khoản rồi mới tới môn.
            requireTeacher(teacherId);
            subjectMutationGuard.lockActive(subjectId, SUBJECT_LOCK_WAIT);
            requireManager(actor, subjectId);
            CourseClass value = CourseClass.createClass(UUID.randomUUID(), subjectId, code, name,
                    description, startsOn, teacherId, clock.instant());
            courseClassRepository.insert(value);
            recordClassEvent(actor, value, "CREATED", Map.of());
            return mapClassView(value);
        });
    }

    /**
     * Updates the class name, description and start date after checking the expected version.
     */
    public ClassView updateClassInfo(Actor actor, UUID id, String name, String description,
            LocalDate startsOn, long version) {
        return executeCommand(actor, "CLASS_UPDATED", id, () -> {
            CourseClass current = loadLockedClass(actor, id, version);
            CourseClass changed = current.updateInfo(name, description, startsOn,
                    clock.instant());
            courseClassRepository.updateClass(changed, version);
            return mapClassView(changed);
        });
    }

    /**
     * Assigns or updates the assigned teacher for a class.
     */
    public ClassView assignClassTeacher(Actor actor, UUID id, UUID teacherId, long version) {
        return executeCommand(actor, "CLASS_TEACHER_CHANGED", id, () -> {
            // Kiểm quyền trên lớp trước (giữ 403/404), rồi khóa tài khoản giảng viên trước dòng môn trong locked():
            // U01 vô hiệu hóa tài khoản khóa tài khoản rồi mới tới môn.
            requireAudience(actor, "manager");
            requireManager(actor, requireClassAccess(actor, "manager", id).subjectId());
            requireTeacher(teacherId);
            CourseClass current = loadLockedClass(actor, id, version);
            CourseClass changed = current.assignTeacher(teacherId, clock.instant());
            courseClassRepository.updateClass(changed, version);
            return mapClassView(changed);
        });
    }

    /**
     * Transitions class status (OPEN, ARCHIVED) while validating conflicts and notifying enrolled students.
     */
    public ClassView changeClassStatus(Actor actor, UUID id, ClassStatus status, long version) {
        return executeCommand(actor, "CLASS_STATUS_CHANGED", id, () -> {
            if (status == null)
                throw ApiException.createBadRequest("INVALID_STATUS", "Chọn trạng thái lớp");
            CourseClass current = loadLockedClass(actor, id, version);
            boolean subjectActive = false;
            if (status == ClassStatus.OPEN) {
                requireTeacherStillEligible(current.teacherId());
                subjectActive = subjectQuery.isActive(current.subjectId());
                var conflicting = enrollmentRepository.listConflictingStudents(id, current.subjectId());
                if (!conflicting.isEmpty()) {
                    var conflictingNames = conflicting.stream()
                            .map(studentId -> accountLookup.getContact(studentId)
                                     .map(AccountContact::displayName)
                                    .filter(name -> !name.isBlank())
                                    .orElse("Sinh viên không còn thông tin tài khoản"))
                            .toList();
                    throw new AcademicsProblem("ENROLLMENT_CONFLICT",
                            "Có sinh viên đang ở lớp khác cùng môn",
                            Map.of("conflictingStudents", conflictingNames));
                }
            }
            CourseClass changed = current.changeStatus(status, subjectActive, false,
                    clock.instant());
            courseClassRepository.updateClass(changed, version);
            String statusEvent = resolveStatusEvent(current.status(), status);
            if (statusEvent != null)
                recordClassEvent(actor, changed, statusEvent,
                        Map.of("before", current.status().name(), "after", status.name()));
            // Mở lớp lần đầu hay mở lại lớp đã lưu trữ: sinh viên đang ghi danh đều được báo lớp đã mở.
            if (status == ClassStatus.OPEN) {
                for (int offset = 0;; offset += 100) {
                    var page = enrollmentRepository.listByClass(id, true, 100, offset);
                    page.forEach(e -> publishEnrollmentActivation(changed, e.accountId()));
                    if (page.size() < 100)
                        break;
                }
            }
            return mapClassView(changed);
        });
    }

    /**
     * Enrolls an individual student into a class.
     */
    public EnrollmentResult enrollStudent(Actor actor, UUID id, UUID accountId) {
        return executeCommand(actor, "STUDENT_ENROLLED", id, () -> {
            requireAudience(actor, "manager");
            requireEditableClass(requireClassAccess(actor, "manager", id));
            AccountContact account = accountLookup.lockContact(Objects.requireNonNull(accountId))
                    .orElse(null);
            CourseClass current = loadLockedClass(actor, id, null);
            requireEditableClass(current);
            return enrollLine(actor, current, account, EnrollmentSource.MANUAL);
        });
    }

    /**
     * Batch enrolls multiple students from raw text / email list with per-row transactional isolation.
     */
    public List<RowResult> enrollStudents(Actor actor, UUID id, String text) {
        return executeGuarded(actor, "STUDENT_ENROLLED", id, () -> {
            requireAudience(actor, "manager");
            requireEditableClass(requireClassAccess(actor, "manager", id));
            var rows = EnrollmentListParser.parseEnrollmentRows(text);
            var emails = rows.stream().filter(r -> r.error() == null && r.email() != null)
                    .map(EnrollmentListParser.Row::email).toList();
            var studentCodes = rows.stream().filter(r -> r.error() == null && r.studentCode() != null)
                    .map(EnrollmentListParser.Row::studentCode).toList();
            Map<String, AccountContact> found = accountLookup.findByEmails(emails).stream()
                    .collect(Collectors.toMap(a -> a.email().toLowerCase(Locale.ROOT),
                            Function.identity(), (a, b) -> a));
            // BR-U05-57: MSSV chỉ tìm thấy tài khoản STUDENT; không thấy thì NOT_FOUND như email.
            Map<String, AccountContact> foundByCode = studentCodes.isEmpty() ? Map.of()
                    : accountLookup.findByStudentCodes(studentCodes).stream()
                            .filter(a -> a.studentCode() != null)
                            .collect(Collectors.toMap(AccountContact::studentCode,
                                    Function.identity(), (a, b) -> a));
            var results = new ArrayList<RowResult>();
            // Mỗi dòng hợp lệ có giao dịch riêng, nên lỗi ở dòng sau không hoàn tác các dòng đã ghi.
            for (var row : rows) {
                EnrollmentResult result = row.error();
                if (result == null)
                    result = lineTransactionTemplate.execute(tx -> {
                        AccountContact resolved = row.email() != null ? found.get(row.email())
                                : foundByCode.get(row.studentCode());
                        // Khóa tài khoản trước môn như U01: mỗi dòng dùng role/trạng thái hiện tại tới lúc commit.
                        AccountContact account = resolved == null ? null
                                : accountLookup.lockContact(resolved.accountId()).orElse(null);
                        CourseClass current = loadLockedClass(actor, id, null);
                        requireEditableClass(current);
                        return enrollLine(actor, current, account, EnrollmentSource.LIST);
                    });
                results.add(new RowResult(row.lineNumber(), row.readIdentifier(), result));
            }
            return List.copyOf(results);
        });
    }

    /**
     * Modifies student enrollment status (e.g. restoring or removing/dropping a student).
     */
    public EnrollmentResult changeEnrollmentStatus(Actor actor, UUID id, UUID accountId,
            EnrollmentStatus status) {
        if (status == EnrollmentStatus.ACTIVE)
            return executeCommand(actor, "STUDENT_ENROLLED", id, () -> {
                requireAudience(actor, "manager");
                requireEditableClass(requireClassAccess(actor, "manager", id));
                AccountContact account = accountLookup.lockContact(accountId).orElse(null);
                CourseClass current = loadLockedClass(actor, id, null);
                requireEditableClass(current);
                EnrollmentResult result = enrollLine(actor, current, account,
                        EnrollmentSource.MANUAL);
                if (result == EnrollmentResult.IN_OTHER_CLASS)
                    throw ApiException.createConflict("IN_OTHER_CLASS",
                            "Sinh viên đang ở lớp khác cùng môn");
                if (result != EnrollmentResult.ENROLLED && result != EnrollmentResult.RESTORED
                        && result != EnrollmentResult.ALREADY_ENROLLED)
                    throw ApiException.createBadRequest(result.name(),
                            "Không thể khôi phục ghi danh. Kiểm tra tài khoản sinh viên");
                return result;
            });
        return executeCommand(actor, "STUDENT_REMOVED", id, () -> {
            if (status == null)
                throw ApiException.createBadRequest("INVALID_STATUS", "Chọn trạng thái ghi danh");
            CourseClass current = loadLockedClass(actor, id, null);
            requireEditableClass(current);
            Enrollment old = enrollmentRepository.findEnrollment(id, accountId)
                    .orElseThrow(() -> ApiException.createNotFound("ENROLLMENT_NOT_FOUND"));
            if (old.status() == EnrollmentStatus.ACTIVE) {
                enrollmentRepository.updateEnrollment(old.removeEnrollment(clock.instant()),
                        EnrollmentStatus.ACTIVE);
            }
            return EnrollmentResult.ALREADY_ENROLLED;
        });
    }

    // Kiểm tư cách sinh viên và ghi mới hoặc khôi phục ghi danh trong giao dịch của caller.
    private EnrollmentResult enrollLine(Actor actor, CourseClass current, AccountContact account,
            EnrollmentSource source) {
        if (account == null)
            return EnrollmentResult.NOT_FOUND;
        if (account.role() != Role.STUDENT)
            return EnrollmentResult.NOT_STUDENT;
        if (account.status() == AccountStatus.DISABLED)
            return EnrollmentResult.DISABLED;
        var existing = enrollmentRepository.findEnrollment(current.id(), account.accountId()).orElse(null);
        var result = EnrollmentPolicy.resolveEnrollmentResult(current.status(), existing,
                enrollmentRepository.hasActiveEnrollmentElsewhere(account.accountId(), current.subjectId(),
                        current.id()));
        if (result == EnrollmentResult.ENROLLED || result == EnrollmentResult.RESTORED) {
            Enrollment value = existing == null
                    ? Enrollment.createEnrollment(current.id(), account.accountId(), source,
                            clock.instant())
                    : existing.restoreEnrollment(source, clock.instant());
            if (existing == null)
                enrollmentRepository.insert(value);
            else
                enrollmentRepository.updateEnrollment(value, EnrollmentStatus.REMOVED);
            if (current.status() == ClassStatus.OPEN)
                publishEnrollmentActivation(current, account.accountId());
        }
        return result;
    }

    // Khóa môn trước lớp, kiểm lại quyền và so version nếu caller gửi version.
    private CourseClass loadLockedClass(Actor actor, UUID id, Long version) {
        requireAudience(actor, "manager");
        CourseClass before = requireClassAccess(actor, "manager", id);
        subjectMutationGuard.lockActive(before.subjectId(), SUBJECT_LOCK_WAIT);
        CourseClass current = requireClassAccess(actor, "manager", id);
        requireManager(actor, current.subjectId());
        if (version != null && (version < 0 || current.version() != version))
            throw ApiException.createConflict("CLASS_VERSION_CONFLICT",
                    "Dữ liệu lớp đã thay đổi. Hãy tải lại");
        return current;
    }

    // Áp quy tắc quyền theo vai trò xem và trả lớp nếu người gọi có phạm vi tương ứng.
    private CourseClass requireClassAccess(Actor actor, String audience, UUID id) {
        CourseClass value = courseClassRepository.findClass(id)
                .orElseThrow(() -> ApiException.createNotFound("CLASS_NOT_FOUND"));
        boolean allowed = switch (audience) {
        case "manager" -> scope.isSubjectManager(actor.accountId(), value.subjectId());
        case "teacher" -> actor.accountId().equals(value.teacherId());
        case "student" ->
            value.status() == ClassStatus.OPEN && enrollmentRepository.findEnrollment(id, actor.accountId())
                    .map(e -> e.status() == EnrollmentStatus.ACTIVE).orElse(false);
        default -> false;
        };
        if (!allowed)
            throw ApiException.createNotFound("CLASS_OUT_OF_SCOPE");
        // Chủ nhiệm xem lớp của môn mình theo phạm vi môn (R2), không cần được giao dạy lớp đó.
        if (audience.equals("manager"))
            requireAuthorization(actor, Actions.CLASS_MANAGE,
                    ResourceRef.subject(value.subjectId()), true);
        else
            requireAuthorization(actor, Actions.CLASS_VIEW, ResourceRef.courseClass(id), true);
        return value;
    }

    // Đối chiếu quyền quản lý môn hiện tại từ cả scope và hồ sơ môn.
    private void requireManager(Actor actor, UUID subjectId) {
        if (subjectId == null || !scope.isSubjectManager(actor.accountId(), subjectId)
                || !actor.accountId().equals(loadSubject(subjectId).managerId()))
            throw ApiException.createNotFound("SUBJECT_OUT_OF_SCOPE");
        requireAuthorization(actor, Actions.CLASS_MANAGE, ResourceRef.subject(subjectId), true);
    }

    // Phân biệt yêu cầu thiếu đăng nhập với tài khoản đăng nhập sai vai trò của route.
    private void requireAudience(Actor actor, String audience) {
        if (actor == null || actor.accountId() == null || actor.role() == null)
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
        boolean roleAllowed = switch (audience) {
        case "manager" -> actor.role() == Role.SUBJECT_MANAGER;
        case "teacher" -> actor.role() == Role.TEACHER || actor.role() == Role.SUBJECT_MANAGER;
        case "student" -> actor.role() == Role.STUDENT;
        default -> false;
        };
        if (!roleAllowed)
            throw ApiException.createForbidden("ROLE_DENIED");
        AccountContact contact = accountLookup.getContact(actor.accountId())
                .orElseThrow(() -> ApiException.createForbidden("ACCOUNT_INACTIVE"));
        if (contact.status() != AccountStatus.ACTIVE || contact.role() != actor.role())
            throw ApiException.createForbidden("ACCOUNT_INACTIVE");
        String action = switch (audience) {
        case "manager" -> Actions.CLASS_MANAGER;
        case "teacher" -> Actions.CLASS_TEACHER;
        default -> Actions.CLASS_STUDENT;
        };
        requireAuthorization(actor, action, ResourceRef.none(), false);
    }

    // Áp kết quả phân quyền và trả 404 nếu từ chối trên tài nguyên cần che.
    private void requireAuthorization(Actor actor, String action, ResourceRef ref,
            boolean hideDeniedResource) {
        var decision = authorization.authorize(actor, action, ref);
        if (decision == null || !decision.allowed()) {
            if (hideDeniedResource)
                throw ApiException.createNotFound("CLASS_OUT_OF_SCOPE");
            throw ApiException.createForbidden("ACCESS_DENIED");
        }
    }

    /** Gán giảng viên: khóa tài khoản tới hết transaction để U01 không đổi role xen vào (BR-U01-64). */
    private void requireTeacher(UUID id) {
        if (id == null)
            return;
        var contact = accountLookup.lockContact(id).orElseThrow(
                () -> ApiException.createBadRequest("INVALID_TEACHER", "Không tìm thấy giảng viên"));
        if (!isEligibleTeacher(contact))
            throw ApiException.createBadRequest("INVALID_TEACHER", "Chọn giảng viên đang hoạt động");
    }

    /** Mở lại lớp với giảng viên đã gán: không tạo phân công mới nên không cần khóa tài khoản. */
    private void requireTeacherStillEligible(UUID id) {
        if (id == null)
            return;
        var contact = accountLookup.getContact(id).orElseThrow(
                () -> ApiException.createBadRequest("INVALID_TEACHER", "Không tìm thấy giảng viên"));
        if (!isEligibleTeacher(contact))
            throw ApiException.createBadRequest("INVALID_TEACHER", "Chọn giảng viên đang hoạt động");
    }

    // Chỉ coi tài khoản ACTIVE có vai trò giảng dạy phù hợp là người được phân công.
    private boolean isEligibleTeacher(AccountContact contact) {
        return contact.status() == AccountStatus.ACTIVE
                && (contact.role() == Role.TEACHER || contact.role() == Role.SUBJECT_MANAGER);
    }

    // Chặn ghi danh và chỉnh sửa trên lớp đã lưu trữ.
    private void requireEditableClass(CourseClass value) {
        if (value.status() == ClassStatus.ARCHIVED)
            throw ApiException.createConflict("CLASS_ARCHIVED", "Lớp đã lưu trữ chỉ được xem");
    }

    // Đọc tham chiếu môn và báo nguồn tạm không khả dụng nếu thiếu dữ liệu.
    private SubjectRef loadSubject(UUID id) {
        return subjectQuery.getSubject(id).orElseThrow(
                () -> ApiException.createUnavailable("SUBJECT_UNAVAILABLE", "Không đọc được môn học"));
    }

    // Gom số lượt ghi danh theo ID lớp trong một truy vấn trước khi dựng danh sách trả về.
    private List<ClassView> mapClassViews(List<CourseClass> values) {
        var counts = academicsReadRepository
                .countEnrollments(values.stream().map(CourseClass::id).toList());
        return values
                .stream().map(
                        courseClass -> mapClassView(courseClass,
                                counts.getOrDefault(courseClass.id(),
                                        new IAcademicsReadRepository.EnrollmentCounts(0, 0))))
                .toList();
    }

    // Dựng phản hồi cho một lớp qua cùng luồng đếm ghi danh của danh sách.
    private ClassView mapClassView(CourseClass value) {
        return mapClassViews(List.of(value)).get(0);
    }

    // Ghép môn, giảng viên và các số đếm đã đọc vào phản hồi của lớp.
    private ClassView mapClassView(CourseClass courseClass,
            IAcademicsReadRepository.EnrollmentCounts counts) {
        var subject = loadSubject(courseClass.subjectId());
        Person teacher = courseClass.teacherId() == null ? null
                : mapPerson(accountLookup.getContact(courseClass.teacherId())
                        .orElseThrow(() -> ApiException.createUnavailable("ACCOUNT_UNAVAILABLE",
                                "Không đọc được giảng viên")));
        return new ClassView(courseClass.id(),
                new Subject(subject.subjectId(), subject.code(), subject.name()),
                courseClass.code(), courseClass.name(),
                Objects.requireNonNullElse(courseClass.description(), ""), courseClass.startsOn(),
                courseClass.deriveTerm(), courseClass.status(), teacher,
                counts.active(), counts.removed(), courseClass.createdAt(),
                courseClass.showGradeDistribution(), courseClass.version());
    }

    // Tổng hợp số lớp theo từng trạng thái để hiển thị tổng quan môn.
    private SubjectSummary mapSubjectSummary(SubjectRef subject) {
        var counts = new EnumMap<ClassStatus, Long>(ClassStatus.class);
        for (var status : ClassStatus.values())
            counts.put(status, 0L);
        academicsReadRepository.listOfSubject(subject.subjectId())
                .forEach(courseClass -> counts.merge(courseClass.status(), 1L, Long::sum));
        return new SubjectSummary(subject.subjectId(), subject.code(), subject.name(),
                subject.description(), subject.status(), Map.copyOf(counts));
    }

    // Ghép trạng thái ghi danh với hồ sơ tài khoản hiện tại của sinh viên.
    private Student mapStudentView(Enrollment enrollment) {
        var account = accountLookup.getContact(enrollment.accountId()).orElseThrow(
                () -> ApiException.createUnavailable("ACCOUNT_UNAVAILABLE", "Không đọc được sinh viên"));
        return new Student(enrollment.accountId(), account.displayName(), account.email(),
                enrollment.enrolledAt(), enrollment.status(), account.studentCode());
    }

    // Chuyển hồ sơ liên lạc sang tên, email và mã sinh viên dùng trong phản hồi.
    private Person mapPerson(AccountContact account) {
        return new Person(account.accountId(), account.displayName(), account.email(),
                account.studentCode());
    }

    // Phát sự kiện sinh viên đã được kích hoạt ghi danh để các unit khác xử lý.
    private void publishEnrollmentActivation(CourseClass courseClass, UUID accountId) {
        eventPublisher
                .publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.ENROLLMENT_ACTIVATED,
                        "U05", clock.instant(), UUID.randomUUID().toString(),
                        Map.of("classId", courseClass.id().toString(), "subjectId",
                                courseClass.subjectId().toString(), "accountId",
                                accountId.toString())));
    }

    /**
     * Lớp chỉ ghi tạo, mở, lưu trữ, mở lại (yêu cầu 2026-11-26); mã hành động kèm mã lớp như
     * {@code CLASS_SE1801_CREATED}. Sửa thông tin, đổi giảng viên, ghi danh không ghi.
     */
    private void recordClassEvent(Actor actor, CourseClass courseClass, String event,
            Map<String, Object> details) {
        recordAudit(actor, AuditActions.buildObjectAction("CLASS", courseClass.code(), event),
                "COURSE_CLASS", courseClass.id(), AuditResult.SUCCESS, null, details);
    }

    /** DRAFT → OPEN là mở lớp, ARCHIVED → OPEN là mở lại, sang ARCHIVED là lưu trữ; chuyển khác không ghi. */
    static String resolveStatusEvent(ClassStatus before, ClassStatus after) {
        if (after == ClassStatus.ARCHIVED && before != ClassStatus.ARCHIVED)
            return "ARCHIVED";
        if (after == ClassStatus.OPEN && before == ClassStatus.DRAFT)
            return "OPENED";
        if (after == ClassStatus.OPEN && before == ClassStatus.ARCHIVED)
            return "REOPENED";
        return null;
    }

    // Ghi sự kiện audit kể cả khi yêu cầu chưa xác định được tài khoản hoặc tài nguyên.
    private void recordAudit(Actor actor, String action, String objectType, UUID id,
            AuditResult result, String reason, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(),
                action, objectType, id == null ? null : id.toString(), result, reason, details,
                clock.instant()));
    }

    // Chạy lệnh ghi trong giao dịch rồi dùng chung cơ chế chuyển lỗi và audit của unit.
    private <T> T executeCommand(Actor actor, String action, UUID id, Supplier<T> work) {
        return executeGuarded(actor, action, id,
                () -> transactionTemplate.execute(tx -> work.get()));
    }

    // Chạy thao tác qua xử lý lỗi và audit với bộ chi tiết yêu cầu mặc định.
    private <T> T executeGuarded(Actor actor, String action, UUID id, Supplier<T> work) {
        return executeGuarded(actor, action, id, Map.of(), work);
    }

    /**
     * Chỉ lần bị từ chối (401, 403, 404) được audit, kèm thao tác bị chặn, vai trò người gọi và {@code request} (góc
     * nhìn, môn đang lọc) để Admin biết ai bị chặn ở đâu. Lỗi nghiệp vụ khác không ghi (yêu cầu 2026-11-26). Id là môn
     * thì ghi loại {@code SUBJECT} để tra được tên môn.
     */
    private <T> T executeGuarded(Actor actor, String action, UUID id, Map<String, Object> request,
            Supplier<T> work) {
        try {
            return work.get();
        } catch (RuntimeException failure) {
            ApiException problem = mapFailure(failure);
            boolean denied = problem.readStatus().value() == 401 || problem.readStatus().value() == 403
                    || problem.readStatus().value() == 404;
            if (!denied)
                throw problem;
            try {
                recordAudit(actor, "ACCESS_DENIED",
                        SUBJECT_ID_ACTIONS.contains(action) ? "SUBJECT" : "COURSE_CLASS", id,
                        AuditResult.DENIED, problem.readCode(), describeDenial(actor, action, request));
            } catch (RuntimeException auditFailure) {
                throw ApiException.createUnavailable("AUDIT_UNAVAILABLE",
                        "Không thể ghi nhật ký an toàn");
            }
            throw problem;
        }
    }

    // Ghép chi tiết yêu cầu với lý do từ chối để audit truy được phạm vi.
    private static Map<String, Object> describeDenial(Actor actor, String action,
            Map<String, Object> request) {
        Map<String, Object> details = new LinkedHashMap<>(request);
        details.putAll(AuditDetails.describeDenial(actor, action));
        return details;
    }

    // Đưa vai trò xem và ID môn vào audit nếu hai giá trị đã được xác định.
    private static Map<String, Object> describeRequest(String audience, UUID subjectId) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (audience != null)
            request.put("audience", audience);
        if (subjectId != null)
            request.put("subjectId", subjectId.toString());
        return request;
    }

    // Quy đổi lỗi nghiệp vụ của lớp sang mã HTTP phù hợp với caller.
    private ApiException mapFailure(RuntimeException failure) {
        if (failure instanceof ApiException problem)
            return problem;
        if (failure instanceof AcademicRuleException rule) {
            if (Set.of("CLASS_EXISTS", "CLASS_VERSION_CONFLICT", "ENROLLMENT_EXISTS",
                    "ENROLLMENT_CONFLICT", "CLASS_ARCHIVED", "INVALID_CLASS_TRANSITION",
                    "TEACHER_REQUIRED", "SUBJECT_INACTIVE").contains(rule.readCode()))
                return ApiException.createConflict(rule.readCode(), rule.getMessage());
            if (rule.readCode().equals("LIST_TOO_LARGE"))
                return ApiException.createPayloadTooLarge(rule.readCode(), rule.getMessage());
            return ApiException.createBadRequest(rule.readCode(), rule.getMessage());
        }
        if (failure instanceof OptimisticLockingFailureException)
            return ApiException.createConflict("CLASS_VERSION_CONFLICT",
                    "Dữ liệu đã thay đổi. Hãy tải lại");
        return ApiException.createUnavailable("ACADEMICS_DEPENDENCY_UNAVAILABLE",
                "Không thể hoàn thành yêu cầu. Hãy thử lại sau");
    }

    // Chặn trang sai giới hạn và offset vượt miền số nguyên trước khi truy vấn.
    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE)
            throw ApiException.createBadRequest("INVALID_PAGE",
                    "Trang phải từ 1, kích thước từ 1 đến 100");
    }

    // Giới hạn độ dài từ khóa trước khi chuyển xuống truy vấn.
    private void validateQuery(String query) {
        if (query != null && query.length() > 200)
            throw ApiException.createBadRequest("INVALID_QUERY", "Từ khóa tối đa 200 ký tự");
    }
}
