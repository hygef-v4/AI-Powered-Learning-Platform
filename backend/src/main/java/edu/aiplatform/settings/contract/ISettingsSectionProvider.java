package edu.aiplatform.settings.contract;

/**
 * U03 khai báo, U06 cài: phần cài đặt do unit khác lưu (API AI chung, quota giảng viên) trên Setting List.
 * {@code summary()} chỉ trả bản tóm tắt đã che.
 */
public interface ISettingsSectionProvider {

    String sectionKey();

    String title();

    String summary();
}
