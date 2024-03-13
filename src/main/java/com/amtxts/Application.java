package com.amtxts;

import javax.swing.*;

public class Application {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppWindow appWindow = new AppWindow();
            appWindow.buildWindow();
        });
    }
}
