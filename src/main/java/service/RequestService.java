package service;

import dao.ChemicalDAO;
import dao.DatabaseConnection;
import dao.RequestDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.Chemical;
import model.ChemicalRequest;
import model.RequestItem;
import model.Teacher;

/**
 * The request workflow.
 * Teacher: create, edit and cancel (only while PENDING).
 * Admin: change the status (approve / reject / pending / cancelled).
 * Rule: stock is released from inventory exactly when a request is APPROVED.
 */
public class RequestService {
    private final RequestDAO requestDAO = new RequestDAO();
    private final ChemicalDAO chemicalDAO = new ChemicalDAO();
    private final InventoryService inventoryService = new InventoryService();
    private final NotificationService notifier = new EmailService();   // abstraction in use

    // ---------------- teacher actions ----------------

    public void createRequest(Teacher teacher, String className, String experiment,
                              List<RequestItem> items) throws SQLException {
        checkFields(teacher, className, experiment, items);
        validateChemicals(items);
        ChemicalRequest request = new ChemicalRequest();
        request.setTeacherId(teacher.getId());
        request.setClassName(className.trim());
        request.setExperiment(experiment.trim());
        for (RequestItem item : items) {
            request.addItem(item);
        }
        requestDAO.insert(request);   // status starts as PENDING
    }

    /** A teacher may edit only their OWN request, and only while it is PENDING. */
    public void updateRequest(Teacher teacher, int requestId, String className, String experiment,
                              List<RequestItem> items) throws SQLException {
        checkFields(teacher, className, experiment, items);
        validateChemicals(items);
        ChemicalRequest request = loadOwnPending(teacher, requestId, "edited");
        request.setClassName(className.trim());
        request.setExperiment(experiment.trim());
        request.getItems().clear();
        for (RequestItem item : items) {
            request.addItem(item);
        }
        requestDAO.update(request);
    }

    private void validateChemicals(List<RequestItem> items) throws SQLException {
        for (RequestItem item : items) {
            Chemical chemical = chemicalDAO.findActiveById(item.getChemicalId());
            if (chemical == null) {
                throw new IllegalArgumentException(
                        "Chemical " + item.getChemicalId() + " is unavailable or inactive.");
            }
            if (chemical.isExpired()) {
                throw new IllegalArgumentException(chemical.getName() + " is expired and cannot be requested.");
            }
        }
    }

    /** A teacher may cancel only their OWN request, and only while it is PENDING. */
    public void cancelRequest(Teacher teacher, int requestId) throws SQLException {
        loadOwnPending(teacher, requestId, "cancelled");
        if (!requestDAO.cancelIfPending(requestId)) {
            throw new IllegalArgumentException("This request is no longer pending, so it cannot be cancelled.");
        }
    }

    private ChemicalRequest loadOwnPending(Teacher teacher, int requestId, String verb) throws SQLException {
        ChemicalRequest request = requestDAO.findById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found.");
        }
        if (request.getTeacherId() != teacher.getId()) {
            throw new IllegalArgumentException("You can only change your own requests.");
        }
        if (!request.isPending()) {
            throw new IllegalArgumentException("Only pending requests can be " + verb + ".");
        }
        return request;
    }

    private void checkFields(Teacher teacher, String className, String experiment, List<RequestItem> items) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class / section is required.");
        }
        if (experiment == null || experiment.trim().isEmpty()) {
            throw new IllegalArgumentException("Purpose / experiment is required.");
        }
        if (teacher.getEmail() == null || teacher.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Teacher email must not be empty.");
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A request must contain at least one chemical.");
        }
    }

    // ---------------- reading ----------------

    public List<ChemicalRequest> getAllRequests() throws SQLException {
        return requestDAO.findAll();
    }

    public List<ChemicalRequest> getRequestsForTeacher(int teacherId) throws SQLException {
        return requestDAO.findByTeacher(teacherId);
    }

    public int count(List<ChemicalRequest> list, String status) {
        int n = 0;
        for (ChemicalRequest r : list) {
            if (r.getStatus().equals(status)) n++;
        }
        return n;
    }

    // ---------------- admin actions ----------------

    public String approve(int requestId, int adminId) throws SQLException {
        return changeStatus(requestId, ChemicalRequest.APPROVED, null, adminId);
    }

    public String reject(int requestId, String reason, int adminId) throws SQLException {
        return changeStatus(requestId, ChemicalRequest.REJECTED, reason, adminId);
    }

    /**
     * Changes a request to any status.
     *  - becoming APPROVED  -> chemicals are released (fails with "Insufficient inventory" if short)
     *  - leaving APPROVED   -> the released chemicals are returned (ADJUSTMENT transactions)
     *  - all stock changes and the status change happen together (all or nothing)
     * The teacher is emailed when the new status is APPROVED or REJECTED.
     * Returns null on success, or a warning text if only the email failed.
     */
    public String changeStatus(int requestId, String newStatus, String reason, int adminId) throws SQLException {
        if (!newStatus.equals(ChemicalRequest.PENDING) && !newStatus.equals(ChemicalRequest.APPROVED)
                && !newStatus.equals(ChemicalRequest.REJECTED) && !newStatus.equals(ChemicalRequest.CANCELLED)) {
            throw new IllegalArgumentException("Unknown status.");
        }
        if (newStatus.equals(ChemicalRequest.REJECTED) && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("Please enter a rejection reason.");
        }
        ChemicalRequest request = requestDAO.findById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found.");
        }
        String oldStatus = request.getStatus();
        if (oldStatus.equals(newStatus)) {
            throw new IllegalArgumentException("The request is already " + oldStatus + ".");
        }

        boolean releasing = !oldStatus.equals(ChemicalRequest.APPROVED) && newStatus.equals(ChemicalRequest.APPROVED);
        boolean returning = oldStatus.equals(ChemicalRequest.APPROVED) && !newStatus.equals(ChemicalRequest.APPROVED);
        String savedReason = newStatus.equals(ChemicalRequest.REJECTED) ? reason.trim() : null;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                for (RequestItem item : request.getItems()) {
                    if (releasing || returning) {
                        if (releasing) {
                            Chemical c = chemicalDAO.findActiveById(con, item.getChemicalId());
                            if (c == null) {
                                throw new IllegalArgumentException(
                                        "Chemical " + item.getChemicalId() + " is unavailable or inactive.");
                            }
                            if (c.isExpired()) {
                                throw new IllegalArgumentException(
                                        c.getName() + " is expired and cannot be released.");
                            }
                            inventoryService.release(con, c, item.getQuantity(), adminId);
                        } else {
                            Chemical c = chemicalDAO.findById(con, item.getChemicalId());
                            if (c == null) {
                                throw new IllegalArgumentException(
                                        "Chemical " + item.getChemicalId() + " could not be found for stock return.");
                            }
                            inventoryService.returnStock(con, c, item.getQuantity(), adminId);
                        }
                    }
                }
                requestDAO.updateStatus(con, requestId, newStatus, savedReason, oldStatus);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();   // nothing changes if any chemical is short
                throw e;
            }
        }

        request.setStatus(newStatus);
        request.setRejectionReason(savedReason);
        try {
            if (newStatus.equals(ChemicalRequest.APPROVED)) {
                notifier.sendApproved(request);
            } else if (newStatus.equals(ChemicalRequest.REJECTED)) {
                notifier.sendRejected(request);
            }
            return null;
        } catch (Exception e) {
            return "Status changed to " + newStatus + " (inventory updated), but the email could not be sent:\n"
                    + e.getMessage();
        }
    }
}
