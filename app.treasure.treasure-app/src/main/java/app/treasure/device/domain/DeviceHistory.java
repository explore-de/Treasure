package app.treasure.device.domain;

import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class DeviceHistory extends PanacheEntity {

    @ManyToOne
    private Device device;

    @ManyToOne
    private Member actor;

    private LocalDateTime happenedAt;

    private String eventType;

    private String fieldName;

    private String oldValue;
    private String newValue;

    private String notes;

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public Member getActor() { return actor; }
    public void setActor(Member actor) { this.actor = actor; }

    public LocalDateTime getHappenedAt() { return happenedAt; }
    public void setHappenedAt(LocalDateTime happenedAt) { this.happenedAt = happenedAt; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getActorName() {
        return actor != null ? actor.getDisplayName() : "system";
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String getFormattedAt() {
        return happenedAt != null ? happenedAt.format(FMT) : "";
    }
}