package com.github.kevinip.command;

import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import picocli.CommandLine;

public class CreateFilesTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private CreateFiles createFiles;

    @BeforeMethod
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        createFiles = spy(new CreateFiles());
    }

    private void setSysOut() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterMethod
    public void afterTest() {
    }

    private void restoreSysOut() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testCallWithInvalidNumberOfFiles() {
        // Given
        setSysOut();
        try {
            String[] args = { "-n=-1", "/tmp/foo" };

            CommandLine cmd = new CommandLine(createFiles);

            // When
            final int actual = cmd.execute(args);

            // Then
            assertEquals(1, actual);
            assertEquals(err.toString(), "Invalid number-of-files; it must be greater than zero\n");
        } finally {
            restoreSysOut();
        }
    }

    @Test
    public void testCallWithInvalidFileSize() {
        // Given
        setSysOut();
        try {
            String[] args = { "-min=11", "/tmp/foo" };

            CommandLine cmd = new CommandLine(createFiles);

            // When
            final int actual = cmd.execute(args);

            // Then
            assertEquals(1, actual);
            assertEquals(err.toString(), "min-file-size (11) must be less than or equal to max-file-size (10)\n");
        } finally {
            restoreSysOut();
        }
    }

    @Test
    public void testWriteFile() throws IOException {
        // Given
        Path path = Files.createTempDirectory("testWriteFile");
        try {
            final int numberOfFiles = 5;
            final int minFileSizeInKilobytes = 1;
            final int maxFileSizeInKilobytes = 2;
            final CreateFiles createFiles = new CreateFiles(
                path.toFile(),
                numberOfFiles,
                minFileSizeInKilobytes,
                maxFileSizeInKilobytes
            );

            final String filename = UUID.randomUUID().toString() + ".bin";
            Path filePath = Paths.get(path.toFile().getAbsolutePath(), filename);
            try {
                // When
                final int count = createFiles.writeFile(filePath);

                // Then
                final String actual = new String(Files.readAllBytes(filePath));
                assertEquals(actual.length(), count * 1024);

            } finally {
                Files.delete(filePath);
            }

        } finally {
            Files.delete(path);
        }
    }

}
