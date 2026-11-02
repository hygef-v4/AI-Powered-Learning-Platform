package edu.aiplatform.files.service;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.model.SettingValidator;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Nhóm cài đặt Tệp (BR-U03-87): chỉ dung lượng tối đa; loại tệp cố định trong {@code PurposePolicy}. */
@Configuration
public class FileSettingDefinitions {

    @Bean
    SettingDefinition materialMaxSize() {
        return new SettingDefinition("files.material.maxSizeMb", SettingGroup.FILES, "Học liệu tối đa",
                "Dung lượng tối đa mỗi tệp học liệu.",
                SettingType.INTEGER, 50,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 50, SettingValidator.UNIT, "MB"));
    }

    @Bean
    SettingDefinition imageMaxSize() {
        return new SettingDefinition("files.documentImage.maxSizeMb", SettingGroup.FILES, "Ảnh trong tài liệu tối đa",
                "Dung lượng tối đa mỗi ảnh trong tài liệu.", SettingType.INTEGER, 5,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 5, SettingValidator.UNIT, "MB"));
    }

    @Bean
    SettingDefinition assignmentSourceMaxSize() {
        return new SettingDefinition("files.assignmentSource.maxSizeMb", SettingGroup.FILES,
                "Tệp nguồn soạn bài tối đa", "Dung lượng tối đa mỗi tệp nguồn soạn bài AI.",
                SettingType.INTEGER, 2,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 2, SettingValidator.UNIT, "MB"));
    }
}
