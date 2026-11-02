package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.CourseClass;
import edu.aiplatform.academics.repository.IAcademicsReadRepository;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAcademicsReadRepository implements IAcademicsReadRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcAcademicsReadRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<CourseClass> listForManager(Collection<UUID> subjectIds, String query, ClassStatus status, int limit, int offset) {
        PersistenceChecks.page(limit,offset);
        if (subjectIds.isEmpty()) return List.of();
        return page("c.subject_id in (:subjectIds)",Map.of("subjectIds",List.copyOf(subjectIds)),query,status,limit,offset);
    }
    @Override public List<CourseClass> listForTeacher(UUID accountId,String query,ClassStatus status,int limit,int offset) {
        return page("c.teacher_id = :accountId",Map.of("accountId",accountId),query,status,limit,offset);
    }
    @Override public List<CourseClass> listForStudent(UUID accountId,String query,ClassStatus status,int limit,int offset) {
        return page("""
                c.status in ('OPEN', 'ARCHIVED') and exists
                  (select 1 from enrollments e where e.class_id = c.id and e.account_id = :accountId and e.status = 'ACTIVE')
                """,Map.of("accountId",accountId),query,status,limit,offset);
    }
    private List<CourseClass> page(String scope,Map<String,Object> scopeArgs,String query,ClassStatus status,int limit,int offset) {
        PersistenceChecks.page(limit,offset);
        var args=new HashMap<>(scopeArgs);
        args.put("limit",limit); args.put("offset",offset);
        String filter="";
        if (status!=null) { filter+=" and c.status = :status"; args.put("status",status.name()); }
        if (query!=null && !query.isBlank()) {
            if (query.length()>200) throw new IllegalArgumentException("Search text is too long");
            String escaped=query.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
            args.put("search","%"+escaped+"%");
            filter+=" and (lower(c.code) like :search escape '!' or lower(c.name) like :search escape '!')";
        }
        return jdbc.query("select c.* from course_classes c where "+scope+filter+" order by c.code, c.id limit :limit offset :offset",args,JdbcCourseClassRepository::map);
    }
    @Override public List<CourseClass> listTaught(UUID accountId) {
        return jdbc.query("select c.* from course_classes c where c.teacher_id = :accountId order by c.code,c.id",Map.of("accountId",accountId),JdbcCourseClassRepository::map);
    }
    @Override public List<CourseClass> listOfSubject(UUID subjectId) {
        return jdbc.query("select c.* from course_classes c where c.subject_id = :subjectId order by c.code,c.id",Map.of("subjectId",subjectId),JdbcCourseClassRepository::map);
    }
    @Override public Map<ClassStatus,Long> countClassesByStatus() {
        var counts=new EnumMap<ClassStatus,Long>(ClassStatus.class);
        for (ClassStatus status:ClassStatus.values()) counts.put(status,0L);
        jdbc.query("select status,count(*) as total from course_classes group by status",Map.of(),
                (rs,row)->Map.entry(ClassStatus.valueOf(rs.getString("status")),rs.getLong("total"))).forEach(e->counts.put(e.getKey(),e.getValue()));
        return Map.copyOf(counts);
    }
    @Override public Map<UUID,Integer> countClassesOfSubjects(Collection<UUID> subjectIds) {
        if (subjectIds.isEmpty()) return Map.of();
        var counts=new HashMap<UUID,Integer>(); subjectIds.forEach(id->counts.put(id,0));
        jdbc.query("select subject_id,count(*) as total from course_classes where subject_id in (:subjectIds) group by subject_id",
                Map.of("subjectIds",List.copyOf(subjectIds)),(rs,row)->Map.entry(rs.getObject("subject_id",UUID.class),Math.toIntExact(rs.getLong("total"))))
                .forEach(e->counts.put(e.getKey(),e.getValue()));
        return Map.copyOf(counts);
    }
    @Override public int countUnarchivedClasses(UUID subjectId) {
        Long value=jdbc.queryForObject("select count(*) from course_classes where subject_id = :subjectId and status <> 'ARCHIVED'",Map.of("subjectId",subjectId),Long.class);
        return Math.toIntExact(value==null?0:value);
    }
    @Override public long countActiveEnrollments() {
        Long value=jdbc.queryForObject("select count(*) from enrollments where status = 'ACTIVE'",Map.of(),Long.class);
        return value==null?0:value;
    }
    @Override public Map<UUID,EnrollmentCounts> enrollmentCounts(Collection<UUID> classIds) {
        if (classIds.isEmpty()) return Map.of();
        var counts=new HashMap<UUID,EnrollmentCounts>(); classIds.forEach(id->counts.put(id,new EnrollmentCounts(0,0)));
        jdbc.query("""
                select class_id, count(*) filter (where status = 'ACTIVE') as active,
                    count(*) filter (where status = 'REMOVED') as removed
                from enrollments where class_id in (:classIds) group by class_id
                """,Map.of("classIds",List.copyOf(classIds)),
                (rs,row)->Map.entry(rs.getObject("class_id",UUID.class),new EnrollmentCounts(rs.getLong("active"),rs.getLong("removed"))))
                .forEach(e->counts.put(e.getKey(),e.getValue()));
        return Map.copyOf(counts);
    }
}
