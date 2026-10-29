package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.User;
import service.AuthenticationService;

/** Login page: the first screen. Shows POLYMORPHISM in login(). */
public class LoginFrame extends JFrame {
    private final JTextField emailField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final AuthenticationService authService = new AuthenticationService();
    private final JPopupMenu emailSuggestions = new JPopupMenu();
    private final DefaultListModel<String> suggestionModel = new DefaultListModel<>();
    private final JList<String> suggestionList = new JList<>(suggestionModel);
    private final Timer suggestionTimer = new Timer(250, event -> loadEmailSuggestions());
    private int suggestionRequestVersion;

    public LoginFrame() {
        setTitle("Hazard & Reagent Tracker - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Theme.styleInput(emailField);
        Theme.styleInput(passwordField);
        emailSuggestions.setFocusable(false);
        suggestionList.setFont(Theme.BODY);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        emailSuggestions.add(new JScrollPane(suggestionList));
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 1) selectEmailSuggestion();
            }
        });
        emailField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateEmailSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateEmailSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateEmailSuggestions();
            }
        });
        emailField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                java.awt.Component opposite = event.getOppositeComponent();
                if (opposite == null || !SwingUtilities.isDescendingFrom(opposite, emailSuggestions)) {
                    emailSuggestions.setVisible(false);
                }
            }
        });

        JPanel form = new JPanel(new GridLayout(2, 2, 12, 12));
        form.setPreferredSize(new Dimension(520, 104));
        form.setMaximumSize(new Dimension(520, 104));
        form.setAlignmentX(CENTER_ALIGNMENT);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        RoundedButton loginButton = new RoundedButton("Login", RoundedButton.Kind.PRIMARY);
        loginButton.addActionListener(e -> login());
        getRootPane().setDefaultButton(loginButton);

        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("Hazard & Reagent Tracker");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(CENTER_ALIGNMENT);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        title.setAlignmentX(CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(24));
        content.add(form);
        content.add(Box.createVerticalStrut(24));
        JPanel buttonRow = new JPanel();
        buttonRow.add(loginButton);
        content.add(buttonRow);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
        setSize(new Dimension(600, 360));
        setLocationRelativeTo(null);
    }

    private void updateEmailSuggestions() {
        suggestionRequestVersion++;
        if (!emailField.getText().isBlank()) suggestionTimer.restart();
        else {
            suggestionTimer.stop();
            suggestionModel.clear();
            emailSuggestions.setVisible(false);
        }
    }

    private void loadEmailSuggestions() {
        String input = emailField.getText().trim();
        int version = suggestionRequestVersion;
        if (input.isEmpty() || !emailField.isFocusOwner()) return;
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return authService.suggestEmails(input);
            }

            @Override
            protected void done() {
                if (version != suggestionRequestVersion || !emailField.isFocusOwner()
                        || !input.equals(emailField.getText().trim())) return;
                try {
                    List<String> matches = new ArrayList<>();
                    for (String email : get()) {
                        if (!email.equalsIgnoreCase(input)) matches.add(email);
                    }
                    showEmailSuggestions(matches);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ignored) {
                    emailSuggestions.setVisible(false);
                }
            }
        }.execute();
    }

    private void showEmailSuggestions(List<String> matches) {
        int shared = 0;
        while (shared < suggestionModel.size() && shared < matches.size()
                && suggestionModel.get(shared).equals(matches.get(shared))) shared++;
        int replaceCount = Math.min(suggestionModel.size(), matches.size());
        for (int i = shared; i < replaceCount; i++) suggestionModel.set(i, matches.get(i));
        while (suggestionModel.size() > matches.size()) suggestionModel.remove(suggestionModel.size() - 1);
        for (int i = suggestionModel.size(); i < matches.size(); i++) suggestionModel.addElement(matches.get(i));
        if (!matches.isEmpty()) {
            if (!emailSuggestions.isVisible()) {
                int textWidth = suggestionList.getFontMetrics(suggestionList.getFont())
                        .stringWidth(matches.get(0)) + 36;
                emailSuggestions.setPopupSize(Math.max(emailField.getWidth(), Math.min(520, textWidth)),
                        Math.min(8, matches.size()) * 32 + 4);
                emailSuggestions.show(emailField, 0, emailField.getHeight());
            }
        } else {
            emailSuggestions.setVisible(false);
        }
    }

    private void selectEmailSuggestion() {
        String suggestion = suggestionList.getSelectedValue();
        if (suggestion == null) return;
        emailField.setText(suggestion);
        emailField.setCaretPosition(suggestion.length());
        emailSuggestions.setVisible(false);
        emailField.requestFocusInWindow();
    }

    private void login() {
        try {
            String password = new String(passwordField.getPassword());
            User user = authService.login(emailField.getText(), password);

            // POLYMORPHISM: 'user' may be an Admin or a Teacher object.
            // The same call showDashboard() opens a different screen for each one.
            dispose();
            user.showDashboard();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
