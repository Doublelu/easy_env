package com.github.hanfeng21050.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonValidateUtil {


    /**
     * У����ַ�Ƿ�Ϸ�
     *
     * @param url
     * @return
     */
    public static boolean isValidURL(String url) {
        // ����URL��������ʽģʽ
        String urlRegex = "^(https?://[^/]+)$";

        // ����Pattern����
        Pattern pattern = Pattern.compile(urlRegex);

        // ����Matcher����
        Matcher matcher = pattern.matcher(url);

        // ���ƥ����
        return matcher.matches();
    }


    public static boolean isFileNameMatch(String fileName, String pattern) {
        // ���� PathMatcher
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        // ���ļ���ת��Ϊ Path ����
        Path path = Paths.get(fileName);

        // ִ��ƥ��
        return matcher.matches(path);
    }
}
