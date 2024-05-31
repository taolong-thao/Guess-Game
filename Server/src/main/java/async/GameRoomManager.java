package async;


import java.util.HashMap;
import java.util.Map;

public class GameRoomManager {
    private final Map<String, GameRoom> gameRooms;

    public GameRoomManager() {
        this.gameRooms = new HashMap<>();
    }

    public synchronized void addGameRoom(String roomId, GameRoom gameRoom) {
        gameRooms.put(roomId, gameRoom);
    }

    public synchronized GameRoom findGameRoomById(String roomId) {
        return gameRooms.get(roomId);
    }

    public synchronized GameRoom deleteGameRoomById(String roomId) {
        return gameRooms.remove(roomId);
    }
}
