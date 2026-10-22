package edu.aiplatform.announcements.domain;

import edu.aiplatform.shared.error.ApiException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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
        a.title=clean(title,200);a.body=clean(body,5000);return a;
    }
    public void edit(String title,String body,UUID actor,Instant now) {
        this.title=clean(title,200);this.body=clean(body,5000);updatedBy=actor;updatedAt=now;
    }
    public void delete(UUID actor,Instant now) {status=Status.DELETED;deletedBy=actor;deletedAt=now;updatedBy=actor;updatedAt=now;}
    private static String clean(String text,int max) {
        String s=text==null?"":text.strip();
        if(s.isEmpty()||s.codePointCount(0,s.length())>max||s.matches("(?s).*<\\s*/?\\s*[A-Za-z][^>]*>.*"))
            throw ApiException.badRequest("ANNOUNCEMENT_INVALID","Kiểm tra độ dài nội dung, không nhập HTML thô");
        return s;
    }
}
