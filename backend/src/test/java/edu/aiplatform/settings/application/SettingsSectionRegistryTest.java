package edu.aiplatform.settings.application;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.settings.contract.ISettingsSectionProvider;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SettingsSectionRegistryTest {

    static ISettingsSectionProvider provider(String key, Supplier<String> summary) {
        return new ISettingsSectionProvider() {
            @Override
            public String sectionKey() {
                return key;
            }

            @Override
            public String title() {
                return "Phần " + key;
            }

            @Override
            public String summary() {
                return summary.get();
            }
        };
    }

    @Test
    void failingOrSlowSectionIsUnavailableWithoutBreakingTheOthers() {
        SettingsSectionRegistry registry = new SettingsSectionRegistry(List.of(
                provider("ok", () -> "Gemini ****1234"),
                provider("broken", () -> {
                    throw new IllegalStateException("U06 lỗi");
                }),
                provider("slow", () -> {
                    try {
                        Thread.sleep(3_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "muộn";
                })));

        List<SettingsSectionRegistry.SectionView> views = registry.summaries();

        assertThat(views).extracting(SettingsSectionRegistry.SectionView::available).containsExactly(true, false, false);
        assertThat(views.get(0).summary()).isEqualTo("Gemini ****1234");
        assertThat(views.get(1).summary()).isNull();
    }
}
