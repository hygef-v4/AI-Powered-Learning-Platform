package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.repository.IMaterialRepository;
import java.sql.Connection;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class MaterialMcpTransactionTest {
    @Test
    void expectedNotFoundRemainsStructuredToolErrorAcrossRealTransactionProxies() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getTransactionIsolation())
                .thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        var manager = new DataSourceTransactionManager(source);
        IMaterialRepository materialRepository = mock(IMaterialRepository.class);
        UUID id = UUID.randomUUID();
        when(materialRepository.findMaterial(id)).thenReturn(Optional.empty());
        var materialQueryService =
                proxy(
                        new MaterialQueryService(
                                materialRepository,
                                mock(MaterialAccessPolicy.class),
                                mock(IArtifact.class)),
                        manager);
        var referenceMaterialService =
                proxy(
                        new ReferenceMaterialService(materialQueryService, materialRepository),
                        manager);
        var tools = proxy(new MaterialMcpTools(materialQueryService, materialRepository), manager);
        var result =
                tools.call(
                        new Actor(UUID.randomUUID(), Role.TEACHER),
                        "read_authorized_material",
                        Map.of("materialId", id.toString()));
        assertThat(result.error()).isTrue();
        assertThat(((Map<?, ?>) result.content()).get("code")).isEqualTo("MATERIAL_NOT_FOUND");
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(T target, DataSourceTransactionManager manager) {
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(
                new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
}
