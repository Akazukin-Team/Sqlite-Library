package net.akazukin.database;

public class PrimaryKey extends Key {
    public PrimaryKey(String name, Type type) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = true;
        this.isNullable = false;
    }

    public PrimaryKey(String name, Type type, int length) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.isPrimaryKey = true;
        this.isNullable = false;
    }
}
