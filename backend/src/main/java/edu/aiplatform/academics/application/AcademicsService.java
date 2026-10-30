package edu.aiplatform.academics.application;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.*;
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
import static edu.aiplatform.academics.application.AcademicsDtos.*;

@Service
public class AcademicsService {
    private final ICourseClassRepository classes;
    private final IEnrollmentRepository enrollments;
    private final IAcademicsReadRepository reads;
    /** Khóa dòng môn của U04 tới cuối transaction: U04 lưu trữ môn và U05 ghi lớp, ghi danh trong cùng môn chạy lần lượt. */
    private final ISubjectMutationGuard subjectGuard;
    private static final Duration SUBJECT_LOCK_WAIT=Duration.ofSeconds(1);
    private final IAccountLookup accounts;
    private final ISubjectQuery subjects;
    private final ISubjectScope scope;
    private final IAuthorization authorization;
    private final IAudit audit;
    private final IEventPublisher events;
    private final TransactionTemplate transaction;
    private final TransactionTemplate lineTransaction;
    private final Clock clock;

    public AcademicsService(ICourseClassRepository classes, IEnrollmentRepository enrollments,
                            IAcademicsReadRepository reads, ISubjectMutationGuard subjectGuard, IAccountLookup accounts,
                            ISubjectQuery subjects, ISubjectScope scope, IAuthorization authorization,
                            IAudit audit, IEventPublisher events, PlatformTransactionManager transactions, Clock clock) {
        this.classes=classes; this.enrollments=enrollments; this.reads=reads; this.subjectGuard=subjectGuard;
        this.accounts=accounts; this.subjects=subjects; this.scope=scope; this.authorization=authorization;
        this.audit=audit; this.events=events; this.clock=clock;
        this.transaction=new TransactionTemplate(transactions);
        this.lineTransaction=new TransactionTemplate(transactions);
        this.lineTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public List<ClassView> list(Actor actor, String audience, UUID subjectId, String query, ClassStatus status, int page, int size) {
        return guarded(actor,"CLASS_LIST",null,() -> {
            requireAudience(actor,audience); page(page,size); validateQuery(query);
            List<CourseClass> values;
            int offset=(page-1)*size;
            if (audience.equals("manager")) {
                var ids=scope.listManagedSubjects(actor.accountId());
                if(subjectId!=null) { requireManager(actor,subjectId); ids=List.of(subjectId); }
                values=ids.isEmpty()?List.of():reads.listForManager(ids,query,status,size,offset);
            } else if(audience.equals("teacher")) values=reads.listForTeacher(actor.accountId(),query,status,size,offset);
            else values=reads.listForStudent(actor.accountId(),query,status,size,offset);
            return views(values);
        });
    }

    public ClassView detail(Actor actor, String audience, UUID id) {
        return guarded(actor,"CLASS_VIEW",id,() -> { requireAudience(actor,audience); return view(requireClassAccess(actor,audience,id)); });
    }

    public List<Student> students(Actor actor,String audience,UUID id,int page,int size) {
        return guarded(actor,"CLASS_STUDENTS_VIEW",id,() -> {
            requireAudience(actor,audience);
            if(audience.equals("student")) throw ApiException.forbidden("ROLE_DENIED");
            requireClassAccess(actor,audience,id);
            page(page,size);
            return enrollments.listByClass(id,!audience.equals("manager"),size,(page-1)*size).stream().map(this::studentView).toList();
        });
    }

    public Student student(Actor actor,UUID id,UUID studentId) {
        return guarded(actor,"CLASS_STUDENT_VIEW",id,() -> {
            requireAudience(actor,"teacher"); requireClassAccess(actor,"teacher",id);
            Enrollment enrollment=enrollments.find(id,studentId).filter(e -> e.status()==EnrollmentStatus.ACTIVE)
                    .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND"));
            return studentView(enrollment);
        });
    }

    public List<SubjectSummary> managedSubjects(Actor actor) {
        return guarded(actor,"MANAGED_SUBJECTS_VIEW",null,() -> {
            requireAudience(actor,"manager");
            return subjects.listManagedSubjects(actor.accountId()).stream().filter(s -> scope.isSubjectManager(actor.accountId(),s.subjectId())).map(this::subjectSummary).toList();
        });
    }

    public SubjectSummary managedSubject(Actor actor,UUID id) {
        return guarded(actor,"MANAGED_SUBJECT_VIEW",id,() -> {requireAudience(actor,"manager");requireManager(actor,id);return subjectSummary(subject(id));});
    }

    public List<Person> teacherCandidates(Actor actor,String query) {
        return guarded(actor,"TEACHER_CANDIDATES_VIEW",null,() -> {
            requireAudience(actor,"manager"); validateQuery(query);
            return accounts.findAccounts(query,Set.of(Role.TEACHER,Role.SUBJECT_MANAGER),20).stream()
                    .filter(a -> teacherEligible(a)).limit(20).map(this::person).toList();
        });
    }

    public List<Person> studentCandidates(Actor actor,UUID id,String query) {
        return guarded(actor,"STUDENT_CANDIDATES_VIEW",id,() -> {
            requireAudience(actor,"manager"); requireClassAccess(actor,"manager",id); validateQuery(query);
            return accounts.findStudents(query,20).stream().filter(a -> a.role()==Role.STUDENT && a.status()!=AccountStatus.DISABLED)
                    .limit(20).map(this::person).toList();
        });
    }

    public ClassView create(Actor actor,UUID subjectId,String code,String name,String description,String term,UUID teacherId) {
        return command(actor,"CLASS_CREATED",subjectId,() -> {
            requireAudience(actor,"manager"); requireManager(actor,subjectId);
            requireManager(actor,subjectId);
            // Khóa tài khoản giảng viên trước dòng môn: U01 vô hiệu hóa tài khoản khóa tài khoản rồi mới tới môn.
            requireTeacher(teacherId);
            subjectGuard.lockActive(subjectId,SUBJECT_LOCK_WAIT); requireManager(actor,subjectId);
            CourseClass value=CourseClass.create(UUID.randomUUID(),subjectId,code,name,description,term,teacherId,clock.instant());
            classes.insert(value); success(actor,"CLASS_CREATED",value.id(),Map.of());
            if(teacherId!=null) success(actor,"CLASS_TEACHER_CHANGED",value.id(),teacherDetails(null,teacherId));
            return view(value);
        });
    }

    public ClassView updateInfo(Actor actor,UUID id,String name,String description,String term,long version) {
        return mutate(actor,"CLASS_UPDATED",id,version,c -> c.updateInfo(name,description,term,clock.instant()),Map.of());
    }
    public ClassView assignTeacher(Actor actor,UUID id,UUID teacherId,long version) {
        return command(actor,"CLASS_TEACHER_CHANGED",id,() -> {
            // Kiểm quyền trên lớp trước (giữ 403/404), rồi khóa tài khoản giảng viên trước dòng môn trong locked():
            // U01 vô hiệu hóa tài khoản khóa tài khoản rồi mới tới môn.
            requireAudience(actor,"manager"); requireManager(actor,requireClassAccess(actor,"manager",id).subjectId());
            requireTeacher(teacherId);
            CourseClass current=locked(actor,id,version);
            CourseClass changed=current.assignTeacher(teacherId,clock.instant());
            classes.update(changed,version); success(actor,"CLASS_TEACHER_CHANGED",id,teacherDetails(current.teacherId(),teacherId));
            return view(changed);
        });
    }
    public ClassView gradeDistribution(Actor actor,UUID id,boolean enabled,long version) {
        return mutate(actor,"GRADE_DISTRIBUTION_CHANGED",id,version,c -> c.setGradeDistribution(enabled,clock.instant()),Map.of("enabled",enabled));
    }
    public ClassView changeStatus(Actor actor,UUID id,ClassStatus status,long version) {
        return command(actor,"CLASS_STATUS_CHANGED",id,() -> {
            if(status==null) throw ApiException.badRequest("INVALID_STATUS","Chọn trạng thái lớp");
            CourseClass current=locked(actor,id,version);
            boolean subjectActive=false;
            if(status==ClassStatus.OPEN) {
                requireTeacherStillEligible(current.teacherId());
                subjectActive=subjects.isActive(current.subjectId());
                var conflicting=enrollments.conflictingStudents(id,current.subjectId());
                if(!conflicting.isEmpty()) {
                    var conflictingNames=conflicting.stream()
                            .map(studentId -> accounts.getContact(studentId)
                                    .map(AccountContact::displayName)
                                    .filter(name -> !name.isBlank())
                                    .orElse("Sinh viên không còn thông tin tài khoản"))
                            .toList();
                    throw new AcademicsProblem("ENROLLMENT_CONFLICT","Có sinh viên đang ở lớp khác cùng môn",
                            Map.of("conflictingStudents",conflictingNames));
                }
            }
            CourseClass changed=current.changeStatus(status,subjectActive,false,clock.instant());
            classes.update(changed,version);
            success(actor,"CLASS_STATUS_CHANGED",id,Map.of("before",current.status().name(),"after",status.name()));
            if(current.status()==ClassStatus.DRAFT && status==ClassStatus.OPEN) {
                for(int offset=0;;offset+=100) {
                    var page=enrollments.listByClass(id,true,100,offset);
                    page.forEach(e -> activated(changed,e.accountId()));
                    if(page.size()<100) break;
                }
            }
            return view(changed);
        });
    }

    public EnrollmentResult enroll(Actor actor,UUID id,UUID accountId) {
        return command(actor,"STUDENT_ENROLLED",id,() -> {
            CourseClass current=locked(actor,id,null); editable(current);
            AccountContact account=accounts.getContact(Objects.requireNonNull(accountId)).orElse(null);
            return enrollLine(actor,current,account,EnrollmentSource.MANUAL);
        });
    }

    public List<RowResult> enrollBatch(Actor actor,UUID id,String text) {
        return guarded(actor,"STUDENT_ENROLLED",id,() -> {
            requireAudience(actor,"manager"); editable(requireClassAccess(actor,"manager",id));
            var rows=EnrollmentListParser.parse(text);
            var emails=rows.stream().filter(r -> r.error()==null).map(EnrollmentListParser.Row::email).toList();
            Map<String,AccountContact> found=accounts.findByEmails(emails).stream()
                    .collect(Collectors.toMap(a -> a.email().toLowerCase(Locale.ROOT),Function.identity(),(a,b)->a));
            var results=new ArrayList<RowResult>();
            for(var row:rows) {
                EnrollmentResult result=row.error();
                if(result==null) result=lineTransaction.execute(tx -> {
                    AccountContact resolved=found.get(row.email());
                    // Khóa tài khoản trước môn như U01: mỗi dòng dùng role/trạng thái hiện tại tới lúc commit.
                    AccountContact account=resolved==null?null:accounts.lockContact(resolved.accountId()).orElse(null);
                    CourseClass current=locked(actor,id,null); editable(current);
                    return enrollLine(actor,current,account,EnrollmentSource.LIST);
                });
                results.add(new RowResult(row.lineNumber(),row.email(),result));
            }
            return List.copyOf(results);
        });
    }

    public EnrollmentResult enrollmentStatus(Actor actor,UUID id,UUID accountId,EnrollmentStatus status) {
        if(status==EnrollmentStatus.ACTIVE) return command(actor,"STUDENT_ENROLLED",id,() -> {
            CourseClass current=locked(actor,id,null); editable(current);
            AccountContact account=accounts.getContact(accountId).orElse(null);
            EnrollmentResult result=enrollLine(actor,current,account,EnrollmentSource.MANUAL);
            if(result==EnrollmentResult.IN_OTHER_CLASS) throw ApiException.conflict("IN_OTHER_CLASS","Sinh viên đang ở lớp khác cùng môn");
            if(result!=EnrollmentResult.ENROLLED && result!=EnrollmentResult.RESTORED && result!=EnrollmentResult.ALREADY_ENROLLED)
                throw ApiException.badRequest(result.name(),"Không thể khôi phục ghi danh. Kiểm tra tài khoản sinh viên");
            return result;
        });
        return command(actor,"STUDENT_REMOVED",id,() -> {
            if(status==null) throw ApiException.badRequest("INVALID_STATUS","Chọn trạng thái ghi danh");
            CourseClass current=locked(actor,id,null); editable(current);
            Enrollment old=enrollments.find(id,accountId).orElseThrow(() -> ApiException.notFound("ENROLLMENT_NOT_FOUND"));
            if(old.status()==EnrollmentStatus.ACTIVE) {
                enrollments.update(old.remove(clock.instant()),EnrollmentStatus.ACTIVE);
                success(actor,"STUDENT_REMOVED",id,Map.of("accountId",accountId.toString()));
            }
            return EnrollmentResult.ALREADY_ENROLLED;
        });
    }

    private EnrollmentResult enrollLine(Actor actor,CourseClass current,AccountContact account,EnrollmentSource source) {
        if(account==null) return EnrollmentResult.NOT_FOUND;
        if(account.role()!=Role.STUDENT) return EnrollmentResult.NOT_STUDENT;
        if(account.status()==AccountStatus.DISABLED) return EnrollmentResult.DISABLED;
        var existing=enrollments.find(current.id(),account.accountId()).orElse(null);
        var result=EnrollmentPolicy.resultFor(current.status(),existing,enrollments.activeInOtherClass(account.accountId(),current.subjectId(),current.id()));
        if(result==EnrollmentResult.ENROLLED || result==EnrollmentResult.RESTORED) {
            Enrollment value=existing==null?Enrollment.create(current.id(),account.accountId(),source,clock.instant()):existing.restore(source,clock.instant());
            if(existing==null) enrollments.insert(value); else enrollments.update(value,EnrollmentStatus.REMOVED);
            success(actor,"STUDENT_ENROLLED",current.id(),Map.of("accountId",account.accountId().toString(),"result",result.name()));
            if(current.status()==ClassStatus.OPEN) activated(current,account.accountId());
        }
        return result;
    }

    private ClassView mutate(Actor actor,String action,UUID id,long version,UnaryOperator<CourseClass> change,Map<String,Object> details) {
        return command(actor,action,id,() -> {
            CourseClass current=locked(actor,id,version);
            CourseClass changed=change.apply(current);
            classes.update(changed,version);
            success(actor,action,id,details);
            return view(changed);
        });
    }
    private CourseClass locked(Actor actor,UUID id,Long version) {
        requireAudience(actor,"manager");
        CourseClass before=requireClassAccess(actor,"manager",id);
        subjectGuard.lockActive(before.subjectId(),SUBJECT_LOCK_WAIT);
        CourseClass current=requireClassAccess(actor,"manager",id);
        requireManager(actor,current.subjectId());
        if(version!=null && (version<0 || current.version()!=version)) throw ApiException.conflict("CLASS_VERSION_CONFLICT","Dữ liệu lớp đã thay đổi. Hãy tải lại");
        return current;
    }
    private CourseClass requireClassAccess(Actor actor,String audience,UUID id) {
        CourseClass value=classes.find(id).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        boolean allowed=switch(audience) {
            case "manager" -> scope.isSubjectManager(actor.accountId(),value.subjectId());
            case "teacher" -> actor.accountId().equals(value.teacherId());
            case "student" -> value.status()==ClassStatus.OPEN && enrollments.find(id,actor.accountId()).map(e -> e.status()==EnrollmentStatus.ACTIVE).orElse(false);
            default -> false;
        };
        if(!allowed) throw ApiException.notFound("CLASS_OUT_OF_SCOPE");
        // Chủ nhiệm xem lớp của môn mình theo phạm vi môn (R2), không cần được giao dạy lớp đó.
        if(audience.equals("manager")) authorize(actor,Actions.CLASS_MANAGE,ResourceRef.subject(value.subjectId()),true);
        else authorize(actor,Actions.CLASS_VIEW,ResourceRef.courseClass(id),true);
        return value;
    }
    private void requireManager(Actor actor,UUID subjectId) {
        if(subjectId==null || !scope.isSubjectManager(actor.accountId(),subjectId) || !actor.accountId().equals(subject(subjectId).managerId())) throw ApiException.notFound("SUBJECT_OUT_OF_SCOPE");
        authorize(actor,Actions.CLASS_MANAGE,ResourceRef.subject(subjectId),true);
    }
    private void requireAudience(Actor actor,String audience) {
        if(actor==null || actor.accountId()==null || actor.role()==null) throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Cần đăng nhập");
        boolean roleAllowed=switch(audience) {
            case "manager" -> actor.role()==Role.SUBJECT_MANAGER;
            case "teacher" -> actor.role()==Role.TEACHER || actor.role()==Role.SUBJECT_MANAGER;
            case "student" -> actor.role()==Role.STUDENT;
            default -> false;
        };
        if(!roleAllowed) throw ApiException.forbidden("ROLE_DENIED");
        AccountContact contact=accounts.getContact(actor.accountId()).orElseThrow(() -> ApiException.forbidden("ACCOUNT_INACTIVE"));
        if(contact.status()!=AccountStatus.ACTIVE || contact.role()!=actor.role()) throw ApiException.forbidden("ACCOUNT_INACTIVE");
        String action=switch(audience) {
            case "manager" -> Actions.CLASS_MANAGER;
            case "teacher" -> Actions.CLASS_TEACHER;
            default -> Actions.CLASS_STUDENT;
        };
        authorize(actor,action,ResourceRef.none(),false);
    }
    private void authorize(Actor actor,String action,ResourceRef ref,boolean hideDeniedResource) {
        var decision=authorization.authorize(actor,action,ref);
        if(decision==null || !decision.allowed()) {
            if(hideDeniedResource) throw ApiException.notFound("CLASS_OUT_OF_SCOPE");
            throw ApiException.forbidden("ACCESS_DENIED");
        }
    }
    /** Gán giảng viên: khóa tài khoản tới hết transaction để U01 không đổi role xen vào (BR-U01-64). */
    private void requireTeacher(UUID id) {
        if(id==null) return;
        var contact=accounts.lockContact(id).orElseThrow(() -> ApiException.badRequest("INVALID_TEACHER","Không tìm thấy giảng viên"));
        if(!teacherEligible(contact)) throw ApiException.badRequest("INVALID_TEACHER","Chọn giảng viên đang hoạt động");
    }
    /** Mở lại lớp với giảng viên đã gán: không tạo phân công mới nên không cần khóa tài khoản. */
    private void requireTeacherStillEligible(UUID id) {
        if(id==null) return;
        var contact=accounts.getContact(id).orElseThrow(() -> ApiException.badRequest("INVALID_TEACHER","Không tìm thấy giảng viên"));
        if(!teacherEligible(contact)) throw ApiException.badRequest("INVALID_TEACHER","Chọn giảng viên đang hoạt động");
    }
    private boolean teacherEligible(AccountContact contact) { return contact.status()==AccountStatus.ACTIVE && (contact.role()==Role.TEACHER || contact.role()==Role.SUBJECT_MANAGER); }
    private void editable(CourseClass value) { if(value.status()==ClassStatus.ARCHIVED) throw ApiException.conflict("CLASS_ARCHIVED","Lớp đã lưu trữ chỉ được xem"); }
    private SubjectRef subject(UUID id) { return subjects.getSubject(id).orElseThrow(() -> ApiException.unavailable("SUBJECT_UNAVAILABLE","Không đọc được môn học")); }
    private List<ClassView> views(List<CourseClass> values) {
        var counts=reads.enrollmentCounts(values.stream().map(CourseClass::id).toList());
        return values.stream().map(courseClass -> view(courseClass,counts.getOrDefault(courseClass.id(),new IAcademicsReadRepository.EnrollmentCounts(0,0)))).toList();
    }
    private ClassView view(CourseClass value) {
        return views(List.of(value)).get(0);
    }
    private ClassView view(CourseClass courseClass,IAcademicsReadRepository.EnrollmentCounts counts) {
        var subject=subject(courseClass.subjectId());
        Person teacher=courseClass.teacherId()==null?null:person(accounts.getContact(courseClass.teacherId())
                .orElseThrow(() -> ApiException.unavailable("ACCOUNT_UNAVAILABLE","Không đọc được giảng viên")));
        return new ClassView(courseClass.id(),new Subject(subject.subjectId(),subject.code(),subject.name()),
                courseClass.code(),courseClass.name(),Objects.requireNonNullElse(courseClass.description(),""),
                Objects.requireNonNullElse(courseClass.term(),""),courseClass.status(),teacher,counts.active(),counts.removed(),
                courseClass.createdAt(),courseClass.showGradeDistribution(),courseClass.version());
    }
    private SubjectSummary subjectSummary(SubjectRef subject) {
        var counts=new EnumMap<ClassStatus,Long>(ClassStatus.class);
        for(var status:ClassStatus.values()) counts.put(status,0L);
        reads.listOfSubject(subject.subjectId()).forEach(courseClass -> counts.merge(courseClass.status(),1L,Long::sum));
        return new SubjectSummary(subject.subjectId(),subject.code(),subject.name(),subject.status(),Map.copyOf(counts));
    }
    private Student studentView(Enrollment enrollment) {
        var account=accounts.getContact(enrollment.accountId())
                .orElseThrow(() -> ApiException.unavailable("ACCOUNT_UNAVAILABLE","Không đọc được sinh viên"));
        return new Student(enrollment.accountId(),account.displayName(),account.email(),enrollment.enrolledAt(),enrollment.status());
    }
    private Person person(AccountContact account) {
        return new Person(account.accountId(),account.displayName(),account.email());
    }
    private Map<String,Object> teacherDetails(UUID before,UUID after) {
        var details=new HashMap<String,Object>();
        details.put("before",before==null?"":before.toString());
        details.put("after",after==null?"":after.toString());
        return details;
    }
    private void activated(CourseClass courseClass,UUID accountId) {
        events.publish(new DomainEvent(1,UUID.randomUUID(),EventTypes.ENROLLMENT_ACTIVATED,"U05",clock.instant(),UUID.randomUUID().toString(),
                Map.of("classId",courseClass.id().toString(),"subjectId",courseClass.subjectId().toString(),"accountId",accountId.toString())));
    }
    private void success(Actor actor,String action,UUID id,Map<String,Object> details) { record(actor,action,id,AuditResult.SUCCESS,null,details); }
    private void record(Actor actor,String action,UUID id,AuditResult result,String reason,Map<String,Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(),actor==null?null:actor.accountId(),action,"COURSE_CLASS",id==null?null:id.toString(),result,reason,details,clock.instant()));
    }
    private <T>T command(Actor actor,String action,UUID id,Supplier<T> work) {
        return guarded(actor,action,id,() -> transaction.execute(tx -> work.get()));
    }
    private <T>T guarded(Actor actor,String action,UUID id,Supplier<T> work) {
        try { return work.get(); }
        catch(RuntimeException failure) {
            ApiException problem=mapFailure(failure);
            boolean denied=problem.status().value()==401 || problem.status().value()==403 || problem.status().value()==404;
            try {
                record(actor,denied?"ACCESS_DENIED":action,id,denied?AuditResult.DENIED:AuditResult.FAILURE,problem.code(),Map.of());
            } catch(RuntimeException auditFailure) {
                throw ApiException.unavailable("AUDIT_UNAVAILABLE","Không thể ghi nhật ký an toàn");
            }
            throw problem;
        }
    }
    private ApiException mapFailure(RuntimeException failure) {
        if(failure instanceof ApiException problem) return problem;
        if(failure instanceof AcademicRuleException rule) {
            if(Set.of("CLASS_EXISTS","CLASS_VERSION_CONFLICT","ENROLLMENT_EXISTS","ENROLLMENT_CONFLICT","CLASS_ARCHIVED","INVALID_CLASS_TRANSITION","TEACHER_REQUIRED","SUBJECT_INACTIVE").contains(rule.code())) return ApiException.conflict(rule.code(),rule.getMessage());
            if(rule.code().equals("LIST_TOO_LARGE")) return ApiException.payloadTooLarge(rule.code(),rule.getMessage());
            return ApiException.badRequest(rule.code(),rule.getMessage());
        }
        if(failure instanceof OptimisticLockingFailureException) return ApiException.conflict("CLASS_VERSION_CONFLICT","Dữ liệu đã thay đổi. Hãy tải lại");
        return ApiException.unavailable("ACADEMICS_DEPENDENCY_UNAVAILABLE","Không thể hoàn thành yêu cầu. Hãy thử lại sau");
    }
    private void page(int page,int size) {if(page<1 || size<1 || size>100 || (long)(page-1)*size>Integer.MAX_VALUE) throw ApiException.badRequest("INVALID_PAGE","Trang phải từ 1, kích thước từ 1 đến 100");}
    private void validateQuery(String query) {if(query!=null && query.length()>200) throw ApiException.badRequest("INVALID_QUERY","Từ khóa tối đa 200 ký tự");}
}
