package com.example.shajara.util;

import java.util.regex.Pattern;

public class SmsUtil {
    public static boolean isPhone(String value){//value telefon formatidami yo'qligini tekshiradi.
        String phoneRegex="^998\\d{9}$";
        return Pattern.matches(phoneRegex,value);
    }
}
