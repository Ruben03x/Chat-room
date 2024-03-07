package com.project1;

import javafx.application.Platform;

public class ClientService {
    private static Client currentClient;
    private static InteractController currentController;

    public static void setCurrentClient(Client client) {
        currentClient = client;
    }

    public static Client getCurrentClient() {
        return currentClient;
    }

    public static synchronized void setCurrentController(InteractController controller) {
        if (controller.globalArea != null) {
            currentController = controller;
            System.out.println("CurrentController set with globalArea initialized.");
        } else {
            System.out.println("Attempted to set CurrentController without globalArea being initialized.");
        }
    }

    public static InteractController getCurrentController() {
        return currentController;
    }

    public static void safelyAppendMessage(String message) {
        InteractController controller = getCurrentController();
        if (controller != null) {
            Platform.runLater(() -> controller.appendMessage(message));
        } else {
            System.out.println("No valid controller instance available for appending messages.");
        }
    }
}
