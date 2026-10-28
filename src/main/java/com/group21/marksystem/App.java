package com.group21.marksystem;

import com.group21.marksystem.cli.CLIMain;
import com.group21.marksystem.gui.GUIMain;

import javax.swing.*;
import java.util.Scanner;

/**
 * Main entry point for the Marks Administration System.
 * Allows the user to choose between CLI and GUI modes at startup.
 *
 * @author Group 21
 */
public class App {

    public static void main(String[] args) {
        printBanner();
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Select interface mode:");
        System.out.println("  1. Command Line Interface (CLI)");
        System.out.println("  2. Graphical User Interface (GUI)");
        System.out.print("Enter choice (1 or 2): ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                System.out.println("\nStarting CLI mode...\n");
                CLIMain cli = new CLIMain(scanner);
                cli.start();
                break;
            case "2":
                System.out.println("\nStarting GUI mode...\n");
                SwingUtilities.invokeLater(() -> {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        System.err.println("Warning: Could not set look and feel: " + e.getMessage());
                    }
                    GUIMain gui = new GUIMain();
                    gui.start();
                });
                break;
            default:
                System.out.println("Invalid choice. Defaulting to CLI mode.");
                new CLIMain(scanner).start();
                break;
        }
    }

    /**
     * Prints the startup banner with an aligned box.
     */
    private static void printBanner() {
        String[] lines = {
                "Marks Administration System - Group 21",
                "SDN260S - Software Design II"
        };

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, line.length());
        }

        System.out.println("╔" + "═".repeat(width + 4) + "╗");
        for (String line : lines) {
            System.out.println("║  " + line + " ".repeat(width - line.length()) + "  ║");
        }
        System.out.println("╚" + "═".repeat(width + 4) + "╝");
    }
}
