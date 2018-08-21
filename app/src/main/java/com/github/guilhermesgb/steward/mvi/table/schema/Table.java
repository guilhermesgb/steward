package com.github.guilhermesgb.steward.mvi.table.schema;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.Arrays;

import static com.github.guilhermesgb.steward.utils.JsonUtils.getBoolean;
import static com.github.guilhermesgb.steward.utils.JsonUtils.getOptionalInt;

@Entity(tableName = "stand")
public class Table implements Serializable, Parcelable {

    @PrimaryKey private int number;
    private boolean available;

    private Table(Parcel in) {
        number = in.readInt();
        available = in.readByte() != 0;
    }

    public static final Creator<Table> CREATOR = new Creator<Table>() {
        @Override
        public Table createFromParcel(Parcel in) {
            return new Table(in);
        }

        @Override
        public Table[] newArray(int size) {
            return new Table[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(number);
        dest.writeByte((byte) (available ? 1 : 0));
    }

    public Table(int number, boolean available) {
        this.number = number;
        this.available = available;
    }

    public int getNumber() {
        return number;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public static Table dejsonizeFrom(JsonObject json) {
        return json == null ? null : new Table(
            getOptionalInt(json, "number"),
            getBoolean(json, "available", false)
        );
    }

    private static JsonObject jsonizeFrom(Table table) {
        if (table == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("number", table.number);
        json.addProperty("available", table.available);
        return json;
    }

    @Override
    public String toString() {
        return jsonizeFrom(this).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return number == table.number &&
            available == table.available;
    }

    @Override
    public int hashCode() {
        Object[] toHash = new Object[] { number, available };
        return Arrays.hashCode(toHash);
    }

}
