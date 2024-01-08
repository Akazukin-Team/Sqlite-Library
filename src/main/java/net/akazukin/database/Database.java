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

    public Database(final Path path) {
        this.path = path;
    }

    public Database(final Path dbName, final String username, final String password) {
        this.path = dbName;
        this.username = username;
        this.password = password;
    }

    public boolean isClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (final SQLException e) {
            e.printStackTrace();
            return connection == null;
        }
    }

    public void close() {
        if (!isClosed()) {
            try {
                connection.close();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        connection = null;
    }

    private Connection connection;
    public Connection getConnection() {
        return getConnection(false);
    }

    public Connection getConnection(final boolean transMode) {
        try {
            try {
                Files.createDirectory(path.getParent());
            } catch (final FileAlreadyExistsException ignored) {
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            if (isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:///" + path, username, password);
            }
            connection.setAutoCommit(!transMode);
            return connection;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeUpdate(final String sql) {
        executeUpdate(sql, false);
    }

    public void executeUpdate(final String sql, final boolean transMode) {
        final Connection con = getConnection(transMode);
        try (final Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResultSet executeQuery(final String sql) {
        try {
            return getConnection().createStatement().executeQuery(sql);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DatabaseMetaData getMetaData() {
        try {
            final Connection connection = getConnection();
            return connection.getMetaData();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Table[] getTables() {
        final List<Table> tables = new ArrayList<>();
        try (final ResultSet rs = getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while(rs.next()) {
                tables.add(getTable(rs.getString("TABLE_NAME")));
            }
            return tables.toArray(new Table[]{});
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Table getTable(final String table) {
        return new Table(this, table);
    }
}
