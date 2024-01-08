package net.akazukin.database;

public class Column extends Key {
    public Column(final String name, final Type type) {
        this.name = name;
        this.type = type;
    }
    public Column(final String name, final Type type, final int length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    public Column(final String name, final Type type, final int length, final boolean notNull, final Object defaulT) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.isNullable = !notNull;
        this.defaulT = defaulT;
    }
}
