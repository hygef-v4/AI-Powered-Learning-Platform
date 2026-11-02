package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly=true, timeout=10)
public class MaterialModuleRepositoryAdapter implements IMaterialModuleRepository {
    private static final String COLUMNS="id,subject_id,title,order_no,created_by,created_at,updated_at,version";
    @PersistenceContext private EntityManager em;

    @Override public Optional<MaterialModule> find(UUID id) {
        List<?> rows=em.createNativeQuery("select "+COLUMNS+" from material_modules where id=?1").setParameter(1,id).getResultList();
        return rows.stream().findFirst().map(row->module((Object[])row));
    }
    @Override public List<MaterialModule> list(UUID subjectId) {
        List<?> rows=em.createNativeQuery("select "+COLUMNS+" from material_modules where subject_id=?1 order by order_no,id")
                .setParameter(1,subjectId).getResultList();
        return rows.stream().map(row->module((Object[])row)).toList();
    }
    @Override @Transactional(timeout=10) public void insert(MaterialModule m) {
        em.createNativeQuery("insert into material_modules("+COLUMNS+") values (?1,?2,?3,?4,?5,?6,?7,?8)")
                .setParameter(1,m.id()).setParameter(2,m.subjectId()).setParameter(3,m.title()).setParameter(4,m.orderNo())
                .setParameter(5,m.createdBy()).setParameter(6,Timestamp.from(m.createdAt())).setParameter(7,Timestamp.from(m.updatedAt()))
                .setParameter(8,m.version()).executeUpdate();
    }
    @Override @Transactional(timeout=10) public boolean update(MaterialModule m,long expected) {
        return em.createNativeQuery("update material_modules set title=?1,order_no=?2,updated_at=?3,version=?4 where id=?5 and version=?6")
                .setParameter(1,m.title()).setParameter(2,m.orderNo()).setParameter(3,Timestamp.from(m.updatedAt()))
                .setParameter(4,m.version()).setParameter(5,m.id()).setParameter(6,expected).executeUpdate()==1;
    }
    @Override @Transactional(timeout=10) public boolean delete(UUID id,long expected) {
        return em.createNativeQuery("delete from material_modules where id=?1 and version=?2")
                .setParameter(1,id).setParameter(2,expected).executeUpdate()==1;
    }
    @Override public int activeMaterials(UUID moduleId) {
        return count("select count(*) from materials where module_id=?1 and status='ACTIVE'",moduleId);
    }
    @Override public int archivedMaterials(UUID moduleId) {
        return count("select count(*) from materials where module_id=?1 and status<>'ACTIVE'",moduleId);
    }
    @Override @Transactional(timeout=10) public void moveArchivedMaterials(UUID from,UUID to) {
        em.createNativeQuery("update materials set module_id=?2 where module_id=?1 and status<>'ACTIVE'")
                .setParameter(1,from).setParameter(2,to).executeUpdate();
    }
    @Override public List<MaterialModule.QuizLink> quizLinks(UUID subjectId) {
        List<?> rows=em.createNativeQuery("select q.module_id,q.quiz_id,q.order_no from material_module_quizzes q "
                + "join material_modules m on m.id=q.module_id where m.subject_id=?1 order by q.order_no,q.created_at,q.quiz_id")
                .setParameter(1,subjectId).getResultList();
        return rows.stream().map(row->link((Object[])row)).toList();
    }
    @Override public Optional<MaterialModule.QuizLink> quizLink(UUID quizId) {
        List<?> rows=em.createNativeQuery("select module_id,quiz_id,order_no from material_module_quizzes where quiz_id=?1")
                .setParameter(1,quizId).getResultList();
        return rows.stream().findFirst().map(row->link((Object[])row));
    }
    @Override @Transactional(timeout=10) public void link(MaterialModule.QuizLink l) {
        em.createNativeQuery("insert into material_module_quizzes(module_id,quiz_id,order_no,created_at) values (?1,?2,?3,?4)")
                .setParameter(1,l.moduleId()).setParameter(2,l.quizId()).setParameter(3,l.orderNo())
                .setParameter(4,Timestamp.from(Instant.now())).executeUpdate();
    }
    @Override @Transactional(timeout=10) public boolean unlink(UUID moduleId,UUID quizId) {
        return em.createNativeQuery("delete from material_module_quizzes where module_id=?1 and quiz_id=?2")
                .setParameter(1,moduleId).setParameter(2,quizId).executeUpdate()==1;
    }

    private int count(String sql,UUID id) {
        return ((Number)em.createNativeQuery(sql).setParameter(1,id).getSingleResult()).intValue();
    }
    private static MaterialModule module(Object[] r) {
        return new MaterialModule((UUID)r[0],(UUID)r[1],(String)r[2],((Number)r[3]).intValue(),(UUID)r[4],
                instant(r[5]),instant(r[6]),((Number)r[7]).longValue());
    }
    private static MaterialModule.QuizLink link(Object[] r) {
        return new MaterialModule.QuizLink((UUID)r[0],(UUID)r[1],((Number)r[2]).intValue());
    }
    private static Instant instant(Object value) {
        if(value instanceof Instant i)return i;
        if(value instanceof Timestamp t)return t.toInstant();
        if(value instanceof java.time.OffsetDateTime o)return o.toInstant();
        throw new IllegalStateException("Unexpected timestamp "+value);
    }
}
