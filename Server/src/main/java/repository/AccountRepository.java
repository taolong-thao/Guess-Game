package repository;

import async.ClientHandler;
import connection.ConnectorManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class AccountRepository {
    private Connection connection;
    public int userId;

    public AccountRepository(Connection connection) {
        this.connection = connection;
        if (this.connection == null) {
            ConnectorManager.connection();
            this.connection = ConnectorManager.getConnection();
        }
    }

    public boolean checkExist(String email) throws SQLException {
        try (
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    public boolean insert(String email, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (email, password) VALUES (?, ?)");
            statement.setString(1, email);
            statement.setString(2, password);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean authenticateUser(String email, String password) {
        if (connection == null) {
            ConnectorManager.connection();
            connection = ConnectorManager.getConnection();
        }
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id, password FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getString("password").equals(password)) {
                userId = rs.getInt("id");
                if (!isUserActive(connection, userId)) {
                    markUserActive(connection, userId);
                } else {
                    return false;
                }
                ClientHandler.userId = userId;
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isUserActive(Connection connection, int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT active FROM sessions WHERE user_id = ? AND active = TRUE");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    private void markUserActive(Connection connection, int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("REPLACE INTO sessions (user_id, active) VALUES (?, TRUE)");
        stmt.setInt(1, userId);
        stmt.executeUpdate();
    }

    public void markUserInactive(int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("UPDATE sessions SET active = FALSE WHERE user_id = ?");
        stmt.setInt(1, userId);
        stmt.executeUpdate();
    }

    public String getRandomWord(int userId) {
        if (connection == null) {
            ConnectorManager.connection();
            connection = ConnectorManager.getConnection();
        }
        String word = null;
        try {
            // Get words used in the last 3 games
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT word_id FROM game_history WHERE user_id = ? ORDER BY played_at DESC LIMIT 3");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            Set<Integer> recentWords = new HashSet<>();
            while (rs.next()) {
                recentWords.add(rs.getInt("word_id"));
            }

            // Get a random word that is not in the recentWords set
            String sql = "SELECT id, word FROM words";
            if (!recentWords.isEmpty()) {
                sql += " WHERE id NOT IN (" + recentWords.toString().replace("[", "").replace("]", "") + ")";
            }
            sql += " ORDER BY RAND() LIMIT 1";
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                int wordId = rs.getInt("id");
                word = rs.getString("word");
                // Record the selected word in the game history
                stmt = connection.prepareStatement("INSERT INTO game_history (user_id, word_id) VALUES (?, ?)");
                stmt.setInt(1, userId);
                stmt.setInt(2, wordId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return word;
    }
}
