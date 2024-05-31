package connection;


import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @Author: 18600355@student.hcmus.edu.vn - Tran Phi Long
 */
public class ConnectorManager {
    static Properties config = ConfigManagement.getConfig();

    public static Connection connection = null;


    public static void connection() {
        try {
            connection =  DriverManager.getConnection(config.getProperty("db.url"),
                    config.getProperty("db.user"),
                    config.getProperty("db.password"));
        } catch (SQLException e) {
            System.out.printf("connected...Error");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}
