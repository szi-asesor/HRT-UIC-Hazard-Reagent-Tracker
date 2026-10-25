package model;

/**
 * ABSTRACTION + ENCAPSULATION
 * User is abstract: you can never create a plain "User", only an Admin or a Teacher.
 * It holds what both share and DECLARES what each child must do (the abstract methods).
 * Fields are private; other classes must use the getters/setters.
 */
public abstract class User {
    private int id;
    private String name;
    private String password;   // stored as a SHA-256 hash
    private String email;

    public User(int id, String name, String password, String email) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** Each child class decides which role it is. */
    public abstract String getRole();

    /** Each child class decides which dashboard to open. */
    public abstract void showDashboard();
}
