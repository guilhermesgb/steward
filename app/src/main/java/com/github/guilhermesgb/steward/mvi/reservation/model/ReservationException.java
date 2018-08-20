package com.github.guilhermesgb.steward.mvi.reservation.model;

import com.google.gson.JsonObject;

public class ReservationException extends Exception {

    private Code code;

    public enum Code {
        CUSTOMER_NOT_FOUND,
        RESERVATION_IN_PLACE,
        CUSTOMER_BUSY,
        TABLE_NOT_FOUND,
        TABLE_UNAVAILABLE,
        DATABASE_FAILURE
    }

    public ReservationException(Code code) {
        this(code, null);
    }

    public ReservationException(Code code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    private static JsonObject jsonizeFrom(ReservationException reservationException) {
        if (reservationException == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("code", reservationException.code.toString());
        if (reservationException.getCause() != null) {
            json.addProperty("cause", reservationException.getCause().toString());
        }
        return json;
    }

    @Override
    public String toString() {
        return jsonizeFrom(this).toString();
    }

}
