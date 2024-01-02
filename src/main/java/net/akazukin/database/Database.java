package net.akazukin.database;


import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Setter
public class Database {
    @Getter
    private final Path path;
    private String username = "Akazukin-Bot";
    private String password = "SQLite-By-Official";

    public Database(Path path) {
        this.path = path;
    }

    public Database(Path dbName, String username, String password) {
        this.path = dbName;
        this.username = username;
        this.password = password;
    }

    public boolean isClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return connection == null;
        }
    }

    public void close() {
        if (!isClosed()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        connection = null;
    }

    private Connection connection;
    public Connection getConnection() {
        return getConnection(false);
    }

    public Connection getConnection(boolean transMode) {
        try {
            try {
                Files.createDirectory(path.getParent());
            } catch (FileAlreadyExistsException ignored) {
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:///" + path, username, password);
            }
            connection.setAutoCommit(!transMode);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeUpdate(String sql) {
        executeUpdate(sql, false);
    }

    public void executeUpdate(String sql, boolean transMode) {
        Connection con = getConnection(transMode);
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultSet executeQuery(String sql) {
        try {
            return getConnection().createStatement().executeQuery(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DatabaseMetaData getMetaData() {
        try {
            Connection connection = getConnection();
            return connection.getMetaData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Table[] getTables() {
        List<Table> tables = new ArrayList<>();
        try (ResultSet rs = getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while(rs.next()) {
                tables.add(getTable(rs.getString("TABLE_NAME")));
            }
            return tables.toArray(new Table[]{});
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Table getTable(String table) {
        return new Table(this, table);
    }
}
