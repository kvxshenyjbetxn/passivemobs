package com.passivemobs;

/** Matches optional mod API types without linking against or initializing their classes. */
public final class TypeHierarchy {
    private TypeHierarchy() {}

    public static boolean hasType(Class<?> type, String name) {
        if (type == null) return false;
        if (type.getName().equals(name)) return true;
        for (Class<?> parent : type.getInterfaces()) {
            if (hasType(parent, name)) return true;
        }
        return hasType(type.getSuperclass(), name);
    }
}
