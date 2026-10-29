package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Reads settings (database, Gmail) from config.properties so passwords are not hard-coded. */
public class Config {
    private static final Properties props = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            System.out.println("Could not read config.properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "").trim();
    }
}
