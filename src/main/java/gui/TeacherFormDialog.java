package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.*;
import service.AuthenticationService;

/** Form for adding a teacher account from the admin dashboard. */
public class TeacherFormDialog extends JDialog {
    private final AuthenticationService authenticationService;
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private boolean saved;

    public TeacherFormDialog(JFrame owner, AuthenticationService authenticationService) {
        super(owner, "Add Teacher", true);
        this.authenticationService = authenticationService;

        Theme.styleInput(nameField);
        Theme.styleInput(emailField);
        Theme.styleInput(passwordField);

        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.add(new JLabel("Name"));
        form.add(nameField);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        RoundedButton saveButton = new RoundedButton("Save", RoundedButton.Kind.PRIMARY);
        RoundedButton cancelButton = new RoundedButton("Cancel", RoundedButton.Kind.DANGER);
        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(saveButton);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    private void save() {
        try {
            authenticationService.addTeacher(
                    nameField.getText(), emailField.getText(), new String(passwordField.getPassword()));
            saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot add teacher",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Cannot add teacher", JOptionPane.ERROR_MESSAGE);
        }
    }
}
