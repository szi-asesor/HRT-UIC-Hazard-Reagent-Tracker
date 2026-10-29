package service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import model.ChemicalRequest;
import util.Config;

/**
 * Sends request emails through Gmail SMTP (smtp.gmail.com, port 587, STARTTLS).
 * It IMPLEMENTS NotificationService (abstraction). No email code lives in the GUI.
 * The Gmail address and App Password come from config.properties.
 */
public class EmailService implements NotificationService {

    @Override
    public void sendApproved(ChemicalRequest r) throws MessagingException {
        String body = "Your chemical request has been approved.\n\n"
                + "Request ID: " + r.getIdText() + "\n\n"
                + "Experiment:\n" + r.getExperiment() + "\n\n"
                + "Chemicals:\n" + r.getItemsText();
        send(r.getTeacherEmail(), "Chemical Request Approved", body);
    }

    @Override
    public void sendCompatibilityClarification(ChemicalRequest r, java.util.List<String> incompatiblePairs)
            throws MessagingException {
        String body = "Your chemical request needs a safety clarification before it can be reviewed.\n\n"
                + "Request ID: " + r.getIdText() + "\n\n"
                + "Experiment:\n" + r.getExperiment() + "\n\n"
                + "The request includes these storage-incompatible chemical pairs:\n - "
                + String.join("\n - ", incompatiblePairs) + "\n\n"
                + "Please reply with the reason these chemicals are being requested together and explain "
                + "how they will be handled separately. The request remains pending while this is clarified.";
        send(r.getTeacherEmail(), "Safety clarification for chemical request " + r.getIdText(), body);
    }

    private void send(String to, String subject, String body) throws MessagingException {
        final String from = Config.get("gmail.address");
        final String appPassword = Config.get("gmail.app.password");

        if (to == null || to.isBlank()) {
            throw new MessagingException("The teacher has no email address.");
        }
        if (from.isEmpty() || appPassword.isEmpty()) {
            throw new MessagingException("Gmail address / app password are not set in config.properties.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, appPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}
