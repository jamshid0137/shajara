package com.example.shajara.util;


import java.util.Random;

public class RandomUtil {
    public static final Random random=new Random();
    public static String getRandomSmsCode(){

        return String.valueOf(random.nextInt(10000,99999));
    }
}