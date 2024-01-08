package net.akazukin.database;

public class PrimaryKey extends Key {
    public PrimaryKey(final String name, final Type type) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = true;
        this.isNullable = false;
    }

    public PrimaryKey(final String name, final Type type, final int length) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.isPrimaryKey = true;
        this.isNullable = false;
    }
}
