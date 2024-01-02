package net.akazukin.database;

public class DatabaseUtils {
    public static boolean validKey(Object key) {
        return key instanceof Number || key instanceof String;
    }

    public static boolean validKey(Key.Type type, Object key) {
        return type == Key.Type.String || (type == Key.Type.Integer && (key instanceof Integer || key instanceof Long));
    }

    public static boolean validValue(Key.Type type, Object key) {
        return type == Key.Type.Binary || (type == Key.Type.Integer && (key instanceof Integer || key instanceof Long)) || (type == Key.Type.Number && key instanceof Number) || (type == Key.Type.Double && (key instanceof Float || key instanceof Double)) || (type == Key.Type.String && (key instanceof Integer || key instanceof Long || key instanceof String));
    }

    public static boolean validValue(Object value) {
        return value instanceof Number || value instanceof String || value instanceof Boolean;
    }
}
