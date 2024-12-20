package com.sdk.tspl.demo.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Android 文本文件读取器
 */
public class AssetFileReader {
    public static List<String> readAssetFile(Context context, String fileName) {
        List<String> lines = new ArrayList<>();
        AssetManager assetManager = context.getAssets();

        try (InputStream inputStream = assetManager.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("读取文件", "文件名：" + fileName);
        }

        return lines;
    }
}
