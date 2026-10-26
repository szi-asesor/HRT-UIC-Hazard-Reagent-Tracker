package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import util.Format;

/**
 * ENCAPSULATION: all fields are private. The quantity can only be changed through
 * addStock() and release(), which refuse invalid amounts, so the object protects its own data.
 */
public class Chemical {
    private int id;
    private String name;
    private double quantity;
    private String unit;
    private String hazardType;          // e.g. "Corrosive,Irritant"
    private String storageGroup;        // Acid, Base, Oxidizer, Flammable, Other
    private String storageTemperature;  // e.g. "15-25°C"
    private String location;            // e.g. "Cabinet A"
    private double minimumStock;
    private LocalDate expirationDate;   // may be null
    private boolean active = true;      // false = removed (hidden) from the chemical list

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getQuantity() { return quantity; }

    public void setQuantity(double quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getHazardType() { return hazardType; }
    public void setHazardType(String hazardType) { this.hazardType = hazardType; }
    public String getStorageGroup() { return storageGroup; }
    public void setStorageGroup(String storageGroup) { this.storageGroup = storageGroup; }
    public String getStorageTemperature() { return storageTemperature; }
    public void setStorageTemperature(String storageTemperature) { this.storageTemperature = storageTemperature; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public double getMinimumStock() { return minimumStock; }
    public void setMinimumStock(double minimumStock) { this.minimumStock = minimumStock; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // ----- controlled behaviour -----

    public void addStock(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        quantity += amount;
    }

    public void release(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (amount > quantity) {
            throw new IllegalArgumentException("Insufficient inventory.");
        }
        quantity -= amount;
    }

    public boolean isLowStock() {
        return quantity <= minimumStock;
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon() {
        return expirationDate != null && !isExpired()
                && !expirationDate.isAfter(LocalDate.now().plusDays(30));
    }

    /** Text shown in the "Status" column. */
    public String getStatus() {
        List<String> flags = new ArrayList<>();
        if (isExpired()) flags.add("EXPIRED");
        else if (isExpiringSoon()) flags.add("EXPIRING SOON");
        if (isLowStock()) flags.add("LOW STOCK");
        return flags.isEmpty() ? "OK" : String.join(", ", flags);
    }

    public String getCode() {
        return String.format("C%03d", id);
    }

    @Override
    public String toString() {
        return name + " (" + Format.qty(quantity) + " " + unit + " available)";
    }
}
