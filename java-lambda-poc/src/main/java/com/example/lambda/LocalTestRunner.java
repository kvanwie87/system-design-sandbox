package com.example.lambda;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple local test runner for the CSV processor.
 * Allows testing the CSV processing logic without deploying to AWS.
 *
 * Usage: java -cp <jar> com.example.lambda.LocalTestRunner <input.csv> <output.json>
 */
public class LocalTestRunner {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: LocalTestRunner <input-csv-path> <output-json-path>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        System.out.println("Input:  " + inputPath);
        System.out.println("Output: " + outputPath);

        CsvProcessorService processor = new CsvProcessorService();

        try (InputStream input = new FileInputStream(inputPath)) {
            String jsonResult = processor.processCSV(input);
            Files.writeString(Path.of(outputPath), jsonResult);
            System.out.println("Processing complete. " + jsonResult.lines().count() + " lines written.");
        }
    }
}
