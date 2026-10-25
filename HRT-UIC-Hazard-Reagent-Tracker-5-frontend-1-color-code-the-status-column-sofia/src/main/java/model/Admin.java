package model;

import gui.AdminDashboard;

/** INHERITANCE: Admin extends User (gets id, name, email... for free). */
public class Admin extends User {

    public Admin(int id, String name, String password, String email) {
        super(id, name, password, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    /** POLYMORPHISM: same method name as Teacher, different behaviour. */
    @Override
    public void showDashboard() {
        new AdminDashboard(this).setVisible(true);
    }
}
