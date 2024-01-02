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
    final Database database;
    final String table;
    public Table(Database database, String table) {
        this.database = database;
        this.table = table;
    }

    public void create(Key... keys) {
        if (tableExists()) {
            Key[] keys2 = getKey();

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
            List<String> primaryKeys = new ArrayList<>();
            List<String> keys2 = new ArrayList<>();
            for (Key key : keys) {
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
        try (ResultSet tables = database.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (tables.getString("TABLE_NAME").equals(table)) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public PrimaryKey[] getPrimaryKey() {
        return Arrays.stream(getKey()).filter(column -> column.isPrimaryKey).toArray(PrimaryKey[]::new);
    }

    public Key[] getKey() {
        try (ResultSet rs = database.executeQuery("PRAGMA table_info(" + table + ");")) {
            List<Key> keys = new ArrayList<>();

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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Column[] getColumn() {
        return Arrays.stream(getKey()).filter(column -> !column.isPrimaryKey).toArray(Column[]::new);
    }

    private void setData(Object[] primaryKeys, Column column, Object value) {
        setData(new Object[]{primaryKeys}, new Column[]{column}, new Object[]{value});
    }

    private void setData(Object[] primaryKeys, Column[] columns, Object... values) {
        PrimaryKey[] primaryKeys2 = getPrimaryKey();

        if (primaryKeys.length != primaryKeys2.length) throw new RuntimeException("Keys length isn't SQL keys length");
        if (columns.length != values.length) throw new RuntimeException("Values length isn't value keys length");

        if (isExists(primaryKeys2, primaryKeys)) {
            List<String> keyStrs = new ArrayList<>();
            for (int i = 0; i < primaryKeys.length; i++) {
                Object primaryKey = primaryKeys[i];
                PrimaryKey primaryKey2 = primaryKeys2[i];
                if (!DatabaseUtils.validKey(primaryKey2.getType(), primaryKey))
                    throw new RuntimeException("Key is invalid (" + primaryKey + ")");

                boolean keyStrCheck = primaryKey2.getType() == Key.Type.String;
                keyStrs.add(primaryKey2.getName() + " = " + (keyStrCheck ? "'" : "") + primaryKey + (keyStrCheck ? "'" : ""));
            }

            List<String> valueStrs = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                Column value2 = columns[i];
                if (!DatabaseUtils.validValue(value2.getType(), value))
                    throw new RuntimeException("Value is invalid (" + value + ")");

                boolean strCheck = value2.getType() == Key.Type.String;
                valueStrs.add(value2.getName() + " = " + (strCheck ? "'" : "") + value + (strCheck ? "'" : ""));
            }

            database.executeUpdate("UPDATE " + table + " SET " + String.join(", ", valueStrs) + " WHERE " + String.join(" AND " , keyStrs) + ";");
        } else {
            List<Key> keys_ = new ArrayList<>(Arrays.asList(primaryKeys2));
            keys_.addAll(Arrays.asList(columns));

            List<Object> values_ = new ArrayList<>(Arrays.asList(primaryKeys));
            values_.addAll(Arrays.asList(values));

            List<String> keyStrs = new ArrayList<>();
            List<String> valueStrs = new ArrayList<>();
            for (int i = 0; i < keys_.size(); i++) {
                Key keyColumn = keys_.get(i);
                boolean keyStrCheck = keyColumn.getType() == Key.Type.String;
                keyStrs.add(keyColumn.getName());
                valueStrs.add((keyStrCheck ? "'" : "") + values_.get(i) + (keyStrCheck ? "'" : ""));
            }

            database.executeUpdate("insert into " + table + "(" + String.join(", ", keyStrs) + ") values(" + String.join(", ", valueStrs) + ");", false);
        }
    }

    public void set(Object[] primaryKeys, Object... values) {
        setData(primaryKeys, getColumn(), values);
    }

    public void setData(Object[] primaryKeys, Object key2, Object value) {
        setData(primaryKeys, Arrays.stream(getKey()).filter(key_ -> !key_.isPrimaryKey && key_.getName().equalsIgnoreCase(String.valueOf(key2))).toArray(Column[]::new), new Object[]{value});
    }

    public boolean isExists(Object... values) {
        return isExists(getPrimaryKey(), values);
    }
    public boolean isExists(Key[] keys, Object... values) {
        if (keys.length != values.length) throw new RuntimeException("Key length isn't values length");

        List<String> sqlStrs = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            Key key = keys[i];
            Object value = values[i];

            boolean strCheck = key.getType() == Key.Type.String;
            sqlStrs.add(key.getName() + " = " + (strCheck ? "'" : "") + value + (strCheck ? "'" : ""));
        }

        try (ResultSet rs = database.executeQuery("SELECT EXISTS(SELECT * FROM " + table +  " WHERE " + String.join(" AND ", sqlStrs) + ");")) {
            return rs.getInt(1) == 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<Column, Object> getWithColumns(Object... primaryKeys) {
        PrimaryKey[] primaryKeys2 = getPrimaryKey();
        if (primaryKeys.length != primaryKeys2.length) throw new RuntimeException("Keys length isn't SQL keys length");

        String sqlStr = "";
        for (int i = 0; i < primaryKeys.length; i++) {
            PrimaryKey primaryKey = primaryKeys2[i];
            Object primaryKey2 = primaryKeys[i];

            boolean strCheck = primaryKey.getType() == Key.Type.String;
            sqlStr += primaryKey.getName() + " = " + (strCheck ? "'" : "") + primaryKey2 + (strCheck ? "'" : "");

            if (primaryKeys.length > i + 1) sqlStr += " AND ";
        }

        Column[] columns = getColumn();
        HashMap<Column, Object> values = new HashMap<>();
        try (ResultSet rs = database.executeQuery("SELECT * FROM " + table + " WHERE " + sqlStr)) {
            while (rs.next()) {
                for (Column column : columns) {
                    values.put(column, rs.getObject(column.getName()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }

    public HashMap<String, Object> get(Object... primaryKeys) {
        HashMap<String, Object> values = new HashMap<>();
        getWithColumns(primaryKeys).forEach((column, o) -> values.put(column.getName(), o));
        return values;
    }

    public void addKey(Key key) {
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
    public void removeKeyValue(Key key) {
        String sql = "ALTER TABLE " + table + " DROP " +
                (key.isPrimaryKey() ? "PRIMARY KEY " : "COLUMN ") +
                key.getName() + ";";
        database.executeUpdate(sql);
    }
    public void removeKey(PrimaryKey[] primaryKeys, Object[] value) {
        String sql = "DELETE FROM " + table + " WHERE ";

        PrimaryKey[] primaryKeys2 = getPrimaryKey();
        if (primaryKeys.length != primaryKeys2.length) throw new IllegalArgumentException("Keys length isn't SQL keys length");

        String keyStr = "";
        for (int i = 0; i < primaryKeys.length; i++) {
            PrimaryKey primaryKey = primaryKeys[i];
            PrimaryKey primaryKey2 = primaryKeys2[i];
            if (!DatabaseUtils.validKey(primaryKey2.getType(), primaryKey))
                throw new RuntimeException("Key is invalid (" + primaryKey + ")");

            boolean keyStrCheck = primaryKey2.getType() == Key.Type.String;
            keyStr += primaryKey2.getName() + " = " + (keyStrCheck ? "'" : "") + primaryKey + (keyStrCheck ? "'" : "");
            if (primaryKeys.length > i + 1) keyStr += " AND ";
        }
        sql += keyStr + ";";

        database.executeUpdate(sql);
    }
}
