package com.example.shajara.util;

import java.util.regex.Pattern;

public class EmailUtil {
    public static boolean isEmail(String value){              //valueni email email emasligini tekshiradi.
        String emailRegex="^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        return Pattern.matches(emailRegex,value);
    }
}
