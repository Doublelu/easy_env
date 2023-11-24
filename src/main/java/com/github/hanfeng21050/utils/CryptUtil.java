package com.github.hanfeng21050.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CryptUtil {

    public static String encryptPassword(String password) throws Exception {
        // ʹ�� sha512 ����
        String sha512Result = sha512(password);

        // ʹ�� md5 ����
        String md5Result = md5(password);

        // ƴ�� sha512 �� md5 �Ľ��
        return sha512Result + "," + md5Result;
    }



    private static String sha512(String input) {
        return hash(input, "SHA-512");
    }

    private static String md5(String input) {
        return hash(input, "MD5");
    }

    private static String hash(String input, String algorithm) {
        try {
            // ��ȡժҪ�㷨ʵ��
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            // �����ϣֵ
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // ���ֽ�����ת��Ϊʮ�������ַ���
            StringBuilder hexStringBuilder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                hexStringBuilder.append(hex.length() == 1 ? "0" : "").append(hex);
            }

            return hexStringBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
