package model;

import gui.TeacherDashboard;

/** INHERITANCE: Teacher extends User. */
public class Teacher extends User {

    public Teacher(int id, String name, String password, String email) {
        super(id, name, password, email);
    }

    @Override
    public String getRole() {
        return "TEACHER";
    }

    /** POLYMORPHISM: same method name as Admin, but opens the Teacher screen. */
    @Override
    public void showDashboard() {
        new TeacherDashboard(this).setVisible(true);
    }
}
