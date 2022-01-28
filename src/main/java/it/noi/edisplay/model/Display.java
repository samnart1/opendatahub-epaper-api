package it.noi.edisplay.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Entity class for Displays that contains all needed information for an
 * E-Display
 *
 * @Author Simon Dalvai
 */
@Entity
@Table(name = "displays")
public class Display {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

//	@NotNull
    private String name;

//	@NotNull
    private String uuid;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    // Saves timestamp when logical display gets updated
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdate;

    // Saves timestamp when real physical display gets updated
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRealDisplayUpdate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastState;

    private String errorMessage;

//	@NotNull
    @ManyToOne
    private Resolution resolution;

    @ManyToOne
    private Template template;

    @ManyToOne
    private Location location;

    @OneToOne(mappedBy = "display", cascade=CascadeType.ALL)
    private DisplayContent displayContent;
    
    @OneToMany(mappedBy="display", fetch=FetchType.LAZY)
    private List<ScheduledContent> scheduledContent;

    private int batteryPercentage;
    
    private boolean ignoreScheduledContent;

    public Display() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getLastState() {
        return lastState;
    }

    public void setLastState(Date lastState) {
        this.lastState = lastState;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(int batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public Date getLastRealDisplayUpdate() {
        return lastRealDisplayUpdate;
    }

    public void setLastRealDisplayUpdate(Date lastRealDisplayUpdate) {
        this.lastRealDisplayUpdate = lastRealDisplayUpdate;
    }

    @PrePersist
    public void prePersist() {
        this.setUuid(UUID.randomUUID().toString());
        if (lastState == null)
            lastState = new Date();
    }

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public DisplayContent getDisplayContent() {
        return displayContent;
    }

    public void setDisplayContent(DisplayContent displayContent) {
        this.displayContent = displayContent;
    }

    public List<ScheduledContent> getScheduledContent() {
        return scheduledContent;
    }

    public void setScheduledContent(List<ScheduledContent> scheduledContent) {
        this.scheduledContent = scheduledContent;
    }

    public boolean isIgnoreScheduledContent() {
        return ignoreScheduledContent;
    }

    public void setIgnoreScheduledContent(boolean ignoreScheduledContent) {
        this.ignoreScheduledContent = ignoreScheduledContent;
    }
}
