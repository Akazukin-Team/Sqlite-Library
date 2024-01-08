package net.akazukin.database;


import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
public class Table {
    private final Database database;
    private final String table;
    public Table(final Database database, final String table) {
        this.database = database;
        this.table = table;
    }

    public void create(final Key... keys) {
        if (Arrays.stream(keys).noneMatch(Key::isPrimaryKey))
            throw new IllegalArgumentException("Primary key was not found");
        if (tableExists()) {
            final Key[] keys2 = getKey();

            Arrays.stream(keys).forEach(key -> {
                if (!Arrays.asList(keys2).contains(key)) {
                    addKey(key);
                }
            });
            Arrays.stream(keys2).forEach(key -> {
                if (!Arrays.asList(keys).contains(key)) {
                    removeKeyValue(key);
                }
            });
        } else {
            final List<String> primaryKeys = new ArrayList<>();
            final List<String> keys2 = new ArrayList<>();
            for (final Key key : keys) {
                String key2 = key.getName() + " " + key.getType().getName();
                if (key.getLength() != 0) key2 += "(" + key.getLength() + ")";
                if (!key.isNullable()) key2 += " NOT NULL";
                keys2.add(key2);

                if (key.isPrimaryKey()) primaryKeys.add(key.getName());
            }
            if (primaryKeys.isEmpty()) {
                throw new IllegalArgumentException("Cannot create table with no primary keys.");
            }
            keys2.add("PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");

            database.executeUpdate("CREATE TABLE " + table + "(" + String.join(", ", keys2) + ");");
        }
    }

    public boolean tableExists() {
        try (final ResultSet tables = database.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (tables.getString("TABLE_NAME").equals(table)) return true;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public PrimaryKey[] getPrimaryKey() {
        return Arrays.stream(getKey()).filter(column -> column.isPrimaryKey).toArray(PrimaryKey[]::new);
    }

    public Key[] getKey() {
        try (final ResultSet rs = database.executeQuery("PRAGMA table_info(" + table + ");")) {
            final List<Key> keys = new ArrayList<>();

            while (rs.next()) {
                final String type = rs.getString("type");
                final Key.Type type2 = Arrays.stream(Key.Type.values()).filter(value2 -> value2.getName().equalsIgnoreCase(type.contains("(") ? type.split("\\(")[0] : type)).findFirst().get();
                final int length = (type.contains("(") ? Integer.parseInt(type.split("\\(")[1].replace(")", "")) : -1);
                final Object dflt_value = rs.getObject("dflt_value");
                final boolean notNull = rs.getInt("notnull") == 0;

                if (rs.getInt("pk") != 0) {
                    keys.add(new PrimaryKey(rs.getString("name"), type2, length));
                } else {
                    keys.add(new Column(rs.getString("name"), type2, length, !notNull, dflt_value));
                }
            }
            return keys.toArray(new Key[0]);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Column[] getColumn() {
        return Arrays.stream(getKey()).filter(column -> !column.isPrimaryKey).toArray(Column[]::new);
    }

    private void setData(final Object[] primaryKeys, final Column[] columns, final Object[] values) {
        setData(primaryKeys, columns, values, false);
    }

    private void setData(final Object[] primaryKeys, final Column[] columns, final Object[] values, final boolean transaction) {
        final PrimaryKey[] primaryKeys2 = getPrimaryKey();

        if (primaryKeys.length != primaryKeys2.length) throw new RuntimeException("Keys length isn't SQL keys length");
        if (columns.length != values.length) throw new RuntimeException("Values length isn't value keys length");

        if (isExists(primaryKeys2, primaryKeys)) {
            final List<String> keyStrs = new ArrayList<>();
            for (int i = 0; i < primaryKeys.length; i++) {
                final Object primaryKey = primaryKeys[i];
                final PrimaryKey primaryKey2 = primaryKeys2[i];
                if (!DatabaseUtils.validKey(primaryKey2.getType(), primaryKey))
                    throw new RuntimeException("Key is invalid (" + primaryKey + ")");

                final boolean keyStrCheck = primaryKey2.getType() == Key.Type.String;
                keyStrs.add(primaryKey2.getName() + " = " + (keyStrCheck ? "'" : "") + primaryKey + (keyStrCheck ? "'" : ""));
            }

            final List<String> valueStrs = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                final Object value = values[i];
                final Column value2 = columns[i];
                if (!DatabaseUtils.validValue(value2.getType(), value))
                    throw new RuntimeException("Value is invalid (" + value + ")");

                final boolean strCheck = value2.getType() == Key.Type.String;
                valueStrs.add(value2.getName() + " = " + (strCheck ? "'" : "") + value + (strCheck ? "'" : ""));
            }

            database.executeUpdate("UPDATE " + table + " SET " + String.join(", ", valueStrs) + " WHERE " + String.join(" AND " , keyStrs) + ";", transaction);
        } else {
            final List<Key> keys_ = new ArrayList<>(Arrays.asList(primaryKeys2));
            keys_.addAll(Arrays.asList(columns));

            final List<Object> values_ = new ArrayList<>(Arrays.asList(primaryKeys));
            values_.addAll(Arrays.asList(values));

            final List<String> keyStrs = new ArrayList<>();
            final List<String> valueStrs = new ArrayList<>();
            for (int i = 0; i < keys_.size(); i++) {
                final Key keyColumn = keys_.get(i);
                final boolean keyStrCheck = keyColumn.getType() == Key.Type.String;
                keyStrs.add(keyColumn.getName());
                valueStrs.add((keyStrCheck ? "'" : "") + values_.get(i) + (keyStrCheck ? "'" : ""));
            }

            database.executeUpdate("insert into " + table + "(" + String.join(", ", keyStrs) + ") values(" + String.join(", ", valueStrs) + ");", transaction);
        }
    }

    public void setData(final Object[] primaryKeys, final Object key2, final Object value, final boolean transaction) {
        setData(primaryKeys, Arrays.stream(getKey()).filter(key_ -> !key_.isPrimaryKey && key_.getName().equalsIgnoreCase(String.valueOf(key2))).toArray(Column[]::new), new Object[]{value}, transaction);
    }

    public void setData(final Object[] primaryKeys, final Object key2, final Object value) {
        setData(primaryKeys, Arrays.stream(getKey()).filter(key_ -> !key_.isPrimaryKey && key_.getName().equalsIgnoreCase(String.valueOf(key2))).toArray(Column[]::new), new Object[]{value}, false);
    }

    public boolean isExists(final Object... values) {
        return isExists(getPrimaryKey(), values);
    }
    public boolean isExists(final Key[] keys, final Object... values) {
        if (keys.length != values.length) throw new RuntimeException("Key length isn't values length");

        final List<String> sqlStrs = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            final Key key = keys[i];
            final Object value = values[i];

            final boolean strCheck = key.getType() == Key.Type.String;
            sqlStrs.add(key.getName() + " = " + (strCheck ? "'" : "") + value + (strCheck ? "'" : ""));
        }

        try (final ResultSet rs = database.executeQuery("SELECT EXISTS(SELECT * FROM " + table +  " WHERE " + String.join(" AND ", sqlStrs) + ");")) {
            return rs.getInt(1) == 1;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<Column, Object> getWithColumns(final Object... primaryKeys) {
        final PrimaryKey[] primaryKeys2 = getPrimaryKey();
        if (primaryKeys.length != primaryKeys2.length) throw new RuntimeException("Keys length isn't SQL keys length");

        String sqlStr = "";
        for (int i = 0; i < primaryKeys.length; i++) {
            final PrimaryKey primaryKey = primaryKeys2[i];
            final Object primaryKey2 = primaryKeys[i];

            final boolean strCheck = primaryKey.getType() == Key.Type.String;
            sqlStr += primaryKey.getName() + " = " + (strCheck ? "'" : "") + primaryKey2 + (strCheck ? "'" : "");

            if (primaryKeys.length > i + 1) sqlStr += " AND ";
        }

        final Column[] columns = getColumn();
        final HashMap<Column, Object> values = new HashMap<>();
        try (final ResultSet rs = database.executeQuery("SELECT * FROM " + table + " WHERE " + sqlStr)) {
            while (rs.next()) {
                for (final Column column : columns) {
                    values.put(column, rs.getObject(column.getName()));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return values;
    }

    public HashMap<String, Object> get(final Object... primaryKeys) {
        final HashMap<String, Object> values = new HashMap<>();
        getWithColumns(primaryKeys).forEach((column, o) -> values.put(column.getName(), o));
        return values;
    }

    public void addKey(final Key key) {
        if (Arrays.stream(getKey()).anyMatch(key_ ->
                Objects.equals(key_.getName(), key.getName()) &&
                        key_.getType() == key.getType() &&
                        key_.getLength() == key.getLength() &&
                        key_.isNullable() == key.isNullable() &&
                        Objects.equals(key_.getDefault(), key.getDefault())
        )) {
            String sql;
            if (key.isPrimaryKey()) {
                //primary keyにする
                database.executeUpdate("ALTER TABLE " + table + " ADD CONSTRAINT "+ table + "_pk PRIMARY KEY (" + key.getName() + ");");
            } else {
                //columnにする
                database.executeUpdate("ALTER TABLE " + table + " DROP CONSTRAINT " + key.getName() + ";");
                database.executeUpdate("ALTER TABLE " + table + " ALTER COLUMN " + key.getName() + " DROP NOT NULL");
            }
        } else {
            String sql = "ALTER TABLE " + table + " ADD " +
                    (key.isPrimaryKey() ? "PRIMARY KEY" : "COLUMN") + "(" +
                    key.getName() + " " +
                    key.getType().getName() + (key.getLength() == 0 ? "" : "(" + key.getLength() + ")");
            if (!key.isNullable()) sql += " NOT NULL";
            if (key.getDefault() != null) {
                sql += " DEFAULT ";
                if (key.getDefault() instanceof String) sql += "\"";
                sql += key.getDefault();
                if (key.getDefault() instanceof String) sql += "\"";
            }
            sql += ");";
            System.out.println(sql);
            database.executeUpdate(sql);
        }

    }
    public void removeKeyValue(final Key key) {
        final String sql = "ALTER TABLE " + table + " DROP " +
                (key.isPrimaryKey() ? "PRIMARY KEY " : "COLUMN ") +
                key.getName() + ";";
        database.executeUpdate(sql);
    }
    public void removeKey(final PrimaryKey[] primaryKeys, final Object[] value) {
        String sql = "DELETE FROM " + table + " WHERE ";

        final PrimaryKey[] primaryKeys2 = getPrimaryKey();
        if (primaryKeys.length != primaryKeys2.length) throw new IllegalArgumentException("Keys length isn't SQL keys length");

        String keyStr = "";
        for (int i = 0; i < primaryKeys.length; i++) {
            final PrimaryKey primaryKey = primaryKeys[i];
            final PrimaryKey primaryKey2 = primaryKeys2[i];
            if (!DatabaseUtils.validKey(primaryKey2.getType(), primaryKey))
                throw new RuntimeException("Key is invalid (" + primaryKey + ")");

            final boolean keyStrCheck = primaryKey2.getType() == Key.Type.String;
            keyStr += primaryKey2.getName() + " = " + (keyStrCheck ? "'" : "") + primaryKey + (keyStrCheck ? "'" : "");
            if (primaryKeys.length > i + 1) keyStr += " AND ";
        }
        sql += keyStr + ";";

        database.executeUpdate(sql);
    }
}
