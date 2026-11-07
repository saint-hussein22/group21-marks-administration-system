package com.group21.marksystem.utils;

import com.group21.marksystem.db.Database;
import com.group21.marksystem.services.CSVService;
import com.group21.marksystem.services.MarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers object-stream snapshots and recursive CSV directory discovery.
 */
class SnapshotHelperTest {

    @TempDir
    Path tempDir;

    private MarkService markService;
    private CSVService csvService;

    @BeforeEach
    void setUp() {
        Database database = new Database(tempDir.resolve("marks.db").toString());
        markService = new MarkService(database);
        markService.seedIfEmpty();
        csvService = new CSVService(markService);
    }

    @Test
    void saveAndLoadSnapshotRoundTrip() throws Exception {
        Path snapshotFile = tempDir.resolve("backup.ser");
        SnapshotHelper.save(snapshotFile.toString(),
                markService.getAllStudents(), markService.getAllSubjects());

        SnapshotHelper.Snapshot snapshot = SnapshotHelper.load(snapshotFile.toString());
        assertEquals(markService.getAllStudents().size(), snapshot.getStudents().size());
        assertEquals(markService.getAllSubjects().size(), snapshot.getSubjects().size());
        assertTrue(snapshot.describe().contains("students="));
    }

    @Test
    void findCsvFilesWalksNestedFolders() throws Exception {
        Path nested = tempDir.resolve("batch").resolve("nested");
        Files.createDirectories(nested);
        Path csv = nested.resolve("marks.csv");
        Files.writeString(csv, "StudentID,FirstName,LastName\n");

        List<String> found = CSVHelper.findCSVFiles(tempDir.resolve("batch").toString());
        assertEquals(1, found.size());
        assertTrue(found.get(0).endsWith("marks.csv"));
    }

    @Test
    void importDirectoryReportsEmptyFolder() throws Exception {
        Path empty = tempDir.resolve("empty");
        Files.createDirectories(empty);

        CSVService.ImportSummary summary = csvService.importDirectoryDetailed(empty.toString());
        assertEquals(0, summary.getMarksSaved());
        assertFalse(summary.getWarnings().isEmpty());
    }
}
