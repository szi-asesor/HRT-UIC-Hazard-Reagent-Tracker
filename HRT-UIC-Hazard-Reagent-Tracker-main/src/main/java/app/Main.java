package app;

import gui.LoginFrame;
import gui.Theme;
import javax.swing.SwingUtilities;

/** Program entry point: the application always starts at the login page. */
public class Main {
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
