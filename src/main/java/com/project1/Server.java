package com.project1;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {

	private static ServerSocket serverSocket;
	private static boolean running = true;
	private static int port;

	public Server(ServerSocket serverSocket) {
		Server.serverSocket = serverSocket;
	}

	public static void main(String[] args) throws IOException {
		Scanner input = new Scanner(System.in);
		System.out.println("Enter a port number:");
		while (true) {
			String stringPort = input.nextLine();
			try {
				port = Integer.parseInt(stringPort);
				serverSocket = new ServerSocket(port);
				break;
			} catch (Exception e) {
				System.out.println("Port " + stringPort + " not usable, Enter another:");
			}
		}
		input.close();

		Server server = new Server(serverSocket);
		server.startServerSocket();
	}

	public void startServerSocket() {
		try {
			System.out.println("Server running on port: " + port);
			while (running && !serverSocket.isClosed()) {
				Socket clientSocket = serverSocket.accept();
				ClientManager client = new ClientManager(clientSocket);
				Thread newThread = new Thread(client);
				newThread.start();
			}
		} catch (IOException e) {
			System.out.println("Server stopped.");
		}
	}

	// public static synchronized void stopServer() {
	// running = false;
	// try {
	// if (!serverSocket.isClosed()) {
	// serverSocket.close();
	// System.out.println("Server has been shut down.");
	// }
	// } catch (IOException e) {
	// System.out.println("Error closing server: " + e.getMessage());
	// }
	// }
}

class ClientManager implements Runnable {

	public static ArrayList<ClientManager> clients = new ArrayList<>();
	public static ArrayList<String> usernames = new ArrayList<>();
	private Socket clientSocket;
	private BufferedReader bufRead;
	private BufferedWriter bufWrite;
	private String username;
	// private volatile Boolean canStart = false;

	public ClientManager(Socket clientSocket) {

		try {
			this.clientSocket = clientSocket;
			bufRead = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			bufWrite = new BufferedWriter(new OutputStreamWriter(
					clientSocket.getOutputStream()));
			while (true) {
				username = bufRead.readLine();

				// ensures client connecting has a uniques username
				if (usernames.contains(username)) {
					bufWrite.write("##USERNAMETAKEN");
					bufWrite.newLine();
					bufWrite.flush();
				} else {
					bufWrite.write("##USERNAMEOK");
					bufWrite.newLine();
					bufWrite.flush();
					System.out.println(username + " connected");
					for (String username_ : usernames) {
						bufWrite.write("##ONLINEUSER" + username_);
						bufWrite.newLine();
						bufWrite.flush();
					}

					clients.add(this);
					usernames.add(username);

					for (ClientManager client_ : clients) {
						if (!client_.username.equals(username)) {
							client_.bufWrite.write("##CLIENTJOIN" + username);
							client_.bufWrite.newLine();
							client_.bufWrite.flush();

						}
					}

					break;

				}
			}

		} catch (Exception e) {
			System.out.println("Error initialising client");
		}
		// canStart = true;
	}

	@Override
	public void run() {
		String msg;
		try {
			while (!clientSocket.isClosed()) {
				msg = bufRead.readLine();
				if (msg != null && msg.equals("##DISCONNECT")) {
					closeAllStreamsBroadcast();
					break;
				} else if (msg != null && msg.startsWith("##WHISPER")) {
					handleWhisperMessage(msg);
				} else if (msg != null) {
					broadcastMessage(msg);
				}
			}
		} catch (Exception e) {
			closeAllStreamsBroadcast();
		}
	}

	private void handleWhisperMessage(String msg) {
		try {
			String[] parts = msg.split(",", 3);

			String targetUsername = parts[1];
			String whisperMsg = parts[2];
			ClientManager targetClient = findClientByUsername(targetUsername);

			// Sends message back to whisperer to print to output
			bufWrite.write("##WHISPERTO," + targetUsername + "," + whisperMsg);
			bufWrite.newLine();
			bufWrite.flush();

			// Sends message to whisperee to print to output
			if (targetClient != null) {
				targetClient.bufWrite.write("##WHISPERFROM," + username + "," + whisperMsg);
				targetClient.bufWrite.newLine();
				targetClient.bufWrite.flush();
			}
		} catch (IOException e) {
			System.err.println("Error handling whisper message: " + e.getMessage());
		}
	}

	private void broadcastMessage(String msg) throws IOException {

		// Sends message back to client to print to output
		bufWrite.write("You: " + msg);
		bufWrite.newLine();
		bufWrite.flush();

		// Sends message to all other clients to print to output
		for (ClientManager client_ : clients) {
			if (!client_.username.equals(this.username)) {
				client_.bufWrite.write(username + ": " + msg);
				client_.bufWrite.newLine();
				client_.bufWrite.flush();
			}
		}
	}

	private ClientManager findClientByUsername(String username) {
		for (ClientManager client : clients) {
			if (client.username.equals(username)) {
				return client;
			}
		}
		return null;
	}

	public void closeAllStreamsBroadcast() {
		System.out.println(username + " disconnected");
		clients.remove(this);
		usernames.remove(username);
		try {
			for (ClientManager client : clients) {
				if (!client.username.equals(this.username)) {
					client.bufWrite.write("##CLIENTLEFT" + username);
					client.bufWrite.newLine();
					client.bufWrite.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		closeAllStreams();

		// if (clients.isEmpty()) {
		// System.out.println("No more clients connected. Shutting down the server.");
		// Server.stopServer();
		// }
	}

	public void closeAllStreams() {
		try {

			if (bufRead != null)
				bufRead.close();
			if (bufWrite != null)
				bufWrite.close();
			if (clientSocket != null)
				clientSocket.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}
}
