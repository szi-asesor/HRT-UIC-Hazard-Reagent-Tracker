package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import util.Config;

/** Opens a JDBC connection to MySQL using the settings in config.properties. */
public class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.get("db.url"), Config.get("db.user"), Config.get("db.password"));
    }
}
