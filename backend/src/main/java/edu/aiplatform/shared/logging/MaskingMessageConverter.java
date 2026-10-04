package edu.aiplatform.shared.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/** Converter `%maskedMsg` dùng trong logback-spring.xml. */
public class MaskingMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataMasker.mask(super.convert(event));
    }
}
