package gui;

import java.awt.GridLayout;
import javax.swing.*;
import model.User;
import service.AuthenticationService;

/** Login page: the first screen. Shows POLYMORPHISM in login(). */
public class LoginFrame extends JFrame {
    private final JTextField emailField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final AuthenticationService authService = new AuthenticationService();

    public LoginFrame() {
        setTitle("Hazard & Reagent Tracker - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        JButton loginButton = new JButton("LOGIN");
        loginButton.addActionListener(e -> login());
        getRootPane().setDefaultButton(loginButton);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel title = new JLabel("Hazard & Reagent Tracker");
        title.setFont(title.getFont().deriveFont(18f));
        title.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(15));
        root.add(form);
        root.add(Box.createVerticalStrut(15));
        root.add(loginButton);

        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
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
