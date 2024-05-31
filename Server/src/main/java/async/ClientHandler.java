package async;

import connection.ConnectorManager;
import org.json.JSONException;
import org.json.JSONObject;
import repository.AccountRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final GameRoomManager gameRoomManager;
    private GameRoom gameRoom;
    private String username;
    public static int userId;
    private String roomCache;
    public int attemptsLeft;
    AccountRepository repository = new AccountRepository(ConnectorManager.getConnection());

    public ClientHandler(Socket socket, GameRoomManager gameRoomManager) {
        this.socket = socket;
        this.gameRoomManager = gameRoomManager;
        ConnectorManager.connection();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            while (true) {
                String clientMessage = in.readLine();
                if (clientMessage == null) break;

                JSONObject getRequest = new JSONObject(clientMessage);
                String action = getRequest.getString("action");
                if ("register".equals(action)) {
                    if (!registerAccount(out, getRequest)) {
                        continue;
                    }
                } else if ("login".equals(action)) {
                    if (!handleLogin(out, getRequest)) {
                        continue;
                    }
                    String modeMessage = in.readLine();
                    JSONObject modeRequest = new JSONObject(modeMessage);
                    int mode = modeRequest.getInt("mode");

                    if (mode == 1) {
                        startSinglePlayerGame(out);
                        System.out.println("Mode - Single Player");
                    } else if (mode == 2) {
                        startMultiplayerGame(in, out);
                        System.out.println("Mode - MultiPlayer");
                    } else {
                        out.println(new JSONObject().put("status", "failure").put("message", "Bye."));
                        socket.close();
                        return;
                    }
                    // Game loop
                    loopGuess(in);
                } else if ("mode".equals(action)) {
                    int mode = getRequest.getInt("mode");
                    if (mode == 1) {
                        startSinglePlayerGame(out);
                        System.out.println("Mode - Single Player");
                    } else if (mode == 2) {
                        startMultiplayerGame(in, out);
                        System.out.println("Room ID - " + roomCache);
                        System.out.println("Mode - MultiPlayer");
                    } else {
                        out.println(new JSONObject().put("status", "failure").put("message", "Bye."));
                        socket.close();
                        return;
                    }
                    // Game loop
                    loopGuess(in);
                } else {
                    out.println(new JSONObject().put("status", "failure").put("message", "Invalid action."));
                }
            }

        } catch (IOException | SQLException | JSONException e) {
            System.out.println("Client - " + username + " Disconnected.");
            try {
                repository.markUserInactive(userId);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void loopGuess(BufferedReader in) throws IOException, JSONException {
        while (true) {
            String msgInGame = in.readLine();
            if (msgInGame == null) break;

            JSONObject guessRequest = new JSONObject(msgInGame);
            if ("guess".equals(guessRequest.getString("action"))) {
                String guess = guessRequest.getString("guess");
                if (gameRoom.handleGuess(guess, this)) {
                    break;
                }
            }
        }
        if (gameRoom.clients.isEmpty() && !gameRoom.flagCorrect) {
            System.out.println("No client guessed correctly ");
            gameRoomManager.deleteGameRoomById(roomCache);
        }
    }

    private boolean handleLogin(PrintWriter out, JSONObject loginRequest) throws IOException, SQLException, JSONException {
        JSONObject loginBody = loginRequest.getJSONObject("body");
        String email = loginBody.getString("email");
        String password = loginBody.getString("password");

        if (repository.authenticateUser(email, password)) {
            username = email;
            System.out.println("Client - " + email + " Connected.");
            out.println(new JSONObject().put("status", "success").put("message", "Login successful. Welcome " + username + "!"));
            return true;
        } else {
            out.println(new JSONObject().put("status", "failure").put("message", "User logged!."));
            return false;
        }
    }

    public boolean registerAccount(PrintWriter out, JSONObject loginRequest) throws IOException, JSONException, SQLException {
        JSONObject loginBody = loginRequest.getJSONObject("body");
        String email = loginBody.getString("email");
        String password = loginBody.getString("password");
        if (repository.checkExist(email)) {
            out.println(new JSONObject().put("status", "failure").put("message", "Account exists"));
            return false;
        } else if (repository.insert(email, password)) {
            out.println(new JSONObject().put("status", "success").put("message", "Account created"));
            return true;
        }
        return false;
    }


    private void startSinglePlayerGame(PrintWriter out) throws SQLException, JSONException {
        String randomWord = repository.getRandomWord(userId);
        if (randomWord == null) {
            randomWord = "default";
        }
        System.out.println("Word - " + randomWord.toUpperCase());
        gameRoom = new GameRoom(randomWord, 3);
        attemptsLeft = 3;
        gameRoom.addClient(this);
        out.println(new JSONObject()
                .put("status", "success")
                .put("message", "You have " + gameRoom.getAttemptsLeft() + " attempts to guess the word. Word suggest - " + randomWord.charAt(0) + "_".repeat(randomWord.length() - 1))
                .put("word", "_".repeat(randomWord.length())));
    }

    private void startMultiplayerGame(BufferedReader in, PrintWriter out) throws IOException, SQLException, JSONException {
        out.println(new JSONObject().put("status", "success").put("message", "Enter room ID:"));
        String roomIdMessage = in.readLine();
        JSONObject roomIdRequest = new JSONObject(roomIdMessage);
        String roomId = roomIdRequest.getString("roomId");
        synchronized (gameRoomManager) {
            int size = 1;
            gameRoom = gameRoomManager.findGameRoomById(roomId);
            if (gameRoom == null) {
                roomCache = roomId;
                String randomWord = repository.getRandomWord(userId);
                if (randomWord == null) {
                    randomWord = "default";
                }
                System.out.println("Word - " + randomWord.toUpperCase());
                gameRoom = new GameRoom(randomWord, 3);
                size = gameRoom.clients.size();
                System.out.println("Member - " + size);
                gameRoomManager.addGameRoom(roomId, gameRoom);
                out.println(new JSONObject()
                        .put("status", "success")
                        .put("message", "Room created. You have " + gameRoom.getAttemptsLeft() + " attempts to guess the word. Word suggest - " + randomWord.charAt(0) + "_".repeat(randomWord.length() - 1))
                        .put("word", "_".repeat(randomWord.length())));
            } else {
                out.println(new JSONObject().put("status", "success").put("message", "Joined existing room.").put("word", gameRoom.getWord()));
                System.out.println("1 Member Joined. All Member - " + String.valueOf(size + 1));
            }
            attemptsLeft = 3;
        }
        gameRoom.addClient(this);
    }

    public void sendMessage(String message) throws IOException, JSONException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(new JSONObject().put("message", message).put("status", "success").put("word", gameRoom.getWord().toUpperCase()));
        out.flush();
    }

    public String getUsername() {
        return username;
    }

}
