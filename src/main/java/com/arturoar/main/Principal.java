package com.arturoar.main;

import com.arturoar.externalsortingalgorithms.BalanceMergeSort;
import com.arturoar.externalsortingalgorithms.ExternalRadixSort;
import com.arturoar.externalsortingalgorithms.PolyphaseMergeSort;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public class Principal {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("""
                        1. Polyphase Merge Sort
                        2. Balanced Merge Sort
                        3. External Radix Sort
                        4. Exit
                        Option: """);
                if (!scanner.hasNextLine()) {
                    return;
                }

                try {
                    int option = Integer.parseInt(scanner.nextLine().strip());
                    if (option == 4) {
                        return;
                    }
                    if (option < 1 || option > 3) {
                        System.out.println("Invalid option.");
                        continue;
                    }

                    File input = selectFile();
                    if (input == null) {
                        continue;
                    }

                    Path result;
                    if (option == 1) {
                        System.out.print("Polyphase block size: ");
                        if (!scanner.hasNextLine()) {
                            return;
                        }
                        int size = Integer.parseInt(scanner.nextLine().strip());
                        result = new PolyphaseMergeSort(input, size).sort();
                    } else if (option == 2) {
                        result = new BalanceMergeSort(input).sort();
                    } else {
                        result = new ExternalRadixSort(input).sort();
                    }
                    System.out.println("Sorted file: " + result);
                } catch (NumberFormatException exception) {
                    System.out.println("Enter a whole number.");
                } catch (IOException | IllegalArgumentException exception) {
                    System.out.println("Could not sort the file. " + exception.getMessage());
                }
            }
        }
    }

    private static File selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        return chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }
}
