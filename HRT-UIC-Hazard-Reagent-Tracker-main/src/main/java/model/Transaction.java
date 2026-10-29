package model;

import java.time.LocalDateTime;

/** A record of one inventory change (ADD, RELEASE or ADJUSTMENT). */
public class Transaction {
    private int id;
    private int chemicalId;
    private String chemicalName;
    private int userId;
    private String userName;
    private String type;
    private double quantity;
    private LocalDateTime date;

    public Transaction() { }

    public Transaction(int chemicalId, int userId, String type, double quantity) {
        this.chemicalId = chemicalId;
        this.userId = userId;
        this.type = type;
        this.quantity = quantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getChemicalId() { return chemicalId; }
    public String getChemicalName() { return chemicalName; }
    public void setChemicalName(String chemicalName) { this.chemicalName = chemicalName; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getType() { return type; }
    public double getQuantity() { return quantity; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
