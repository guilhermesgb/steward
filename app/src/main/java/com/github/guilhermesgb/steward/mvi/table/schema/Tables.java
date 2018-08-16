package com.github.guilhermesgb.steward.mvi.table.schema;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Tables implements Serializable, Parcelable {

    private List<Table> tables;

    private Tables(Parcel in) {
        tables = in.createTypedArrayList(Table.CREATOR);
    }

    public static final Creator<Tables> CREATOR = new Creator<Tables>() {
        @Override
        public Tables createFromParcel(Parcel in) {
            return new Tables(in);
        }

        @Override
        public Tables[] newArray(int size) {
            return new Tables[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(tables);
    }

    public Tables(List<Table> tables) {
        this.tables = tables;
    }

    public List<Table> getTables() {
        return tables;
    }

    public static Tables dejsonizeFrom(JsonArray json) {
        if (json == null) {
            return null;
        }
        List<Table> tables = new LinkedList<>();
        for (int i=0; i<json.size(); i++) {
            JsonObject mapped = new JsonObject();
            mapped.addProperty("number", i);
            mapped.addProperty("isAvailable",
                json.get(i).getAsBoolean());
            tables.add(Table.dejsonizeFrom(mapped));
        }
        return new Tables(tables);
    }

    public static JsonArray jsonizeFrom(Tables tables) {
        if (tables == null) {
            return null;
        }
        JsonArray json = new JsonArray();
        for (Table table : tables.tables) {
            json.add(table.isAvailable());
        }
        return json;
    }

    @Override
    public String toString() {
        return jsonizeFrom(this).toString();
    }

}
