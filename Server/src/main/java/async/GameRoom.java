package async;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    public final String word;
    //    private final int maxAttempts;
    public int attemptsLeft;
    public final List<ClientHandler> clients = new ArrayList<>();

    public boolean flagCorrect = false;

    public GameRoom(String word, int maxAttempts) {
        this.word = word;
        this.attemptsLeft = maxAttempts;
    }

    public synchronized void addClient(ClientHandler client) {
        clients.add(client);
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public synchronized boolean handleGuess(String guess, ClientHandler client) throws IOException, JSONException {
        if (guess.equalsIgnoreCase(word)) {
            for (ClientHandler ch : clients) {
                ch.sendMessage("Congratulations! The word was " + word.toUpperCase() + ". " + client.getUsername() + " guessed it correctly.");
                System.out.println(client.getUsername() + " guessed it correctly." + " Game end!");
            }
            clients.clear();
            flagCorrect = true;
            return true;
        } else {
            client.attemptsLeft--;
            if (client.attemptsLeft <= 0) {
                client.sendMessage("Game over! The word was " + word.toUpperCase() + ".");
                System.out.println("Client " + client.getUsername() + " Close!");
                removeClient(client);
                return true;
            } else {
                client.sendMessage("Incorrect guess. Attempts left: " + client.attemptsLeft);
            }
        }
        return false;
    }

    public String getWord() {
        return word;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }
}
