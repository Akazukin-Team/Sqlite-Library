package net.akazukin.database;

public class Column extends Key {
    public Column(String name, Type type) {
        this.name = name;
        this.type = type;
    }
    public Column(String name, Type type, int length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    public Column(String name, Type type, int length, boolean notNull, Object defaulT) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.isNullable = !notNull;
        this.defaulT = defaulT;
    }
}