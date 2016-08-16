package com.way.downloadmanager.lib;

import java.util.HashMap;
import java.util.Locale;


final class EnumTypeParser {
    private HashMap<String, Object> enumValues;
    private boolean ignoreCase;
    private Object firstValue;

    public EnumTypeParser(Class<?> enumType, boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        enumValues = new HashMap<String, Object>();
        Object[] values = enumType.getEnumConstants();
        if (values.length > 0)
            firstValue = values[0];
        for (Object obj : values) {
            Enum<?> enumValue = (Enum<?>) obj;
            String name = enumValue.name();
            if (ignoreCase)
                name = name.toLowerCase();
            enumValues.put(name, obj);
        }
    }

    public final Object getFirstValue() {
        return firstValue;
    }

    public final Object getValue(String value) {
        if (ignoreCase)
            value = value.toLowerCase(Locale.getDefault());
        return enumValues.get(value);
    }
}
