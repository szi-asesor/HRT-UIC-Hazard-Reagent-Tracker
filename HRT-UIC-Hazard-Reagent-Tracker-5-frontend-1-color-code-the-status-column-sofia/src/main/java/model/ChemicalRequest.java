package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** ENCAPSULATION: a request owns its list of items; outsiders add items through addItem(). */
public class ChemicalRequest {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private int id;
    private int teacherId;
    private String teacherName;
    private String teacherEmail;
    private String className;
    private String experiment;
    private String status = PENDING;
    private String rejectionReason;
    private LocalDateTime requestDate;
    private List<RequestItem> items = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getTeacherEmail() { return teacherEmail; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getExperiment() { return experiment; }
    public void setExperiment(String experiment) { this.experiment = experiment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public List<RequestItem> getItems() { return items; }

    public void addItem(RequestItem item) {
        items.add(item);
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public String getIdText() {
        return String.format("%03d", id);
    }

    /** "Hydrochloric Acid — 500 mL" on each line. */
    public String getItemsText() {
        StringBuilder sb = new StringBuilder();
        for (RequestItem item : items) {
            sb.append(item).append("\n");
        }
        return sb.toString();
    }

    public String getDetailsText() {
        String text = "Request ID: " + getIdText()
                + "\nTeacher: " + teacherName
                + "\nClass: " + className
                + "\nExperiment: " + experiment
                + "\nStatus: " + status;
        if (REJECTED.equals(status) && rejectionReason != null) {
            text += "\nReason: " + rejectionReason;
        }
        return text + "\n\nChemicals:\n" + getItemsText();
    }
}
