package dev.xerohero;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RunGui {
    public static void main(String[] args) {
        // --- Force macOS Native Dock Icon Override ---
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {

                    // Create a matching 128x128 buffered image for the Apple Dock
                    BufferedImage dockImg = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = dockImg.createGraphics();

                    // Enable high-quality anti-aliasing
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // 1. Draw Sleek Terminal Background (Matching #2c3e50)
                    g2.setColor(new Color(0x2C, 0x3E, 0x50));
                    g2.fillRoundRect(8, 8, 112, 112, 24, 24);

                    // 2. Draw Neon Green Prompts (Matching #2ecc71)
                    g2.setColor(new Color(0x2E, 0xCC, 0x71));
                    g2.setStroke(new java.awt.BasicStroke(8, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));

                    // Draw Chevron '>'
                    g2.drawLine(35, 40, 60, 55);
                    g2.drawLine(60, 55, 35, 70);

                    // Draw Cursor '_'
                    g2.fillRect(70, 64, 25, 8);

                    // 3. Draw Strata Layer Bars
                    g2.setColor(new Color(0x34, 0x98, 0xDB)); // Stream Blue
                    g2.fillRect(35, 88, 60, 6);

                    g2.setColor(new Color(0xE6, 0x7E, 0x22)); // Alert Amber
                    g2.fillRect(35, 98, 35, 6);

                    g2.dispose();

                    // Explicitly push the image memory block into the macOS Dock architecture
                    taskbar.setIconImage(dockImg);
                }
            }
        } catch (Exception e) {
            // Silently fall back if running on a system without a native taskbar
            System.out.println("OS Taskbar override skipped: " + e.getMessage());
        }

        // Now boot up the standard JavaFX dashboard window
        DashboardApp.main(args);
    }
}