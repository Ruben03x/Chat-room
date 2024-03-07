package com.project1;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ListView;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class InteractController {

    @FXML
    private TextField textLogin;

    @FXML
    private TextField fieldMessage;

    @FXML
    public volatile ListView<String> userListView;

    @FXML
    public TextArea globalArea;

    @FXML
    public volatile TextArea textWhisper;

    @FXML
    private volatile TabPane tabPane;

    @FXML
    private Tab whispersTab;

    @FXML
    private Tab globalTab;

    @FXML
    private TextField textAddress;

    @FXML
    private TextField textPort;

    private List<String> messageQueue = new ArrayList<>();

    private String username;

    private Client client = null;

    private volatile Boolean onWhisper = false;
    private Hashtable<String, ArrayList<String>> whisperMessages = new Hashtable<>();

    /**
     * Displays the whisper messages for the selected user.
     * 
     * This gets triggered when the user clicks on an online user int the listview
     * of online users.
     * It then retrieves the selected user,
     * 
     * @param event The mouse event that triggered the method.
     */
    @FXML
    void displayWhisperMessages(MouseEvent event) {
        // System.out.println("This is happening");
        String whisperee = getSelectedUser();
        if (whisperee.startsWith("*")) {
            whisperNotification(whisperee);
            whisperee = whisperee.substring(1);
        }
        ArrayList<String> messages = whisperMessages.get(whisperee);
        textWhisper.clear();
        for (String message : messages) {
            // System.out.println("//" + message);
            appendWhisperMessage(message);
        }

    }

    @FXML
    private void handleSignIn(ActionEvent event) {
        try {
            username = textLogin.getText();
            String serverAddress = textAddress.getText();
            int serverPort = Integer.parseInt(textPort.getText());

            // initialise the client connection to the server
            if (client == null) {
                Socket socket = new Socket(serverAddress, serverPort);
                textAddress.setDisable(true);
                textPort.setDisable(true);
                client = new Client(socket, this);
                ClientService.setCurrentClient(client);
                client.receiver();
            }
            client.sendUserName(username);
        } catch (Exception e) {
            // System.out.println("Server is offline : " + e.getMessage());
            // e.printStackTrace();
            Platform.runLater(() -> showErrorDialog("Server not available of given address and port"));
        }

        if (client != null) {
            while (!client.checkedUsername) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    // e.printStackTrace();
                }
            }
            client.checkedUsername = false;
            if (client.usernameOK) {
                try {

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project1/GUI_Main.fxml"));
                    loader.setController(this);
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(loader.load()));
                    stage.setTitle(username);

                    stage.setOnCloseRequest(e -> {
                        if (client != null) {
                            client.disconnect();
                        }
                    });

                    stage.show();

                    flushMessageQueue();
                } catch (Exception e) {
                    System.out.println("Error occurred loading Main GUI: " + e.getMessage());
                }
            }
        }

    }

    @FXML
    public void handleSend(ActionEvent event) {
        String message = fieldMessage.getText();
        SingleSelectionModel<Tab> selectedTab = tabPane.getSelectionModel();
        onWhisper = selectedTab.getSelectedIndex() == 1;

        String whisperTo = getSelectedUser();
        if (onWhisper) {
            if (whisperTo != null) {
                if (!message.isEmpty()) {
                    client.sendMessage("##WHISPER," + whisperTo + "," + message);
                    fieldMessage.clear();
                } else
                    showErrorDialog("Message cannot be empty.");

            } else {
                // Show an error dialog if no user is selected
                showErrorDialog("Please select a user to whisper to.");
            }
        } else if (!message.isEmpty() && client != null) {
            fieldMessage.clear();
            client.sendMessage(message);
        } else if (message.isEmpty()) {
            showErrorDialog("Message cannot be empty.");
        } else {
            System.out.println("Client is not initialized.");
        }
    }

    /**
     * Retrieves the selected user from the user list view.
     * 
     * @return The username of the selected user.
     */
    public String getSelectedUser() {
        return userListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Updates the user list view with the provided list of users.
     * 
     * This method updates the user list view on the UI with the provided list of
     * users.
     * 
     * @param users The list of users to be displayed in the user list view.
     */
    public void updateUserList(ArrayList<String> users) {
        Platform.runLater(() -> userListView.getItems().setAll(users));
        System.out.println("User list updated");
    }

    /**
     * Appends a message to the global area.
     * 
     * This method appends the provided message to the global area on the UI. If the
     * global area is null, the message is queued for later display.
     * 
     * @param message The message to be appended.
     */
    public void appendMessage(String message) {
        Platform.runLater(() -> {
            if (globalArea != null) {
                globalArea.appendText(message + "\n");
            } else {
                System.out.println("globalArea is null, queuing message.");
                messageQueue.add(message);
            }
        });
    }

    /**
     * Appends a whisper message to the whisper area.
     * 
     * This method appends the provided whisper message to the whisper area on the
     * UI. If the whisper area is null, the whisper message is not appended.
     * 
     * @param message The whisper message to be appended.
     */

    public void appendWhisperMessage(String message) {
        Platform.runLater(() -> {
            if (textWhisper != null) {
                textWhisper.appendText(message + "\n");
            } else {
                System.out.println("textWhisper is null, cannot append whisper message.");
            }
        });
    }

    /**
     * Flushes the message queue by appending messages to the global area.
     * 
     * This method is used to display messages in the global area on the UI. It is
     * executed on the JavaFX application thread.
     */
    private void flushMessageQueue() {
        Platform.runLater(() -> {
            if (globalArea != null) {
                messageQueue.forEach(msg -> globalArea.appendText(msg + "\n"));
                messageQueue.clear();
            }
        });
    }

    /**
     * Initializes the InteractController.
     * 
     * This method is called upon initialization of the InteractController. It
     * checks if the global area is null and prints a message to the console.
     */
    public void initialize() {
        System.out.println("InteractController initialized. globalArea null? " + (globalArea == null));
    }

    /**
     * Shows an error dialog with the specified message.
     * 
     * @param message The error message to be displayed.
     */
    public void showErrorDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Addss the user to the hashmap.
     *
     * @param whisperee Username of the whisperee.
     */

    public void addWhisperee(String whisperee) {
        whisperMessages.put(whisperee, new ArrayList<String>());
    }

    /**
     * removes the user from the hashmap.
     *
     * @param whisperee Username of the whisperee.
     */
    public void removeWhisperee(String whisperee) {
        whisperMessages.remove(whisperee);
    }

    /**
     * Adds the whisper to the corresponding chat.
     *
     * @param whisperee Username of the user that sent the whisper or sent the
     *                  whisper.
     * @param message   The message to be added
     */
    public void addWhisperMessage(String whisperee, String message) {
        ArrayList<String> messages = whisperMessages.get(whisperee);
        messages.add(message);
        // whisperMessages.put(whisperee, messages);
    }

    /**
     * Adds a star, or removes a star from the user that send a whisper to this
     * user.
     *
     * @param whisperer Username of the whisperer.
     */
    public void whisperNotification(String whisperer) {
        // Find the index of the item to update
        ArrayList<String> usernames = client.clients;
        int indexToUpdate = -1;
        for (int i = 0; i < usernames.size(); i++) {
            if (whisperer.equals(usernames.get(i))) {
                indexToUpdate = i;
                break;
            }
        }
        // Update the item if found
        if (indexToUpdate != -1) {
            if (!whisperer.startsWith("*"))
                usernames.set(indexToUpdate, "*" + whisperer);
            else
                usernames.set(indexToUpdate, whisperer.substring(1));
        }

        updateUserList(usernames);

    }
}
