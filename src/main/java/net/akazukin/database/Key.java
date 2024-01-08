package net.akazukin.database;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

public abstract class Key {
    @Getter
    protected String name = null;
    @Getter
    protected Type type = null;
    @Getter
    protected int length = 0;
    protected boolean isPrimaryKey = false;
    protected boolean isNullable = true;
    protected Object defaulT = null;

    public boolean isNullable() {
        return isNullable;
    }

    public Object getDefault() {
        return defaulT;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Key key = (Key) o;

        return length == key.length && isPrimaryKey == key.isPrimaryKey && isNullable == key.isNullable && Objects.equals(name, key.name) && type == key.type && Objects.equals(defaulT, key.defaulT);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, length, isPrimaryKey, isNullable, defaulT);
    }

    public enum Type {
        String("TEXT"),
        Number("NUMERIC"),
        Integer("INTEGER"),
        Double("REAL"),
        Binary("BLOB");

        private final String name;
        Type(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Type getType(final String type) {
            return Arrays.stream(values()).filter(type2 -> (type2.getName().equalsIgnoreCase(type))).findFirst().get();
        }
    }
}
