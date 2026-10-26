package edu.aiplatform.files.application;

import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.domain.SettingValidator;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nhóm cài đặt Tệp (BR-U03-87): dung lượng tối đa; loại học liệu và ảnh cố định trong {@code PurposePolicy}, chỉ tệp
 * nguồn soạn bài bằng AI còn chọn loại.
 */
@Configuration
public class FileSettingDefinitions {

    @Bean
    SettingDefinition materialMaxSize() {
        return new SettingDefinition("files.material.maxSizeMb", SettingGroup.FILES, "Học liệu tối đa",
                "Dung lượng tối đa của một tệp học liệu khi Chủ nhiệm môn hoặc giảng viên tải lên. "
                        + "Tệp lớn hơn bị từ chối trước khi lưu.",
                SettingType.INTEGER, 50,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 50, SettingValidator.UNIT, "MB"));
    }

    @Bean
    SettingDefinition imageMaxSize() {
        return new SettingDefinition("files.documentImage.maxSizeMb", SettingGroup.FILES, "Ảnh trong tài liệu tối đa",
                "Dung lượng tối đa của một ảnh chèn vào đề và bài làm", SettingType.INTEGER, 5,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 5, SettingValidator.UNIT, "MB"));
    }

    @Bean
    SettingDefinition assignmentSourceMaxSize() {
        return new SettingDefinition("files.assignmentSource.maxSizeMb", SettingGroup.FILES,
                "Tệp nguồn soạn bài tối đa", "Dung lượng tối đa mỗi tệp nguồn riêng chỉ dùng khi soạn bài bằng AI.",
                SettingType.INTEGER, 2,
                Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 2, SettingValidator.UNIT, "MB"));
    }

    @Bean
    SettingDefinition assignmentSourceTypes() {
        return new SettingDefinition("files.assignmentSource.allowedTypes", SettingGroup.FILES,
                "Loại tệp nguồn soạn bài", "Loại tệp văn bản được dùng làm nguồn khi soạn bài bằng AI.",
                SettingType.ENUM_SET, List.of("TXT", "DOCX", "PDF"),
                Map.of(SettingValidator.OPTIONS, List.of("TXT", "DOCX", "PDF"), SettingValidator.NON_EMPTY, true));
    }
}
