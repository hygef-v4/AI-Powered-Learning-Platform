package edu.aiplatform.notifications.model;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
@Entity @Table(name="notification_preferences") @IdClass(NotificationPreference.Key.class)
public class NotificationPreference {
    @Id public UUID accountId;
    @Id @Column(length=40) public String type;
    public boolean emailEnabled;
    public NotificationPreference() { }
    public static class Key implements Serializable {
        public UUID accountId;public String type;
        public Key() { }
        public boolean equals(Object o) {return o instanceof Key k&&java.util.Objects.equals(accountId,k.accountId)&&java.util.Objects.equals(type,k.type);}
        public int hashCode() {return java.util.Objects.hash(accountId,type);}
    }
}
