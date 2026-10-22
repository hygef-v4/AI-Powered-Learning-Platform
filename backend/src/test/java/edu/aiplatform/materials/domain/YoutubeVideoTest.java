package edu.aiplatform.materials.domain;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;

class YoutubeVideoTest {
    @ParameterizedTest @ValueSource(strings={"https://youtu.be/abcdefghijk","https://youtube.com/watch?v=abcdefghijk",
            "https://www.youtube.com/watch?v=abcdefghijk&t=10"})
    void acceptsSingleVideo(String url) {assertThat(YoutubeVideo.parse(url).url()).isEqualTo("https://www.youtube.com/watch?v=abcdefghijk");}
    @ParameterizedTest @ValueSource(strings={"http://youtu.be/abcdefghijk","https://youtube.com/playlist?list=x",
            "https://youtube.com/watch?v=abcdefghijk&list=x","https://youtu.be/abcdefghijk?list=x",
            "https://youtube.com.evil.test/watch?v=abcdefghijk","javascript:alert(1)","https://user@youtube.com/watch?v=abcdefghijk",
            "https://youtu.be/short","https://youtube.com/watch?v=abcdefghijk&v=abcdefghijk","https://youtu.be/abcdefghijk/extra",
            "https://youtube.com:8443/watch?v=abcdefghijk","https://youtube.com/watch?v=abcdefghijk&%6cist=x",""})
    void rejectsUnsafeOrPlaylistUrls(String url) {assertThatIllegalArgumentException().isThrownBy(()->YoutubeVideo.parse(url));}
    @Test void revisionPreservesIdentityAndOldSnapshot() {
        UUID id=UUID.randomUUID(),subject=UUID.randomUUID(),actor=UUID.randomUUID();Instant now=Instant.now();
        var m=new Material(id,Material.Scope.SUBJECT,subject,null,Material.Kind.FILE," title ",null,0,Material.Status.ACTIVE,1,actor,now,now,0);
        var next=m.revise("updated","description",1,2,Material.Status.ARCHIVED,now.plusSeconds(1));
        assertThat(m.title()).isEqualTo("title");assertThat(m.currentVersionNo()).isEqualTo(1);
        assertThat(next.id()).isEqualTo(id);assertThat(next.version()).isEqualTo(1);assertThat(next.currentVersionNo()).isEqualTo(2);
    }
    @Test void rejectsMissingScopeAndOversizedMetadata() {
        UUID id=UUID.randomUUID();Instant now=Instant.now();
        assertThatIllegalArgumentException().isThrownBy(()->new Material(id,Material.Scope.CLASS,id,null,Material.Kind.FILE,
                "title","",0,Material.Status.ACTIVE,1,id,now,now,0));
        assertThatIllegalArgumentException().isThrownBy(()->new Material(id,Material.Scope.SUBJECT,id,null,Material.Kind.FILE,
                "x".repeat(201),"",0,Material.Status.ACTIVE,1,id,now,now,0));
    }
}
