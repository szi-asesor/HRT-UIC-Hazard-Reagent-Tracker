package model;

import util.Format;

/** One line of a request: a chemical and how much of it the teacher wants. */
public class RequestItem {
    private int chemicalId;
    private String chemicalName;
    private String unit;
    private double quantity;

    public RequestItem(int chemicalId, String chemicalName, String unit, double quantity) {
        this.chemicalId = chemicalId;
        this.chemicalName = chemicalName;
        this.unit = unit;
        this.quantity = quantity;
    }

    public int getChemicalId() { return chemicalId; }
    public String getChemicalName() { return chemicalName; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }

    public void setQuantity(double quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return chemicalName + " — " + Format.qty(quantity) + " " + unit;
    }
}
