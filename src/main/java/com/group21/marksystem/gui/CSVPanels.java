package com.group21.marksystem.gui;

import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.ReportService;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * Panel for CSV import/export operations.
 *
 * @author Group 21
 */
public class CSVPanels extends JPanel {
    private CSVService csvService;
    private ReportService reportService;

    public CSVPanels(CSVService csvService, ReportService reportService) {
        this.csvService = csvService;
        this.reportService = reportService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridLayout(2, 1, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Import panel
        JPanel importPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        importPanel.setBorder(BorderFactory.createTitledBorder("CSV Import"));
        JButton importBtn = new JButton("Import Marks from CSV...");
        importBtn.addActionListener(e -> importCSV());
        importPanel.add(importBtn);
        importPanel.add(new JLabel("Select a CSV file to import student marks."));

        add(importPanel);

        // Export panel
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exportPanel.setBorder(BorderFactory.createTitledBorder("CSV Export"));
        JButton exportBtn = new JButton("Export All Marks to CSV...");
        exportBtn.addActionListener(e -> exportCSV());
        exportPanel.add(exportBtn);
        exportPanel.add(new JLabel("Export all marks data to a CSV file."));

        add(exportPanel);
    }

    private void importCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            csvService.importMarksFromCSVAsync(filePath)
                    .thenAccept(count -> {
                        setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(this,
                                "Successfully imported " + count + " record(s) from:\n" + filePath,
                                "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                        reportService.logActivity("Imported " + count + " records from " + filePath);
                    })
                    .exceptionally(ex -> {
                        setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(this,
                                "Import failed: " + ex.getCause().getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    });
        }
    }

    private void exportCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV File");
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();

            try {
                int count = csvService.exportMarksToCSV(filePath);
                JOptionPane.showMessageDialog(this,
                        "Successfully exported " + count + " record(s) to:\n" + filePath,
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                reportService.logActivity("Exported " + count + " records to " + filePath);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Export failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
