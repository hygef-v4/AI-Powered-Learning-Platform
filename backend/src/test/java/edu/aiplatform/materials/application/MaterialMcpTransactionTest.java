package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.domain.IMaterialRepository;
import java.sql.Connection;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class MaterialMcpTransactionTest {
    @Test void expectedNotFoundRemainsStructuredToolErrorAcrossRealTransactionProxies() throws Exception {
        DataSource source=mock(DataSource.class);Connection connection=mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        var manager=new DataSourceTransactionManager(source);
        IMaterialRepository repository=mock(IMaterialRepository.class);UUID id=UUID.randomUUID();
        when(repository.find(id)).thenReturn(Optional.empty());
        var queries=proxy(new MaterialQueryService(repository,mock(MaterialAccessPolicy.class),mock(IArtifact.class)),manager);
        var references=proxy(new ReferenceMaterialService(queries,repository),manager);
        var tools=proxy(new MaterialMcpTools(queries,repository),manager);
        var result=tools.call(new Actor(UUID.randomUUID(),Role.TEACHER),"read_authorized_material",Map.of("materialId",id.toString()));
        assertThat(result.error()).isTrue();assertThat(((Map<?,?>)result.content()).get("code")).isEqualTo("MATERIAL_NOT_FOUND");
        verify(connection).commit();verify(connection,never()).rollback();
    }
    @SuppressWarnings("unchecked")
    private <T> T proxy(T target,DataSourceTransactionManager manager) {
        var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        return (T)factory.getProxy();
    }
}
