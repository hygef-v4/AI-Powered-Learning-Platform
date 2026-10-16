package edu.aiplatform.files.application;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.domain.SettingValidator;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Nhóm cài đặt Tệp (BR-U03-87). */
@Configuration
public class FileSettingDefinitions {

    @Bean
    SettingDefinition materialMaxSize() {
        return new SettingDefinition("files.material.maxSizeMb", SettingGroup.FILES, "Dung lượng tối đa học liệu (MB)",
                "Trần dung lượng mỗi tệp học liệu PDF, DOCX, PPTX", SettingType.INTEGER, 50,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 50));
    }

    @Bean
    SettingDefinition materialTypes() {
        return new SettingDefinition("files.material.allowedTypes", SettingGroup.FILES, "Loại tệp học liệu được phép",
                "Loại tệp giảng viên được tải lên làm học liệu", SettingType.ENUM_SET, List.of("PDF", "DOCX", "PPTX"),
                Map.of(SettingValidator.OPTIONS, List.of("PDF", "DOCX", "PPTX"), SettingValidator.NON_EMPTY, true));
    }

    @Bean
    SettingDefinition imageMaxSize() {
        return new SettingDefinition("files.documentImage.maxSizeMb", SettingGroup.FILES,
                "Dung lượng tối đa ảnh trong tài liệu (MB)", "Trần dung lượng mỗi ảnh chèn vào đề và bài làm",
                SettingType.INTEGER, 5, Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 5));
    }

    @Bean
    SettingDefinition imageTypes() {
        return new SettingDefinition("files.documentImage.allowedTypes", SettingGroup.FILES,
                "Loại ảnh được phép trong tài liệu", "Loại ảnh được chèn vào đề và bài làm", SettingType.ENUM_SET,
                List.of("PNG", "JPEG", "GIF", "SVG"),
                Map.of(SettingValidator.OPTIONS, List.of("PNG", "JPEG", "GIF", "SVG"), SettingValidator.NON_EMPTY,
                        true));
    }
}
