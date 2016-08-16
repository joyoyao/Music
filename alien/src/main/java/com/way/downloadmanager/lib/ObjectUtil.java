package com.way.downloadmanager.lib;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public final class ObjectUtil {

    public static Object newInstance(Class<?> type) {
        try {
            return type.newInstance();
        } catch (Exception e) {
            Log.v(LogConst.TAG_OBJECT,
                    "ObjectUtil.newInstance");
            ErrorException.printException(ObjectUtil.class,
                    LogConst.TAG_OBJECT + type.getSimpleName(), e);
        }
        return null;
    }

    public static List<Field> getFields(Class<?> type) {
        List<Field> list = new ArrayList<Field>();
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            if (!list.contains(field))
                list.add(field);
        }
        Class<?> superType = type.getSuperclass();
        while (superType != Object.class) {
            List<Field> superFields = getFields(superType);
            for (Field field : superFields) {
                if (!list.contains(field))
                    list.add(field);
            }
            superType = superType.getSuperclass();
        }
        return list;
    }

    public static boolean isSimpleClass(Class<?> type) {
        return type == Boolean.class || type == boolean.class || type == byte.class
                || type == Byte.class || type == char.class || type == Character.class
                || type == short.class || type == Short.class || type == int.class
                || type == Integer.class || type == long.class || type == Long.class
                || type == float.class || type == Float.class || type == double.class
                || type == Long.class || type == String.class;
    }

    public static Object getValue(Field field, Object obj) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        return null;
    }

    public static void setValue(Field field, Object obj, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
    }

    public static Object getEnumValue(Class<?> enumType, String xmlValue, Field info,
                                      String defaultValue, boolean ignoreCase) {
        if (enumType == Object.class)
            enumType = info.getType();
        EnumTypeParser enumParser = new EnumTypeParser(enumType, ignoreCase);
        Object value = enumParser.getValue(xmlValue);
        if (value != null)
            return value;
        if (StringUtil.isEmpty(defaultValue))
            return enumParser.getFirstValue();
        else {
            value = enumParser.getValue(defaultValue);
            if (value != null)
                return value;
            else
                return enumParser.getFirstValue();
        }
    }

    public static void readBooleanValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            field.setBoolean(obj, inputStream.readBoolean());
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

    public static void writeBooleanValue(DataOutputStream outputStream, Object obj, Field field)
            throws ErrorException {
        try {
            outputStream.writeBoolean(field.getBoolean(obj));
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

    public static void readIntValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            field.setInt(obj, inputStream.readInt());
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

    public static void writeIntValue(DataOutputStream outputStream, Object obj, Field field)
            throws ErrorException {
        try {
            outputStream.writeInt(field.getInt(obj));
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "write int data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "write int data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "write int data from file error", e);
        }
    }

    public static void readLongValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            field.setLong(obj, inputStream.readLong());
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "read long data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "read long data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "read long data from file error", e);
        }
    }

    public static void writeLongValue(DataOutputStream outputStream, Object obj, Field field)
            throws ErrorException {
        try {
            outputStream.writeLong(field.getLong(obj));
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "write long data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "write long data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "write long data from file error", e);
        }
    }

    public static void readFloatValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            field.setFloat(obj, inputStream.readFloat());
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "read float data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "read float data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "read float data from file error", e);
        }
    }

    public static void writeFloatValue(DataOutputStream outputStream, Object obj, Field field)
            throws ErrorException {
        try {
            outputStream.writeFloat(field.getFloat(obj));
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "write float data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "write float data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "write float data from file error", e);
        }
    }

    public static void readDoubleValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            field.setDouble(obj, inputStream.readDouble());
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "read double data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "read double data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "read double data from file error", e);
        }
    }

    public static void writeDoubleValue(DataOutputStream outputStream, Object obj, Field field)
            throws ErrorException {
        try {
            outputStream.writeDouble(field.getDouble(obj));
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, "write double data from file error", e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, "write double data from file error", e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, "write double data from file error", e);
        }
    }

    public static void readStringValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            String value = inputStream.readUTF();
            field.set(obj, value);
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

    public static void writeStringValue(DataOutputStream outputStream, Object obj, Field field)
            throws ErrorException {
        try {
            String value = (String) field.get(obj);
            if (StringUtil.isEmpty(value)) {
                value = "";
            }
            outputStream.writeUTF(value);
        } catch (IllegalArgumentException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IOException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        } catch (IllegalAccessException e) {
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

    public static void readEnumValue(DataInputStream inputStream, Object obj, Field field)
            throws ErrorException {
        try {
            Class<?> enumType = Class.forName(inputStream.readUTF());
            String value = inputStream.readUTF();
            Object enumValue = ObjectUtil.getEnumValue(enumType, value, field, null, true);
            field.set(obj, enumValue);
        } catch (Exception e) {
            Log.v(LogConst.TAG_OBJECT,
                    "ObjectUtil.readEnumValue");
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

    public static void writeEnumValue(DataOutputStream outStream, Object obj, Field field)
            throws ErrorException {
        String cls = field.getType().getCanonicalName();
        try {
            outStream.writeUTF(cls);
            String value = field.get(obj).toString();
            outStream.writeUTF(value);
        } catch (Exception e) {
            Log.v(LogConst.TAG_OBJECT,
                    "ObjectUtil.writeEnumValue");
            throw new ErrorException(ObjectUtil.class, LogConst.TAG_OBJECT, e);
        }
    }

}
