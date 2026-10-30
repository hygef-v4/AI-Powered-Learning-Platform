package edu.aiplatform.academics.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.academics.repository.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AcademicsMcpToolsTest {
    private final IAcademicsReadRepository reads = mock(IAcademicsReadRepository.class);
    private final ISubjectQuery subjects = mock(ISubjectQuery.class);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);

    @Test void onlyTaughtClassesArePagedAndSubjectsAreDeduplicated() {
        UUID subject = UUID.randomUUID();
        var now=Instant.now();
        var one=CourseClass.create(UUID.randomUUID(),subject,"C1","First","","",teacher.accountId(),now);
        var two=CourseClass.create(UUID.randomUUID(),subject,"C2","Second","","",teacher.accountId(),now);
        when(reads.listTaught(teacher.accountId())).thenReturn(List.of(one,two));
        when(subjects.getSubject(subject)).thenReturn(Optional.of(new SubjectRef(subject,"S","Subject",SubjectStatus.ACTIVE,UUID.randomUUID())));
        var tools=new AcademicsMcpTools(reads,subjects);
        var result=(Map<?,?>)tools.call(teacher,"list_my_classes",Map.of("size",1,"page",1)).content();
        assertThat(result.get("total")).isEqualTo(2);
        assertThat((List<?>)result.get("items")).hasSize(1);
        var found=(Map<?,?>)tools.call(teacher,"list_my_subjects",Map.of()).content();
        assertThat((List<?>)found.get("items")).hasSize(1);
        verify(subjects,never()).listManagedSubjects(teacher.accountId());
    }

    @Test void studentAdminAndInvalidPaginationAreRejectedBeforeQueries() {
        var tools=new AcademicsMcpTools(reads,subjects);
        for(Role role:List.of(Role.STUDENT,Role.ADMIN)) {
            assertThatThrownBy(()->tools.call(new Actor(UUID.randomUUID(),role),"list_my_classes",Map.of())).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(()->tools.call(teacher,"list_my_classes",Map.of("page",-1))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->tools.call(teacher,"list_my_classes",Map.of("size",101))).isInstanceOf(ApiException.class);
        verifyNoInteractions(reads);
    }
}
