package edu.aiplatform.announcements.application;
import java.time.Instant;
import java.util.*;
public final class AnnouncementViews {
    private AnnouncementViews() { }
    public record Item(UUID id,UUID classId,String className,String title,String body,boolean important,boolean sendEmail,
                       UUID createdBy,String authorName,Instant createdAt,Instant updatedAt,long version,boolean canManage) { }
    public record ClassOption(UUID classId,String code,String name,boolean canManage,int studentCount) { }
    public record Feed(List<Item> items,List<ClassOption> classes,int page,int size,boolean hasMore) { }
}
