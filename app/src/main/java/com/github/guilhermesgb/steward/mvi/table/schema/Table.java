package com.github.guilhermesgb.steward.mvi.table.schema;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.JsonObject;

import java.io.Serializable;

import static com.github.guilhermesgb.steward.utils.JsonUtils.getBoolean;
import static com.github.guilhermesgb.steward.utils.JsonUtils.getOptionalInt;

@Entity(tableName = "stand")
public class Table implements Serializable, Parcelable {

    @PrimaryKey private int number;
    private boolean isAvailable;

    private Table(Parcel in) {
        number = in.readInt();
        isAvailable = in.readByte() != 0;
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
        dest.writeByte((byte) (isAvailable ? 1 : 0));
    }

    public Table(int number, boolean isAvailable) {
        this.number = number;
        this.isAvailable = isAvailable;
    }

    public int getNumber() {
        return number;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public static Table dejsonizeFrom(JsonObject json) {
        return json == null ? null : new Table(
            getOptionalInt(json, "number"),
            getBoolean(json, "isAvailable", false)
        );
    }

    public static JsonObject jsonizeFrom(Table table) {
        if (table == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("number", table.number);
        json.addProperty("isAvailable", table.isAvailable);
        return json;
    }

    @Override
    public String toString() {
        return jsonizeFrom(this).toString();
    }

}
