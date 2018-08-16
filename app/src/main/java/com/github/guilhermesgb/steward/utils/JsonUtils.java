package com.github.guilhermesgb.steward.utils;

import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import static com.github.guilhermesgb.steward.utils.StringUtils.isEmpty;

@SuppressWarnings({"WeakerAccess", "unused"})
public class JsonUtils {

    private static class JsonBuilder {

        private final JsonObject underlyingObject;

        private JsonBuilder() {
            underlyingObject = new JsonObject();
        }

        private JsonBuilder(JsonObject json) {
            underlyingObject = json;
        }

        private void putString(String key, String value) {
            underlyingObject.addProperty(key, value);
        }

        public void putBoolean(String key, boolean value) {
            underlyingObject.addProperty(key, value);
        }

        private void putObject(String key, JsonObject value) {
            underlyingObject.add(key, value);
        }

        private void putArray(String key, JsonArray value) {
            underlyingObject.add(key, value);
        }

        private JsonObject build() {
            return underlyingObject;
        }

    }

    public static JsonObject json(Object... keysAndValues) {
        return makeJson(new JsonBuilder(), keysAndValues);
    }

    public static String rawJson(Object... keyAndValues) {
        return new Gson().toJson(json(keyAndValues));
    }

    public static JsonObject json(JsonObject json, Object... keysAndValues) {
        return makeJson(new JsonBuilder(json), keysAndValues);
    }

    public static String rawJson(JsonObject json, Object... keyAndValues) {
        return new Gson().toJson(json(json, keyAndValues).toString());
    }

    private static JsonObject makeJson(JsonBuilder builder, Object... keysAndValues) {
        try {
            for (int i=0; i<keysAndValues.length-1; i+=2) {
                String key = (String) keysAndValues[i];
                Object value = keysAndValues[i+1];
                if (value instanceof String) {
                    builder.putString(key, (String) value);
                } else if (value instanceof Boolean) {
                    builder.putBoolean(key, (boolean) value);
                } else if (value instanceof JsonObject) {
                    builder.putObject(key, (JsonObject) value);
                } else if (value instanceof JsonArray) {
                    builder.putArray(key, (JsonArray) value);
                }
            }
            return builder.build();
        } catch (Throwable ignore) {
            return new JsonObject();
        }
    }

    public static String getOptionalString(JsonObject json, String key) {
        return getString(json, key, null);
    }

    public static String getString(JsonObject json, String key, String defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : defaultValue;
    }

    public static String[] getOptionalStrings(JsonObject json, String key) {
        List<String> strings = new LinkedList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray jsonArray = json.get(key).getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonPrimitive()) {
                    return new String[0];
                }
                strings.add(jsonElement.getAsString());
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    public static boolean getOptionalBoolean(JsonObject json, String key) {
        return getBoolean(json, key, false);
    }

    public static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : defaultValue;
    }

    public static int getOptionalInt(JsonObject json, String key) {
        return getInt(json, key, -1);
    }

    public static int getInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : defaultValue;
    }

    public static long getLong(JsonObject json, String key, long defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsLong() : defaultValue;
    }

    public static float getOptionalFloat(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsFloat() : -1;
    }

    public static double getOptionalDouble(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : -1;
    }

    public static double getDouble(JsonObject json, String key, long defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsDouble() : defaultValue;
    }

    public static DateTime getOptionalDate(JsonObject json, String key) {
        return getDate(json, key, null);
    }

    public static DateTime getDate(JsonObject json, String key, DateTime defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? new DateTime(json.get(key).getAsString()) : defaultValue;
    }

    public static <E extends Enum<E>> E getEnum(JsonObject json, String key, Class<E> enumeration, E defaultValue) {
        if (!(json.has(key) && json.get(key).isJsonPrimitive())) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumeration, json.get(key).getAsString()
                    .toUpperCase().replace("-", "_"));
        } catch (Throwable throwable) {
            return defaultValue;
        }
    }

    public static <P extends Parcelable> Parcelable getOptionalParcelable(JsonObject json, String key,
                                                                          Class<P> parcelableClass) {
        return getParcelable(json, key, parcelableClass, null);
    }

    public static <P extends Parcelable> Parcelable getParcelable(JsonObject json, String key,
                                                                  Class<P> parcelableClass, P defaultValue) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            return defaultValue;
        }
        return dejsonizeFrom(json.get(key), parcelableClass);
    }

    public static <P extends Parcelable> Parcelable[] getOptionalParcelables(JsonObject json, String key,
                                                                             Class<P> parcelableClass) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return (Parcelable[]) Array.newInstance(parcelableClass, 0);
        }
        return dejsonizeArray(json.get(key).getAsJsonArray(), parcelableClass);
    }

    public static <P extends Parcelable> Parcelable[] destringifyArray(String jsonRaw, Class<P> parcelableClass) {
        if (isEmpty(jsonRaw)) {
            return (Parcelable[]) Array.newInstance(parcelableClass, 0);
        }
        return dejsonizeArray(new JsonParser().parse(jsonRaw), parcelableClass);
    }

    private static <P extends Parcelable> Parcelable[] dejsonizeArray(JsonElement json, Class<P> parcelableClass) {
        if (!json.isJsonArray()) {
            return (Parcelable[]) Array.newInstance(parcelableClass, 0);
        }
        JsonArray array = json.getAsJsonArray();
        Parcelable[] toReturn = (Parcelable[]) Array.newInstance(parcelableClass, array.size());
        int pos = 0;
        for (JsonElement entry : array) {
            toReturn[pos++] = dejsonizeFrom(entry, parcelableClass);
        }
        return toReturn;
    }

    public static <P extends Parcelable> Parcelable dejsonizeFrom(JsonElement json, Class<P> parcelableClass) {
        if (json == null || !(json.isJsonObject() || json.isJsonPrimitive())) {
            return null;
        }
        if (json.isJsonPrimitive()) {
            JsonObject toReplace = new JsonObject();
            toReplace.addProperty("_id", json.getAsString());
            json = toReplace;
        }
        try {
            Method dejsonizeFrom = parcelableClass.getDeclaredMethod("dejsonizeFrom", JsonObject.class);
            return (Parcelable) dejsonizeFrom.invoke(null, json.getAsJsonObject());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    public static <P extends Parcelable> Parcelable destringifyFrom(String jsonRaw, Class<P> parcelableClass) {
        if (isEmpty(jsonRaw)) {
            return null;
        }
        return dejsonizeFrom(new JsonParser().parse(jsonRaw), parcelableClass);
    }

    public static <P extends Parcelable> JsonArray jsonizeArray(Parcelable[] parcelables, Class<P> parcelableClass) {
        JsonArray array = new JsonArray();
        if (parcelables == null) {
            return array;
        }
        for (Parcelable parcelable : parcelables) {
            array.add(jsonizeFrom(parcelable, parcelableClass));
        }
        return array;
    }

    public static <P extends Parcelable> JsonElement jsonizeFrom(Parcelable parcelable, Class<P> parcelableClass) {
        if (parcelable == null) {
            return JsonNull.INSTANCE;
        }
        try {
            Method jsonizeFrom = parcelableClass.getDeclaredMethod("jsonizeFrom", parcelableClass);
            return (JsonObject) jsonizeFrom.invoke(null, parcelable);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return JsonNull.INSTANCE;
        }
    }

    public static <P extends Parcelable> String stringifyFrom(Parcelable parcelable, Class<P> parcelableClass) {
        JsonElement json = jsonizeFrom(parcelable, parcelableClass);
        return !json.isJsonObject() ? null : json.toString();
    }

    public static <P extends Parcelable> String stringifyArray(Parcelable[] parcelables, Class<P> parcelableClass) {
        return jsonizeArray(parcelables, parcelableClass).toString();
    }

    public static JsonArray jsonizeStrings(String[] strings) {
        JsonArray jsonArray = new JsonArray();
        for (String string : strings) {
            jsonArray.add(string);
        }
        return jsonArray;
    }

}
