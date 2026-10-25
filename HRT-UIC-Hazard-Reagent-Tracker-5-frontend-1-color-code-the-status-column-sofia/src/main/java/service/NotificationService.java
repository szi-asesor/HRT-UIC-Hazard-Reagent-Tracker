package service;

import model.ChemicalRequest;

/**
 * ABSTRACTION: this interface says WHAT a notification service can do,
 * without saying HOW (Gmail, SMS, etc.). RequestService only knows this interface.
 */
public interface NotificationService {
    void sendApproved(ChemicalRequest request) throws Exception;

    void sendRejected(ChemicalRequest request) throws Exception;
}
