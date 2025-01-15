package com.openeuler.collect.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class JsonFileUtil {

    public static String read(String name) {
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(JsonFileUtil.class.getClassLoader().getResourceAsStream(name), "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
