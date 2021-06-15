package Project.Server;


import Project.Console.ConsoleColors;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Amir Iravanimanesh
 * @date 6/7/2021
 */
public class UserThread extends Thread {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter writer;
    private String userName;
    private String role;
    private boolean isAlive = true;

    public UserThread(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            printUsers();
            while (true) {
                userName = reader.readLine();
                if (isDuplicate(userName))
                    sendMessage("DUPLICATE-USERNAME");
                else {
                    sendMessage("USERNAME-ACCEPTED");
                    server.addUserName(userName);
                    break;
                }
            }
            String serverMessage = "New user connected: " + userName;
            server.broadcast(serverMessage, this, null);

            String clientMessage;

            do {
                clientMessage = reader.readLine();
                serverMessage = ConsoleColors.BLUE + "[" + userName + "]: " + clientMessage + ConsoleColors.RESET;
                if (ChatServer.state.equalsIgnoreCase("VOTING")) {
                    boolean correct = false;
                    for (String user : ChatServer.userNames) {
                        if (clientMessage.equalsIgnoreCase(user)) {
                            checkPreviousVote();
                            ArrayList<String> current = ChatServer.userAndVotes.get(user);
                            current.add(userName);
                            ChatServer.userAndVotes.put(user, current);
                            correct = true;
                            break;
                        }
                    }
                    if (!correct) {
                        sendMessage("Invalid username, please try again.");
                    }
                } else if (ChatServer.state.equalsIgnoreCase("MAFIA")) {
                    server.notifyRole("MAFIA", serverMessage);
                    server.notifyRole("DOCTOR LECTRE", serverMessage);
                    server.notifyRole("GODFATHER", serverMessage);
                } else if (ChatServer.state.equalsIgnoreCase("GODFATHER") && role.equalsIgnoreCase("GODFATHER")) {
                    boolean correct = false;
                    for (String user : ChatServer.userNames) {
                        if (clientMessage.equalsIgnoreCase(user)) {
                            ChatServer.toBeSavedCitizen = user;
                            server.notifyRole("MAFIA", ConsoleColors.RED_BOLD + "CHOSEN CITIZEN: " + user + ConsoleColors.RESET);
                            server.notifyRole("DOCTOR LECTRE", ConsoleColors.RED_BOLD + "CHOSEN CITIZEN: " + user + ConsoleColors.RESET);
                            server.notifyRole("GODFATHER", ConsoleColors.RED_BOLD + "CHOSEN CITIZEN: " + user + ConsoleColors.RESET);
                            correct = true;
                            break;
                        }
                    }
                    if (!correct) {
                        sendMessage("Invalid username, please try again.");
                    } else ChatServer.state = "GODFATHER DONE";
                } else if (ChatServer.state.equalsIgnoreCase("DOCTOR LECTRE") && role.equalsIgnoreCase("DOCTOR LECTRE")) {
                    boolean correct = false;
                    for (String user : ChatServer.userNames) {
                        if (server.getRoleByUsername(clientMessage).equalsIgnoreCase("MAFIA") || server.getRoleByUsername(clientMessage).equalsIgnoreCase("GODFATHER")) {
                            ChatServer.SavedMafia = clientMessage;
                            server.notifyRole("DOCTOR LECTRE", ConsoleColors.GREEN_BOLD + "YOU HAVE COVERED: " + user + ConsoleColors.RESET);
                            correct = true;
                            break;
                        }
                    }
                    if (!correct) {
                        sendMessage("Invalid username, please try again.");
                    } else ChatServer.state = "DOCTOR LECTRE DONE";
                } else if (ChatServer.state.equalsIgnoreCase("DOCTOR") && role.equalsIgnoreCase("DOCTOR")) {
                    boolean correct = false;
                    for (String user : ChatServer.userNames) {
                        if (clientMessage.equalsIgnoreCase(user)) {
                            if (ChatServer.toBeSavedCitizen.equalsIgnoreCase(user))
                                ChatServer.toBeSavedCitizen = null;
                            server.notifyRole("DOCTOR", ConsoleColors.GREEN_BOLD + "YOU HAVE COVERED: " + user + ConsoleColors.RESET);
                            correct = true;
                            break;
                        }
                    }
                    if (!correct) {
                        sendMessage("Invalid username, please try again.");
                    } else ChatServer.state = "DOCTOR DONE";
                } else {
                    server.broadcast(serverMessage, null, null);
                }

            } while (!clientMessage.equals("bye"));

            server.removeUser(userName, this);
            socket.close();

            serverMessage = userName + " has quitted.";
            server.broadcast(serverMessage, this, null);

        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void checkPreviousVote() {
        for (String string : ChatServer.userNames) {
            if (ChatServer.userAndVotes.get(string).remove(userName))
                sendMessage("Vote changed.");
        }
    }

    /**
     * Sends a list of online users to the newly connected user.
     */
    void printUsers() {
        if (server.hasUsers()) {
            writer.println("Connected users: " + server.getUserNames());
        } else {
            writer.println("No other users connected");
        }
    }

    /**
     * Sends a message to the client.
     */
    void sendMessage(String message) {
        writer.println(message);
    }

    boolean isDuplicate(String userName) {
        List<String> usernames = server.getUserNames();
        for (String string : usernames)
            if (string.equalsIgnoreCase(userName))
                return true;
        return false;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean userIsAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public String getUserName() {
        return userName;
    }
}
