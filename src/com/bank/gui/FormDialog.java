package com.bank.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small reusable modal pop-up form.
 *
 * Built in the Playground Swing style (JDialog with a GridBag layout
 * and anonymous ActionListeners). Callers add labelled fields, show the
 * dialog, and read the typed values back after it closes.
 *
 * Example:
 * <pre>
 *   FormDialog form = new FormDialog(parent, "Deposit");
 *   JTextField account = form.addTextField("Account number:");
 *   JTextField amount  = form.addTextField("Amount:");
 *   if (form.showDialog()) {
 *       // use account.getText(), amount.getText()
 *   }
 * </pre>
 */
public class FormDialog extends JDialog {

    private final JPanel fieldsPanel;
    private final GridBagConstraints gbc;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private boolean confirmed = false;
    private JComponent firstField;

    public FormDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        add(fieldsPanel, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
    }

    // ---------- builders ----------

    /** Adds a labelled single-line text field and returns it. */
    public JTextField addTextField(String label) {
        JTextField field = new JTextField(18);
        addRow(label, field);
        return field;
    }

    /** Adds a labelled drop-down and returns the combo box. */
    public JComboBox<String> addComboBox(String label, String[] options) {
        JComboBox<String> combo = new JComboBox<>(options);
        addRow(label, combo);
        return combo;
    }

    private void addRow(String label, JComponent field) {
        int row = fields.size();

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        fieldsPanel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        fieldsPanel.add(field, gbc);

        fields.put(label, field);
        if (firstField == null) {
            firstField = field;
        }
    }

    private JComponent buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        bar.add(ok);
        bar.add(cancel);
        getRootPane().setDefaultButton(ok); // Enter confirms
        return bar;
    }

    // ---------- show / read ----------

    /** Displays the dialog modally; returns true if the user pressed OK. */
    public boolean showDialog() {
        pack();
        setLocationRelativeTo(getOwner());
        if (firstField != null) {
            firstField.requestFocusInWindow();
        }
        setVisible(true);
        return confirmed;
    }
}
