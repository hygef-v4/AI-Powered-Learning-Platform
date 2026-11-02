package edu.aiplatform.notifications.domain;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="deadline_reminders") @IdClass(DeadlineReminder.Key.class)
public class DeadlineReminder {
    @Id public UUID assignmentId;
    @Id public UUID classId;
    @Id public Instant closesAt;
    @Column(length=16) public String status;
    public Instant processedAt;
    public DeadlineReminder() { }
    public static class Key implements Serializable {
        public UUID assignmentId;public UUID classId;public Instant closesAt;
        public Key() { }
        public boolean equals(Object o) {return o instanceof Key k&&java.util.Objects.equals(assignmentId,k.assignmentId)&&java.util.Objects.equals(classId,k.classId)&&java.util.Objects.equals(closesAt,k.closesAt);}
        public int hashCode() {return java.util.Objects.hash(assignmentId,classId,closesAt);}
    }
}
