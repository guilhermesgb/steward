package com.github.guilhermesgb.steward.utils;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ArgumentsParser {

    public static final List<Argument> WILL_NOT_DEFINE_ARGUMENTS = new LinkedList<>();
    public static final ArgumentsParsedCallback WILL_NOT_DEFINE_RESOLVED_ARGUMENT_VALUES_ASSIGNER = new ArgumentsParsedCallback() {
        @Override
        public void doUponArgumentParsingCompleted(Object... values) {}
    };

    Object[] parseArguments(Bundle savedInstanceState, Intent intent, List<Argument> arguments) {
        return parseArguments(savedInstanceState, intent != null ? intent.getExtras() : null, arguments);
    }

    Object[] parseArguments(Bundle savedInstanceState, Bundle extras, List<Argument> arguments) {
        Object[] argumentValues = new Object[arguments.size()];
        for (int i=0; i<arguments.size(); i++) {
            argumentValues[i] = parseArgument(savedInstanceState, extras, arguments.get(i));
        }
        return argumentValues;
    }

    private Object parseArgument(Bundle savedInstanceState, Bundle extras, Argument argument) {
        Object argumentValue = null;
        if (extras != null) {
            argumentValue = parseByArgumentType(argument.getType(), argument.getKey(), extras);
        }
        if (argumentValue != null) {
            return argumentValue;
        } else if (savedInstanceState != null) {
            argumentValue = parseByArgumentType(argument.getType(), argument.getKey(), savedInstanceState);
            if (argumentValue != null) {
                return argumentValue;
            }
        }
        return argument.getType() == ArgumentType.INT_DEFAULT_ZERO ? Integer.valueOf(0)
            : argument.getType() == ArgumentType.INT_DEFAULT_MINUS_ONE ? Integer.valueOf(-1)
            : argument.getType() == ArgumentType.BOOLEAN_DEFAULT_FALSE ? Boolean.valueOf(false)
            : argument.getType() == ArgumentType.BOOLEAN_DEFAULT_TRUE ? true
            : argument.getType() == ArgumentType.INT_ARRAY ? new int[0]
            : argument.getType() == ArgumentType.PARCELABLE_ARRAY ? new Parcelable[0]
            : argument.getType() == ArgumentType.PARCELABLE_ARRAY_LIST ? new ArrayList<Parcelable>()
            : argument.getType() == ArgumentType.BUNDLE ? new Bundle()
            : null;
    }

    private static Object parseByArgumentType(ArgumentType argumentType, String argumentKey, Bundle source) {
        switch (argumentType) {
            case STRING:
                return source.getString(argumentKey);
            case INT_DEFAULT_ZERO:
                return source.getInt(argumentKey);
            case INT_DEFAULT_MINUS_ONE:
                return source.getInt(argumentKey, -1);
            case INT_ARRAY:
                return source.getIntArray(argumentKey);
            case LONG:
                return source.getLong(argumentKey);
            case FLOAT:
                return source.getFloat(argumentKey);
            case BOOLEAN_DEFAULT_FALSE:
                return source.getBoolean(argumentKey, false);
            case BOOLEAN_DEFAULT_TRUE:
                return source.getBoolean(argumentKey, true);
            case SERIALIZABLE:
                return source.getSerializable(argumentKey);
            case PARCELABLE:
                return source.getParcelable(argumentKey);
            case PARCELABLE_ARRAY:
                return source.getParcelableArray(argumentKey);
            case PARCELABLE_ARRAY_LIST:
                return source.getParcelableArrayList(argumentKey);
            case BUNDLE:
                return source.getBundle(argumentKey);
        }
        return null;
    }

}
