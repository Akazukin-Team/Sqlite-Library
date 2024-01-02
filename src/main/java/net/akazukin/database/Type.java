package net.akazukin.database;

public interface Type {
    Column.Type valueOf(String value);
}
