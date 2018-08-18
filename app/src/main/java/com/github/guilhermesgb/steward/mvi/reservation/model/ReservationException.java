package com.github.guilhermesgb.steward.mvi.reservation.model;

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

}
