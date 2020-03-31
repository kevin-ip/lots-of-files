/*
 * Copyright (c) 2020-Present Incorta. All Rights Reserved.
 * Licensed Material - Property of Incorta.
 */
package com.github.kevinip.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "create",
    aliases = { "c" },
    mixinStandardHelpOptions = true,
    description = { "Create lots of files with random sizes" })
public class CreateFiles implements Callable<Integer> {

    private static final String LIBRARY = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Parameters(index = "0", description = "The directory where lots of files are created")
    private File directory;

    @Option(names = { "-n", "--number-of-files" }, description = "Number of files to be generated; Default 10")
    private int numberOfFiles = 10;

    @Option(names = { "-min", "--min-file-size" }, description = "Minimum file size for generated files; Default 1 kB")
    private int minFileSizeInKilobytes = 1;

    @Option(names = { "-max", "--max-file-size" }, description = "Maximum file size for generated files; Default 10 kB")
    private int maxFileSizeInKilobytes = 10;

    public CreateFiles() {
        //empty
    }

    CreateFiles(File directory, int numberOfFiles, int minFileSizeInKilobytes, int maxFileSizeInKilobytes) {
        this.directory = directory;
        this.numberOfFiles = numberOfFiles;
        this.minFileSizeInKilobytes = minFileSizeInKilobytes;
        this.maxFileSizeInKilobytes = maxFileSizeInKilobytes;
    }

    @Override
    public Integer call() throws Exception {

        if (numberOfFiles <= 0) {
            System.err.println("Invalid number-of-files; it must be greater than zero");
            return 1;
        }

        if (minFileSizeInKilobytes > maxFileSizeInKilobytes) {
            System.err.printf("min-file-size (%s) must be less than or equal to max-file-size (%s)\n",
                minFileSizeInKilobytes,
                maxFileSizeInKilobytes);
            return 1;
        }

        //Create the directory if not exists
        final Path path = Paths.get(
            directory.getAbsolutePath(),
            "lotOfFiles-" + System.currentTimeMillis());
        System.out.println("Path: " + path);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        doCreate(path);

        return 0;
    }

    void doCreate(Path rootPath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");

        for (int i = 1; i <= numberOfFiles; i++) {
            if (i % 1000 == 0 || i == numberOfFiles) {
                System.out.printf("%s / %s", i, numberOfFiles);
            }

            final String filename = "file-" + UUID.randomUUID().toString() + ".bin";
            final byte[] digest = md.digest(filename.getBytes("UTF-8"));
            String digestString = new BigInteger(1, digest).toString(16);

            //create intermediate directories to make `ls` happy
            final String prefix = digestString.substring(0, 4);
            final String subPrefix = digestString.substring(4, 8);

            final Path directory = Paths.get(rootPath.toFile().getAbsolutePath(), prefix, subPrefix);
            createDirectories(directory);

            final Path file = Paths.get(directory.toFile().getAbsolutePath(), filename);
            writeFile(file);
        }
    }

    /**
     * @param filePath
     * @return number of kilobytes written to the file
     * @throws IOException
     */
    int writeFile(Path filePath) throws IOException {
        final Random random = new Random();
        final int sizeInKilobytes = random.nextInt(maxFileSizeInKilobytes - minFileSizeInKilobytes) + minFileSizeInKilobytes;

        //create an iterator with sizeInKilobytes number of character sequence
        // Each character sequence consists of 1024 characters
        final Iterator<Seq<Character>> pages = Iterator
            .continually(() -> LIBRARY.charAt(random.nextInt(LIBRARY.length())))
            .grouped(1024)
            .zipWithIndex()
            .takeWhile(tuple -> {
                final int index = tuple._2;
                return index < sizeInKilobytes;
            })
            .map(Tuple2::_1);

        try (final Writer writer = new FileWriter(filePath.toFile())) {
            for (Seq<Character> page : pages) {
                writer.write(page.mkString());
            }
        }

        return sizeInKilobytes;
    }

    void createDirectories(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (FileAlreadyExistsException e) {
                // ignore
            }
        }
    }
}
