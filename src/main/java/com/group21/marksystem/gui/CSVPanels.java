package com.group21.marksystem.gui;

import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.MarkService;
import com.group21.marksystem.services.ReportService;
import com.group21.marksystem.utils.SnapshotHelper;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Panel for CSV template, import and export operations.
 *
 * @author Group 21
 */
public class CSVPanels extends JPanel {
    private static final String TEMPLATE_FILE = "CPUT_Marks_Template.csv";

    private final CSVService csvService;
    private final ReportService reportService;
    private final MarkService markService;
    private final Runnable onDataChanged;

    public CSVPanels(CSVService csvService, ReportService reportService) {
        this(csvService, reportService, null, () -> { });
    }

    public CSVPanels(CSVService csvService, ReportService reportService, Runnable onDataChanged) {
        this(csvService, reportService, null, onDataChanged);
    }

    public CSVPanels(CSVService csvService, ReportService reportService,
                     MarkService markService, Runnable onDataChanged) {
        this.csvService = csvService;
        this.reportService = reportService;
        this.markService = markService;
        this.onDataChanged = onDataChanged != null ? onDataChanged : () -> { };
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridLayout(5, 1, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Template panel
        JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        templatePanel.setBorder(BorderFactory.createTitledBorder("Download Marks Template"));
        JButton templateBtn = new JButton("Download CSV Template...");
        templateBtn.addActionListener(e -> downloadTemplate());
        templatePanel.add(templateBtn);
        templatePanel.add(new JLabel("<html>CPUT Bellville class list with StudentID, SubjectCode and "
                + "Assessment1-5 columns.<br>Fill in the marks, then import the saved file.</html>"));

        // Import panel
        JPanel importPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        importPanel.setBorder(BorderFactory.createTitledBorder("CSV Import"));
        JButton importBtn = new JButton("Import Marks from CSV...");
        importBtn.addActionListener(e -> importCSV());
        importPanel.add(importBtn);
        importPanel.add(new JLabel("Updates existing students and replaces their marks."));

        // Recursive folder import
        JPanel folderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Recursive Folder Import"));
        JButton folderBtn = new JButton("Import Folder of CSVs...");
        folderBtn.addActionListener(e -> importFolder());
        folderPanel.add(folderBtn);
        folderPanel.add(new JLabel("Walks sub-folders recursively and imports every .csv file found."));

        // Export panel
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exportPanel.setBorder(BorderFactory.createTitledBorder("CSV Export"));
        JButton exportBtn = new JButton("Export All Marks to CSV...");
        exportBtn.addActionListener(e -> exportCSV());
        exportPanel.add(exportBtn);
        exportPanel.add(new JLabel("Creates a file in the same layout as the template."));

        // Object snapshot
        JPanel snapshotPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        snapshotPanel.setBorder(BorderFactory.createTitledBorder("Object Snapshot"));
        JButton snapshotBtn = new JButton("Save .ser Snapshot...");
        snapshotBtn.addActionListener(e -> saveSnapshot());
        snapshotPanel.add(snapshotBtn);
        snapshotPanel.add(new JLabel("Java object serialization backup of students, subjects and marks."));

        add(templatePanel);
        add(importPanel);
        add(folderPanel);
        add(exportPanel);
        add(snapshotPanel);
    }

    private void downloadTemplate() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Marks Template");
        fileChooser.setSelectedFile(new File(TEMPLATE_FILE));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = ensureCsvExtension(fileChooser.getSelectedFile().getAbsolutePath());
        try {
            int count = csvService.generateTemplate(filePath);
            JOptionPane.showMessageDialog(this,
                    "Template saved with " + count + " student row(s):\n" + filePath
                            + "\n\nFill in the Assessment columns and import the file.",
                    "Template Downloaded", JOptionPane.INFORMATION_MESSAGE);
            reportService.logActivity("Downloaded CSV template: " + filePath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not create the template: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Marks CSV File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        csvService.importWithReportAsync(filePath)
                .thenAccept(summary -> SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JTextArea area = new JTextArea(summary.describe(), 14, 70);
                    area.setEditable(false);
                    area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    JOptionPane.showMessageDialog(this,
                            new JScrollPane(area),
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    reportService.logActivity("Imported " + summary.getMarksSaved()
                            + " mark record(s) from " + filePath);
                    onDataChanged.run();
                }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    SwingUtilities.invokeLater(() -> {
                        setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(this,
                                "Import failed: " + cause.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
    }

    private void exportCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Marks CSV File");
        fileChooser.setSelectedFile(new File("marks_export.csv"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = ensureCsvExtension(fileChooser.getSelectedFile().getAbsolutePath());
        try {
            int count = csvService.exportMarksToCSV(filePath);
            JOptionPane.showMessageDialog(this,
                    "Exported " + count + " record(s) to:\n" + filePath,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            reportService.logActivity("Exported " + count + " records to " + filePath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Export failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Folder Containing CSV Files");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String directory = fileChooser.getSelectedFile().getAbsolutePath();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        csvService.importDirectoryAsync(directory)
                .thenAccept(summary -> SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JTextArea area = new JTextArea(summary.describe(), 14, 70);
                    area.setEditable(false);
                    area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    JOptionPane.showMessageDialog(this,
                            new JScrollPane(area),
                            "Folder Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    reportService.logActivity("Recursive CSV import from " + directory
                            + " saved " + summary.getMarksSaved() + " mark(s)");
                    onDataChanged.run();
                }))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    SwingUtilities.invokeLater(() -> {
                        setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(this,
                                "Folder import failed: " + cause.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
    }

    private void saveSnapshot() {
        if (markService == null) {
            JOptionPane.showMessageDialog(this,
                    "Snapshot is unavailable in this view.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Object Snapshot");
        fileChooser.setSelectedFile(new File("marks_snapshot.ser"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".ser")) {
            filePath = filePath + ".ser";
        }

        try {
            SnapshotHelper.save(filePath, markService.getAllStudents(), markService.getAllSubjects());
            SnapshotHelper.Snapshot snapshot = SnapshotHelper.load(filePath);
            JOptionPane.showMessageDialog(this,
                    "Snapshot saved:\n" + filePath + "\n\n" + snapshot.describe(),
                    "Snapshot Complete", JOptionPane.INFORMATION_MESSAGE);
            reportService.logActivity("Saved object snapshot: " + filePath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Snapshot failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String ensureCsvExtension(String path) {
        return path.toLowerCase().endsWith(".csv") ? path : path + ".csv";
    }
}
