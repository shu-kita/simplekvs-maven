package com.shu.simplekvs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IOUtils {
    // KeyとValueをファイルに書き込む関数
    public static void dumpKV(BufferedOutputStream bos, String key, String value) throws IOException {
        // keyをbyte配列にエンコードし、長さを取得
        byte[][] KeyAndLen = IOUtils.getByteStrAndLength(key);
        byte[] byteKey = KeyAndLen[0];
        byte[] keyLenBytes = KeyAndLen[1];

        // valueをbyte配列にエンコードし、長さを取得
        byte[][] valueAndLen = IOUtils.getByteStrAndLength(value);
        byte[] byteValue = valueAndLen[0];
        byte[] valueLenBytes = valueAndLen[1];

        // byte配列の結合
        byte[] writeBytes = IOUtils.combineBytes(keyLenBytes, byteKey, valueLenBytes, byteValue);

        bos.write(writeBytes);
    }

    public static String[] loadKV(BufferedInputStream bis, int position) throws IOException {
        String[] kvPair = new String[2]; 
        bis.skip(position);
        for (int i = 0 ; i < 2; i++) {
            byte[] bytes = new byte[4];
            bis.read(bytes, 0, bytes.length);
            int length = ByteBuffer.wrap(bytes).getInt();
            byte[] byteStr = new byte[length];
            bis.read(byteStr, 0, byteStr.length);
            kvPair[i] = new String(byteStr);
        }
        return kvPair;
    }

    public static void dumpIndex(BufferedOutputStream bos, String key, int position) throws IOException {
        byte[][] KeyAndLen = IOUtils.getByteStrAndLength(key);
        byte[] byteKey = KeyAndLen[0];
        byte[] keyLenBytes = KeyAndLen[1];
        byte[] posBytes = ByteBuffer.allocate(4).putInt(position).array();
        byte[] writeBytes = IOUtils.combineBytes(keyLenBytes, byteKey, posBytes);
        bos.write(writeBytes);
    }

    public static Map<String, Integer> loadIndex(BufferedInputStream bis) throws IOException {
        Map<String, Integer> index = new HashMap<>();
        byte[] bytes = new byte[4];
        int read;
        while ((read = bis.read(bytes, 0, bytes.length)) != -1) {
            int length = ByteBuffer.wrap(bytes).getInt();
            byte[] byteKey = new byte[length];
            bis.read(byteKey, 0, byteKey.length);

            bis.read(bytes, 0, bytes.length);
            String key = new String(byteKey);
            int position = ByteBuffer.wrap(bytes).getInt();
            index.put(key, position);
        }
        return index;
    }

    protected static byte[][] getByteStrAndLength(String str) {
        byte[][] bytes = new byte[2][];
        bytes[0] = str.getBytes(StandardCharsets.UTF_8);
        int strLength = bytes[0].length;
        bytes[1] = ByteBuffer.allocate(4).putInt(strLength).array();
        return bytes;
    }

    // 4つのByte配列を結合する関数
    protected static byte[] combineBytes(byte[] byteArray1, byte[] byteArray2, byte[] byteArray3, byte[] byteArray4) {
        // 各配列の長さを取得
        int length1 = byteArray1.length;
        int length2 = byteArray2.length;
        int length3 = byteArray3.length;
        int length4 = byteArray4.length;

        byte[] combinedArray = new byte[length1 + length2 + length3 + length4];

        // 順に結合
        System.arraycopy(byteArray1, 0, combinedArray, 0, length1);
        System.arraycopy(byteArray2, 0, combinedArray, length1, length2);
        System.arraycopy(byteArray3, 0, combinedArray, (length1+length2), length3);
        System.arraycopy(byteArray4, 0, combinedArray, (length1+length2+length3), length4);

        return combinedArray;
    }

    // 3つのByte配列を結合する関数
    protected static byte[] combineBytes(byte[] byteArray1, byte[] byteArray2, byte[] byteArray3) {
        // 各配列の長さを取得
        int length1 = byteArray1.length;
        int length2 = byteArray2.length;
        int length3 = byteArray3.length;

        byte[] combinedArray = new byte[length1 + length2 + length3];

        // 順に結合
        System.arraycopy(byteArray1, 0, combinedArray, 0, length1);
        System.arraycopy(byteArray2, 0, combinedArray, length1, length2);
        System.arraycopy(byteArray3, 0, combinedArray, (length1+length2), length3);

        return combinedArray;
    }

    // 2つのByte配列を結合する関数
    protected static byte[] combineBytes(byte[] byteArray1, byte[] byteArray2) {
        // 各配列の長さを取得
        int length1 = byteArray1.length;
        int length2 = byteArray2.length;

        byte[] combinedArray = new byte[length1 + length2];

        // 順に結合
        System.arraycopy(byteArray1, 0, combinedArray, 0, length1);
        System.arraycopy(byteArray2, 0, combinedArray, length1, length2);

        return combinedArray;
    }
    
    // byte配列のスライス用関数
    public static byte[] slice(byte[] arr, int stIndx, int enIndx) {
        byte[] sclicedArr = Arrays.copyOfRange(arr, stIndx, enIndx);
        return sclicedArr;
    }
    
    // string配列のスライス用関数
    public static String[] slice(String[] arr, int stIndx, int enIndx) {
        String[] sclicedArr = Arrays.copyOfRange(arr, stIndx, enIndx);
        return sclicedArr;
    }
}
