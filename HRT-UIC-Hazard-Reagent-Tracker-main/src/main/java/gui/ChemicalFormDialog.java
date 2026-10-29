package gui;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import model.Chemical;
import service.ChemicalService;
import util.Format;

/** Add / Edit form for one chemical (hazards, storage and expiry included). */
public class ChemicalFormDialog extends JDialog {
    private static final String[] HAZARDS = {"Flammable", "Corrosive", "Toxic", "Irritant", "Oxidizer",
            "Cyanide", "Organic Material"};
    private static final String[] STORAGE_LOCATIONS = {"Flammable", "Acid Corrosive", "Base Corrosive",
            "Oxidizer", "Others", "None"};

    private final Chemical existing;
    private final int userId;
    private final ChemicalService service;
    private boolean saved = false;

    private final JTextField nameField = new JTextField(20);
    private final JTextField quantityField = new JTextField(20);
    private final JComboBox<String> unitBox = new JComboBox<>(new String[]{"mL", "L", "g", "kg"});
    private final JCheckBox[] hazardBoxes = new JCheckBox[HAZARDS.length];
    private final JTextField temperatureField = new JTextField("15-25°C", 20);
    private final JComboBox<String> locationBox = new JComboBox<>(STORAGE_LOCATIONS);
    private final JTextField minStockField = new JTextField("0", 20);
    private final JTextField expiryField = new JTextField(20);

    public ChemicalFormDialog(JFrame owner, Chemical existing, int userId, ChemicalService service) {
        super(owner, existing == null ? "Add Chemical" : "Edit Chemical", true);
        this.existing = existing;
        this.userId = userId;
        this.service = service;

        Theme.styleInput(nameField);
        Theme.styleInput(quantityField);
        Theme.styleInput(unitBox);
        Theme.styleInput(temperatureField);
        Theme.styleInput(locationBox);
        Theme.styleInput(minStockField);
        Theme.styleInput(expiryField);
        locationBox.setSelectedIndex(-1);

        JPanel hazardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        for (int i = 0; i < HAZARDS.length; i++) {
            hazardBoxes[i] = new JCheckBox(HAZARDS[i]);
            hazardPanel.add(hazardBoxes[i]);
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Name"));                      form.add(nameField);
        form.add(new JLabel("Quantity"));                  form.add(quantityField);
        form.add(new JLabel("Unit"));                      form.add(unitBox);
        form.add(new JLabel("Hazards (none = None)"));     form.add(hazardPanel);
        form.add(new JLabel("Storage temperature"));       form.add(temperatureField);
        form.add(new JLabel("Storage location"));          form.add(locationBox);
        form.add(new JLabel("Minimum stock"));             form.add(minStockField);
        form.add(new JLabel("Expiration (yyyy-MM-dd)"));   form.add(expiryField);

        if (existing != null) {
            fillForm();
        }

        RoundedButton saveBtn = new RoundedButton("Save", RoundedButton.Kind.PRIMARY);
        RoundedButton cancelBtn = new RoundedButton("Cancel", RoundedButton.Kind.DANGER);
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    private void fillForm() {
        nameField.setText(existing.getName());
        quantityField.setText(Format.qty(existing.getQuantity()));
        quantityField.setEnabled(false);   // stock is changed only on the Inventory screen
        unitBox.setSelectedItem(existing.getUnit());
        for (JCheckBox box : hazardBoxes) {
            box.setSelected(existing.getHazardType().contains(box.getText()));
        }
        temperatureField.setText(existing.getStorageTemperature());
        locationBox.setSelectedItem(ChemicalService.canonicalStorageLocation(
                existing.getLocation(), existing.getStorageGroup()));
        minStockField.setText(Format.qty(existing.getMinimumStock()));
        if (existing.getExpirationDate() != null) {
            expiryField.setText(existing.getExpirationDate().toString());
        }
    }

    private void save() {
        try {
            Chemical c = new Chemical();
            c.setName(nameField.getText().trim());
            c.setUnit((String) unitBox.getSelectedItem());
            String selectedLocation = (String) locationBox.getSelectedItem();
            if (selectedLocation == null) throw new IllegalArgumentException("Select a storage location.");
            c.setStorageGroup(storageGroupFor(selectedLocation));
            c.setStorageTemperature(temperatureField.getText().trim());
            c.setLocation((String) locationBox.getSelectedItem());
            c.setMinimumStock(parseNumber(minStockField.getText(), "Minimum stock"));
            c.setHazardType(readHazards());
            c.setExpirationDate(readDate());
            if (existing == null) {
                c.setQuantity(parseNumber(quantityField.getText(), "Quantity"));
            } else {
                c.setId(existing.getId());
                c.setQuantity(existing.getQuantity());
            }

            service.validate(c, existing == null);

            // simple compatibility check against chemicals in the same location
            String warning = service.checkCompatibility(c);
            if (warning != null) {
                int answer = JOptionPane.showConfirmDialog(this, warning + "\nSave anyway?",
                        "Compatibility warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            if (existing == null) {
                service.add(c, userId);
            } else {
                service.update(c);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot save", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double parseNumber(String text, String fieldName) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private String readHazards() {
        StringBuilder sb = new StringBuilder();
        for (JCheckBox box : hazardBoxes) {
            if (box.isSelected()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(box.getText());
            }
        }
        return sb.length() == 0 ? "None" : sb.toString();
    }

    private String storageGroupFor(String location) {
        return switch (location) {
            case "Acid Corrosive" -> "Acid";
            case "Base Corrosive" -> "Base";
            case "Others" -> "Other";
            default -> location;
        };
    }

    private LocalDate readDate() {
        String text = expiryField.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Expiration date must look like 2027-12-31.");
        }
    }
}
