package edu.aiplatform.settings.contract;

import java.util.Optional;

/** Đọc giá trị cài đặt hiện hành (cache 30 giây); rỗng là chưa cấu hình. Dùng bởi U03, U08. */
public interface ISettings {

    <T> Optional<T> get(SettingKey<T> key);
}
