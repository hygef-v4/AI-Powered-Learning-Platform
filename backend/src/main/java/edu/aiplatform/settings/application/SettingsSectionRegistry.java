package edu.aiplatform.settings.application;

import edu.aiplatform.settings.contract.ISettingsSectionProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Phần cài đặt ủy quyền (P15): mỗi {@code summary()} có timeout 1 giây, lỗi chỉ ảnh hưởng phần đó (BR-U03-89). */
@Component
public class SettingsSectionRegistry {

    static final Duration SUMMARY_TIMEOUT = Duration.ofSeconds(1);

    private final List<ISettingsSectionProvider> providers;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "settings-section");
        thread.setDaemon(true);
        return thread;
    });

    public SettingsSectionRegistry(List<ISettingsSectionProvider> providers) {
        this.providers = providers;
    }

    public record SectionView(String sectionKey, String title, String summary, boolean available) {
    }

    public List<SectionView> summaries() {
        List<SectionView> views = new ArrayList<>();
        for (ISettingsSectionProvider provider : providers) {
            String key;
            String title;
            try {
                key = provider.sectionKey();
                title = provider.title();
            } catch (RuntimeException e) {
                continue;
            }
            Future<String> summary = executor.submit(provider::summary);
            try {
                views.add(new SectionView(key, title, summary.get(SUMMARY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS),
                        true));
            } catch (Exception e) {
                summary.cancel(true);
                views.add(new SectionView(key, title, null, false));
            }
        }
        return views;
    }
}
