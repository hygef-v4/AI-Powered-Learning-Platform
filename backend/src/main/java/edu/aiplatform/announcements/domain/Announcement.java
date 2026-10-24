package edu.aiplatform.announcements.domain;

import edu.aiplatform.shared.error.ApiException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

@Entity @Table(name="announcements")
public class Announcement {
    public enum Status { ACTIVE, DELETED }
    @Id public UUID id;
    @Column(nullable=false) public UUID classId;
    @Column(nullable=false,length=200) public String title;
    @Column(nullable=false,length=5000) public String body;
    public boolean important;
    public boolean sendEmail;
    @Enumerated(EnumType.STRING) @Column(length=16) public Status status;
    public UUID createdBy;
    public Instant createdAt;
    public UUID updatedBy;
    public Instant updatedAt;
    public UUID deletedBy;
    public Instant deletedAt;
    public long version;
    public Announcement() { }
    public static Announcement create(UUID classId,UUID actor,String title,String body,boolean important,boolean email,Instant now) {
        if(email&&!important)throw ApiException.badRequest("ANNOUNCEMENT_EMAIL_INVALID","Chỉ thông báo quan trọng mới có email");
        var a=new Announcement();a.id=UUID.randomUUID();a.classId=classId;a.createdBy=actor;a.updatedBy=actor;
        a.createdAt=now;a.updatedAt=now;a.important=important;a.sendEmail=email;a.status=Status.ACTIVE;
        a.title=cleanTitle(title);a.body=cleanBody(body);return a;
    }
    public void edit(String title,String body,UUID actor,Instant now) {
        String nextTitle=cleanTitle(title),nextBody=cleanBody(body);
        this.title=nextTitle;this.body=nextBody;updatedBy=actor;updatedAt=now;
    }
    public void delete(UUID actor,Instant now) {status=Status.DELETED;deletedBy=actor;deletedAt=now;updatedBy=actor;updatedAt=now;}
    private static String bounded(String text,int max) {
        String s=text==null?"":text.strip();
        if(s.isEmpty()||s.codePointCount(0,s.length())>max)
            throw ApiException.badRequest("ANNOUNCEMENT_INVALID","Kiểm tra độ dài tiêu đề và nội dung");
        return s;
    }
    private static String cleanTitle(String title) {
        String value=bounded(title,200);
        if(!Jsoup.isValid(value,Safelist.none()))
            throw ApiException.badRequest("ANNOUNCEMENT_INVALID","Tiêu đề phải là văn bản thuần");
        return value;
    }
    private static String cleanBody(String body) {
        String value=bounded(body,5000);
        // Keep Markdown/newlines, drop tags/attributes and script/style data. Never unescape
        // the output back into HTML; the renderer also enforces its Markdown URL allowlist.
        String cleaned=Jsoup.clean(value,"",Safelist.none(),new Document.OutputSettings().prettyPrint(false)).strip();
        return bounded(cleaned,5000);
    }
}
