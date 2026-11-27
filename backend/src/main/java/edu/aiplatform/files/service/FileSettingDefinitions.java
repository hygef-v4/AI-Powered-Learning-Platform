package edu.aiplatform.files.service;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.model.SettingValidator;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nhóm cài đặt Tệp (BR-U03-87): chỉ dung lượng tối đa của tài liệu và ảnh. Tệp nguồn soạn bài và tệp nhập đề dùng trần
 * cứng trong {@code PurposePolicy}, không có setting (yêu cầu 2026-11-26).
 */
@Configuration
public class FileSettingDefinitions {

    @Bean
    SettingDefinition materialMaxSize() {
        return new SettingDefinition("files.material.maxSizeMb", SettingGroup.FILES, "Tài liệu tối đa",
                "Dung lượng tối đa của mỗi tệp tài liệu.",
                SettingType.INTEGER, 50,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 50, SettingValidator.UNIT, "MB"));
    }

    @Bean
    SettingDefinition imageMaxSize() {
        return new SettingDefinition("files.documentImage.maxSizeMb", SettingGroup.FILES, "Ảnh tối đa",
                "Dung lượng tối đa của mỗi ảnh trong tài liệu.", SettingType.INTEGER, 5,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 5, SettingValidator.UNIT, "MB"));
    }
}
