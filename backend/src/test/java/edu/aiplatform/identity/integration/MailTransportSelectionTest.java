package edu.aiplatform.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.config.IdentityProperties;
import edu.aiplatform.identity.contract.IMail;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

/** {@code MAIL_TRANSPORT} chọn đúng một adapter cho {@link IMail}. */
class MailTransportSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(IdentityProperties.class, IdentityTestData::properties)
            .withUserConfiguration(MailTransport.class, SmtpMailAdapter.class, BrevoApiMailAdapter.class);

    @Test
    void smtpIsTheDefault() {
        runner.run(context -> assertThat(context).hasSingleBean(IMail.class).getBean(IMail.class)
                .isInstanceOf(SmtpMailAdapter.class));
    }

    @Test
    void brevoApiSelectsTheHttpsAdapter() {
        runner.withPropertyValues(MailTransport.PROPERTY + "=brevo-api")
                .run(context -> assertThat(context).hasSingleBean(IMail.class).getBean(IMail.class)
                        .isInstanceOf(BrevoApiMailAdapter.class));
    }

    @Test
    void unknownTransportStopsStartup() {
        runner.withPropertyValues(MailTransport.PROPERTY + "=sendgrid")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasRootCauseMessage("MAIL_TRANSPORT phải là smtp hoặc brevo-api"));
    }
}
