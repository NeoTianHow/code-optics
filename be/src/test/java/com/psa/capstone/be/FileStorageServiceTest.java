package com.psa.capstone.be;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.psa.capstone.be.model.ReportFileInfo;
import com.psa.capstone.be.service.FileStorageService;

class FileStorageServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void saveReportWritesPdfUsingTimestampAndReportId() throws Exception {
        FileStorageService service = storageService(tempDir);
        byte[] content = "pdf-bytes".getBytes();
        LocalDateTime generatedAt = LocalDateTime.of(2026, 6, 9, 17, 45, 30);

        String filename = service.saveReport("abc-123", content, generatedAt);

        assertEquals("Project_Report_20260609_174530_abc-123.pdf", filename);
        assertArrayEquals(content, Files.readAllBytes(tempDir.resolve(filename)));
        assertArrayEquals(content, service.getReportFile(filename));
    }

    @Test
    void listAvailableReportsIgnoresNonReportsAndSortsNewestFirst() throws Exception {
        FileStorageService service = storageService(tempDir);
        Files.write(tempDir.resolve("Project_Report_20240101_010101_old.pdf"), "old".getBytes());
        Files.write(tempDir.resolve("Project_Report_20260203_040506_new.pdf"), "new".getBytes());
        Files.write(tempDir.resolve("notes.txt"), "ignore".getBytes());
        Files.write(tempDir.resolve("Project_Report_20260203_040506_not-a-pdf.txt"), "ignore".getBytes());

        List<ReportFileInfo> reports = service.listAvailableReports();

        assertEquals(2, reports.size());
        assertEquals("new", reports.get(0).getReportId());
        assertEquals("Project_Report_20260203_040506_new.pdf", reports.get(0).getFilename());
        assertEquals("/api/reports/files/Project_Report_20260203_040506_new.pdf", reports.get(0).getDownloadUrl());
        assertEquals("old", reports.get(1).getReportId());
        assertTrue(reports.get(0).getGeneratedAt().isAfter(reports.get(1).getGeneratedAt()));
    }

    @Test
    void listAvailableReportsReturnsEmptyListWhenDirectoryHasNoReports() {
        FileStorageService service = storageService(tempDir);

        assertTrue(service.listAvailableReports().isEmpty());
    }

    private static FileStorageService storageService(Path reportDir) {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "reportStorageLocation", reportDir.toString());
        service.init();
        return service;
    }
}
