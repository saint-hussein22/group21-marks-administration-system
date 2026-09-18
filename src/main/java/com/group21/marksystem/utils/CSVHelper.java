package com.group21.marksystem.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading and writing CSV files.
 *
 * @author Group 21
 */
public class CSVHelper {

    /**
     * Reads a CSV file and returns the data as a list of string arrays.
     *
     * @param filePath Path to the CSV file.
     * @return List of rows, each row being an array of string values.
     */
    public static List<String[]> readCSV(String filePath) throws IOException {
        List<String[]> data = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                data.add(line);
            }
        } catch (CsvValidationException e) {
            throw new IOException("Error parsing CSV file: " + e.getMessage(), e);
        }

        return data;
    }

    /**
     * Writes data to a CSV file.
     *
     * @param filePath Path to output the CSV file.
     * @param headers  Header row.
     * @param data     Data rows.
     */
    public static void writeCSV(String filePath, String[] headers, List<String[]> data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            if (headers != null) {
                csvWriter.writeNext(headers);
            }

            for (String[] row : data) {
                csvWriter.writeNext(row);
            }
        }
    }

    /**
     * Recursively processes all CSV files in a directory.
     *
     * @param directoryPath Path to the directory.
     * @return List of all CSV file paths found.
     */
    public static List<String> findCSVFiles(String directoryPath) {
        List<String> csvFiles = new ArrayList<>();
        File dir = new File(directoryPath);

        if (dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
            if (files != null) {
                for (File file : files) {
                    csvFiles.add(file.getAbsolutePath());
                }
            }

            // Recursively search subdirectories
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    csvFiles.addAll(findCSVFiles(subDir.getAbsolutePath()));
                }
            }
        }

        return csvFiles;
    }

    /**
     * Generates a sample CSV file for student marks.
     *
     * @param filePath Output file path.
     * @return Number of records written.
     */
    public static int generateSampleCSV(String filePath) throws IOException {
        String[] headers = {"StudentID", "FirstName", "LastName", "Email", "Program", "Year",
                "SubjectCode", "Assessment1", "Assessment2", "Assessment3", "Total", "Grade"};

        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"S001", "Alice", "Johnson", "alice@student.university.ac.za", "Computer Science", "2",
                "CS260", "85", "90", "78", "", ""});
        data.add(new String[]{"S002", "Bob", "Smith", "bob@student.university.ac.za", "Computer Science", "2",
                "CS260", "72", "88", "91", "", ""});
        data.add(new String[]{"S003", "Carol", "Williams", "carol@student.university.ac.za", "Computer Science", "2",
                "CS260", "", "65", "72", "", ""});  // Missing first assessment
        data.add(new String[]{"S004", "David", "Brown", "david@student.university.ac.za", "Computer Science", "2",
                "CS260", "55", "48", "60", "", ""});

        writeCSV(filePath, headers, data);
        return data.size();
    }
}
