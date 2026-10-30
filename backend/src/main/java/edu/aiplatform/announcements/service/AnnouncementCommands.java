package edu.aiplatform.announcements.service;
import jakarta.validation.constraints.*;
public final class AnnouncementCommands {
    private AnnouncementCommands() { }
    public record Create(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=5000) String body,boolean important,boolean sendEmail) { }
    public record Edit(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=5000) String body,@PositiveOrZero long version) { }
}
