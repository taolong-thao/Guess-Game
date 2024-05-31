import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

import static java.lang.System.out;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {


        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             Scanner consoleIn = new Scanner(System.in)) {

            while (true) {
                System.out.println("Welcome! Please choose an option:");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                String choice = consoleIn.nextLine();

                switch (choice) {
                    case "1":
                        register(consoleIn, out, in);
                        break;
                    case "2":
                        Boolean loggedIn = getLoggedIn(consoleIn, out, in);
                        if (loggedIn == null) return;
                        if (!loggedIn) {
                            System.out.println("Failed to log in. Exiting.");
                            return;
                        }
                        startGame(in, consoleIn, out);
                        break;
                    case "3":
                        System.out.println("Exiting the program. Goodbye!");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }


        } catch (IOException | JSONException e) {
            out.println("Client error: " + e.getMessage());
        }
    }

    private static void startGame(BufferedReader in, Scanner consoleIn, BufferedWriter out) throws IOException, JSONException {
        boolean check = true;
        while (check) {
            String serverMessage;
            System.out.println("Choose game mode: 1.Single Player  2.Multiplayer  3.No Thanks!");
            System.out.print("Enter your choice: ");
            int choice = consoleIn.nextInt();
            consoleIn.nextLine(); // Consume newline
            JSONObject modeRequest = new JSONObject();
            modeRequest.put("action", "mode");
            modeRequest.put("mode", choice);
            out.write(modeRequest.toString() + "\n");
            out.flush();

            while ((serverMessage = in.readLine()) != null) {
                JSONObject response = new JSONObject(serverMessage);
                System.out.println("Server: " + response.getString("message"));

                if (!response.getString("status").equals("success")) {
                    System.out.println("Game over. Exiting.");
                    System.exit(0);
                    break;
                }
                if (response.getString("message").startsWith("Congratulations") || response.getString("message").startsWith("Game over")) {
                    break;
                }
                if (response.getString("message").startsWith("Enter room ID") && response.getString("status").equals("success")) {
                    String roomId = consoleIn.nextLine();
                    JSONObject roomRequest = new JSONObject();
                    roomRequest.put("roomId", roomId);
                    out.write(roomRequest.toString() + "\n");
                    out.flush();
                }
                if (response.has("word")) {
                    System.out.print("Enter your guess: ");
                    String guess = consoleIn.nextLine();
                    JSONObject guessRequest = new JSONObject();
                    guessRequest.put("action", "guess");
                    guessRequest.put("guess", guess);
                    out.write(guessRequest.toString() + "\n");
                    out.flush();
                }
            }
        }
    }

    private static Boolean getLoggedIn(Scanner consoleIn, BufferedWriter out, BufferedReader in) throws JSONException, IOException {
        System.out.print("Enter username: ");
        String username = consoleIn.nextLine();
        System.out.print("Enter password: ");
        String password = consoleIn.nextLine();

        // Login
        JSONObject loginRequest = new JSONObject();
        loginRequest.put("action", "login");
        JSONObject loginBody = new JSONObject();
        loginBody.put("email", username);
        loginBody.put("password", password);
        loginRequest.put("body", loginBody);

        out.write(loginRequest.toString() + "\n");
        out.flush();

        String serverMessage;
        boolean loggedIn = false;
        while ((serverMessage = in.readLine()) != null) {
            System.out.println("Server: " + serverMessage);
            JSONObject response = new JSONObject(serverMessage);

            if (response.getString("status").equals("failure")) {
                System.out.println("Login failed. Exiting.");
                return null;
            }

            if (response.getString("status").equals("success") && response.has("message")) {
                System.out.println("Server: " + response.getString("message"));
                if (response.getString("message").startsWith("Login successful")) {
                    loggedIn = true;
                    break;
                }
            }
        }
        return loggedIn;
    }

    private static void register(Scanner consoleIn, BufferedWriter out, BufferedReader in) throws JSONException, IOException {
        boolean check = true;
        while (check) {
            System.out.print("Enter username: ");
            String username = consoleIn.nextLine();
            System.out.print("Enter password: ");
            String password = consoleIn.nextLine();

            JSONObject registRequest = new JSONObject();
            registRequest.put("action", "register");
            JSONObject loginBody = new JSONObject();
            loginBody.put("email", username);
            loginBody.put("password", password);
            registRequest.put("body", loginBody);

            out.write(registRequest.toString() + "\n"); // Using println instead of write + "\n"
            out.flush();

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                System.out.println("Server: " + serverMessage);
                JSONObject response = new JSONObject(serverMessage);

                if ("failure".equals(response.getString("status"))) {
                    check = true;
                    break;
                } else if ("success".equals(response.getString("status")) && response.has("message")) {
                    System.out.println("Server: " + response.getString("message"));
                    check = false;
                    break;
                }
            }
        }
    }
}
