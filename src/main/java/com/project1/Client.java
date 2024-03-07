package com.project1;

import java.io.*;
import java.net.*;
import java.util.*;

import javafx.application.Platform;

public class Client {

	public volatile ArrayList<String> clients = new ArrayList<>();

	private Socket socket = null;
	private BufferedReader bufRead = null;
	private BufferedWriter bufWrite = null;
	private InteractController interactController;
	public Boolean checkedUsername = false;
	public Boolean usernameOK = false;

	/**
	 * Client constructor, starts the neccessary streams for communication with the
	 * server
	 *
	 * @param socket             This clients socket
	 * @param interactController The InteractController to communicate with the GUI
	 */
	public Client(Socket socket, InteractController interactController) {
		try {
			this.socket = socket;
			this.bufRead = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			this.bufWrite = new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream()));
			this.interactController = interactController;

		} catch (IOException e) {
			closeAllSreams(bufRead, bufWrite, socket);
		}

	}

	/**
	 * Sends the username to the Server.
	 *
	 * @param username Username chosen by user.
	 */
	public void sendUserName(String username) {
		try {
			bufWrite.write(username);
			bufWrite.newLine();
			bufWrite.flush();
		} catch (Exception e) {

			closeAllSreams(bufRead, bufWrite, socket);
		}
	}

	/**
	 * Sends the message to the server.
	 *
	 * @param message The message the user wants to send.
	 */
	public void sendMessage(String message) {
		try {
			bufWrite.write(message);
			bufWrite.newLine();
			bufWrite.flush();
		} catch (Exception e) {
			closeAllSreams(bufRead, bufWrite, socket);
		}
	}

	/**
	 * Receives messages from the server and handles them accordingly and does all
	 * this in a seperate thread
	 * When a message is received, it updates it updates the GUI accordingly and
	 * displays the message at its correct output
	 */
	public void receiver() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					// message from server
					String msg;

					while (socket.isConnected()) {
						msg = bufRead.readLine();
						System.out.println(msg);
						if (msg == null || msg.equals("##DISCONNECT")) {
							closeAllSreams(bufRead, bufWrite, socket);
							break;
						}
						if (msg.equals("##USERNAMETAKEN")) {
							checkedUsername = true;
							Platform.runLater(() -> interactController
									.showErrorDialog("Username is taken. Please try a different username."));
						}
						if (msg.equals("##USERNAMEOK")) {
							checkedUsername = true;
							usernameOK = true;
						}
						if (msg.startsWith("##WHISPERFROM")) {
							String[] parts = msg.split(",", 3);
							String whisperFrom = parts[1];
							String whisperMsg = parts[2];
							interactController.addWhisperMessage(whisperFrom, whisperFrom + ": " + whisperMsg);
							String selectedUser = interactController.getSelectedUser();
							if (selectedUser != null) {
								if (selectedUser.equals(whisperFrom)) {
									interactController.appendWhisperMessage(whisperFrom + ": " + whisperMsg);
								} else {
									interactController.whisperNotification(whisperFrom);
								}
							} else {
								interactController.whisperNotification(whisperFrom);
							}
						}
						if (msg.startsWith("##WHISPERTO")) {
							String[] parts = msg.split(",", 3);
							String whisperTo = parts[1];
							String whisperMsg = parts[2];
							interactController.addWhisperMessage(whisperTo, "You: " + whisperMsg);
							interactController.appendWhisperMessage("You: " + whisperMsg);
						}

						if (msg.startsWith("##ONLINEUSER")) {
							clients.add(msg.substring(12));
							interactController.updateUserList(clients);
							interactController.addWhisperee(msg.substring(12));
						}

						if (msg.startsWith("##CLIENTJOIN")) {
							clients.add(msg.substring(12));
							System.out.println(msg.substring(12) + " joined");
							interactController.appendMessage(msg.substring(12) + " joined");
							interactController.updateUserList(clients);
							interactController.addWhisperee(msg.substring(12));
						}
						if (msg.startsWith("##CLIENTLEFT")) {
							clients.remove(msg.substring(12));
							System.out.println(msg.substring(12) + " left");
							interactController.appendMessage(msg.substring(12) + " left");
							interactController.updateUserList(clients);
							interactController.removeWhisperee(msg.substring(12));
						}

						if (msg.charAt(0) != '#') {
							System.out.println(msg);
							interactController.appendMessage(msg);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					closeAllSreams(bufRead, bufWrite, socket);
				}
			}
		}).start();
	}

	/**
	 * Closes all the streams associated with this client
	 *
	 * @param bufRead BufferedReader, reads from the connected socket.
	 * @param bWriter The BufferedWriter, writes to the connected socket.
	 * @param socket  Socket, connects this client to the server.
	 */
	public void closeAllSreams(BufferedReader bufRead, BufferedWriter bWriter, Socket socket) {
		System.out.println("Server disconnected");
		try {
			if (bufRead != null)
				bufRead.close();
			if (bufWrite != null)
				bufWrite.close();
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		;
		System.exit(0);
	}

	/**
	 * Sends a disconnect message to the server and closes all streams
	 */
	public void disconnect() {
		try {
			if (bufWrite != null) {
				bufWrite.write("##DISCONNECT");
				bufWrite.newLine();
				bufWrite.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeAllSreams(bufRead, bufWrite, socket);
		}
	}
}
