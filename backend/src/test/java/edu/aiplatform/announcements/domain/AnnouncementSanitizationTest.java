package edu.aiplatform.announcements.domain;

import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnnouncementSanitizationTest {
    Announcement create(String body) {
        return Announcement.create(UUID.randomUUID(), UUID.randomUUID(), "Thông báo", body, false, false, Instant.now());
    }
    @Test void stripsScriptAndAttributesButRetainsMarkdownAndLineBreaks() {
        var body = "**đậm** · [link](https://example.com)\n\n<script>alert(1)</script><b onclick='attack()'>text</b>";
        assertThat(create(body).body).isEqualTo("**đậm** · [link](https://example.com)\n\ntext");
    }
    @Test void editUsesTheSamePolicyAsCreate() {
        var a = create("Ban đầu");
        a.edit("Tên mới", "[link](https://example.com)<img src=x onerror=alert(1)>", UUID.randomUUID(), Instant.now());
        assertThat(a.body).isEqualTo("[link](https://example.com)");
    }
    @Test void boundsApplyBeforeSanitizationAndEmptySanitizedContentIsRejected() {
        assertThat(create("a".repeat(5000)).body).hasSize(5000);
        for (var body : new String[] {"a".repeat(5001), "<script>" + "a".repeat(5000) + "</script>",
                "<script>alert(1)</script>", "<img src=x onerror=alert(1)>", "<style>body{color:red}</style>"}) {
            assertThatThrownBy(() -> create(body)).isInstanceOf(ApiException.class);
        }
    }
    @Test void stripsNestedForeignContentAndDoesNotUnescapeHtmlBackIntoMarkup() {
        var a = create("Tin <svg><script>alert(1)</script><a onload=alert(1)>x</a></svg> &lt;script&gt;");
        assertThat(a.body).doesNotContain("<", "alert", "onload").contains("Tin", "&lt;script&gt;");
    }
    @Test void titleRemainsPlainAndInvalidEditDoesNotPartiallyChangeTheAnnouncement() {
        var actor=UUID.randomUUID();var now=Instant.now();
        assertThat(Announcement.create(UUID.randomUUID(),actor,"T".repeat(200),"Nội dung",false,false,now).title).hasSize(200);
        for(var title:new String[]{"T".repeat(201),"<b>Tên</b>","<script>alert(1)</script>"})
            assertThatThrownBy(()->Announcement.create(UUID.randomUUID(),actor,title,"Tin",false,false,now)).isInstanceOf(ApiException.class);
        var a=create("Ban đầu");
        assertThatThrownBy(()->a.edit("Tên mới","<script>alert(1)</script>",actor,now)).isInstanceOf(ApiException.class);
        assertThat(a.title).isEqualTo("Thông báo");assertThat(a.body).isEqualTo("Ban đầu");
    }
}
