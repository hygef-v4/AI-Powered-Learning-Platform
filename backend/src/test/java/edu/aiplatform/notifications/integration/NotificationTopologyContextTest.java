package edu.aiplatform.notifications.integration;

import edu.aiplatform.notifications.config.NotificationTopology;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Declarables;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotificationTopologyContextTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void topologyConfigurationStartsWithoutBeanNameCollision(boolean allowOverriding) {
        new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(allowOverriding)
                .withUserConfiguration(NotificationTopology.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(NotificationTopology.class);
                    assertThat(context).hasSingleBean(Declarables.class);
                });
    }
}
