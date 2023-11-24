package com.github.hanfeng21050.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author  hanfeng32305
 * Date  2023/11/1 0:26
 */
public class MyPluginLoader {
    private final SeeConfig seeConfig;
    private final Project project;

    public MyPluginLoader(Project project, SeeConfig seeConfig) {
        this.seeConfig = seeConfig;
        this.project = project;
    }

    /**
     * �����������ؽ���
     */
    public void startBlockingLoadingProcess() {
        // ����UI��ʹ�û����ܽ�����������
        ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(project).setAlternativeResolveEnabled(true));

        // ִ�м�������
        ProgressManager.getInstance().run(new Task.Modal(project, "������...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // ������ִ����Ҫ���ص�����ͬʱ���½���
                // ��ȡ��ǰ��Ŀ������
                String name = project.getName();
                String applicationName = name + "-svr";
                try {
                    // ��¼����ȡ auth ��Ϣ
                    SeeRequest.login(seeConfig);

                    String auth = SeeRequest.getAuth(seeConfig);
                    progressIndicator.setText("auth��ȡ�ɹ�, auth��" + auth);

                    // ��ȡӦ��id
                    String applicationId = SeeRequest.getApplication(seeConfig, applicationName, auth);
                    progressIndicator.setText(applicationName + "��ȡ��ȡӦ��id�ɹ���applicationId:" + applicationId);

                    if (StringUtils.isNotBlank(applicationId)) {
                        // ��ȡ������Ϣ
                        JSONObject config = SeeRequest.getConfigInfo(seeConfig, applicationId, auth);
                        progressIndicator.setText(applicationName + "��ȡ��Ŀ������Ϣ�ɹ�");

                        // ��������
                        saveConfigToFile(progressIndicator, config);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage("��Ŀ" + applicationName + "���û�ȡ�ɹ�", "��Ϣ");
                        });
                    } else {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage("δ��ȡ����ǰ��Ŀ" + applicationName + "�������ļ�������", "��ʾ");
                        });
                    }
                } catch (Exception ex) {
                    String errMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog("����ʧ�ܣ����顣" + errMsg, "����");
                    });
                    throw new RuntimeException(ex);
                }

            }

            @Override
            public void onFinished() {
                // ������ɺ�����UI
                ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(project).setAlternativeResolveEnabled(false));
            }
        });
    }

    // login
    private void login(@NotNull ProgressIndicator indicator) throws URISyntaxException, IOException {
        HttpClientUtil.clearCookie();

        indicator.setText("��ʼ��¼��" + seeConfig.getAddress());
        String str = HttpClientUtil.httpGet(seeConfig.getAddress() + "/cas/login?get-lt=true");
        String ltPattern = "\"lt\":\"(.*?)\"";
        String executionPattern = "\"execution\":\"(.*?)\"";

        Pattern ltRegex = Pattern.compile(ltPattern);
        Pattern executionRegex = Pattern.compile(executionPattern);

        Matcher ltMatcher = ltRegex.matcher(str);
        Matcher executionMatcher = executionRegex.matcher(str);
        if (ltMatcher.find() && executionMatcher.find()) {
            String ltValue = ltMatcher.group(1);
            String executionValue = executionMatcher.group(1);

            Map<String, String> map = new HashMap<>();
            map.put("username", seeConfig.getUsername());
            // ����
            map.put("password", seeConfig.getPassword());
            map.put("execution", executionValue);
            map.put("lt", ltValue);
            map.put("submit", "LOGIN");
            map.put("_eventId", "submit");
            // ��¼
            HttpClientUtil.httpPost(seeConfig.getAddress() + "/cas/login", map);

            // ҳ����ת����ȡcookie
            HttpClientUtil.httpGet(seeConfig.getAddress() + "/cas/login?service=http%3A%2F%2F10.20.36.109%3A8081%2Facm%2Fcloud.htm");
            indicator.setFraction(0.2);
            indicator.checkCanceled();
        }
    }

    private String getAuth(@NotNull ProgressIndicator indicator) throws URISyntaxException, IOException {
        indicator.setText("���ڻ�ȡauth��Ϣ");
        // /acm/system/auth.json
        String str = HttpClientUtil.httpPost(seeConfig.getAddress() + "/acm/system/auth.json", new HashMap<>());
        String tokenPattern = "\"token\":\"(.*?)\"";
        Pattern tokenRegex = Pattern.compile(tokenPattern);
        Matcher matcher = tokenRegex.matcher(str);
        if (matcher.find()) {
            String token = matcher.group(1);
            indicator.setFraction(0.3);
            indicator.checkCanceled();
            return token;
        }
        return "";
    }

    private String getApplication(@NotNull ProgressIndicator indicator, String applicationName, String auth) throws IOException {
        // /acm/dssp/application/authority/query.json
        indicator.setText("��ȡӦ��id��" + applicationName);
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        body.put("pageNo", "1");
        body.put("pageSize", "1");
        body.put("name", applicationName);
        body.put("allowedUpgradeMark", "true");
        header.put("Authorization", "Bearer " + auth);

        String str = HttpClientUtil.httpPost(seeConfig.getAddress() + "/acm/dssp/application/authority/query.json", body, header);
        JSONObject parse = JSONObject.parse(str);
        String errorInfo = (String) parse.get("error_info");
        if (StringUtils.isBlank(errorInfo)) {
            JSONArray jsonArray = parse.getJSONObject("data").getJSONArray("items");
            if (!jsonArray.isEmpty()) {
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                return (String) jsonObject.get("id");
            }
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog("error: " + errorInfo, "����");
            });
            throw new RuntimeException(errorInfo);
        }
        return "";
    }

    private JSONObject getConfig(@NotNull ProgressIndicator indicator, String applicationId, String auth) throws IOException {
        indicator.setText("��ȡ������Ϣ��" + applicationId);
        // /acm/dssp/config/getCompareConfig.json
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("applicationId", applicationId);
        String str = HttpClientUtil.httpPost(seeConfig.getAddress() + "/acm/dssp/config/getCompareConfig.json", body, header);
        indicator.setFraction(0.5);
        indicator.checkCanceled();
        indicator.setText("��ȡ���óɹ���" + applicationId);
        return JSONObject.parse(str);
    }

    /**
     * @param indicator
     * @param jsonObject
     */
    private void saveConfigToFile(@NotNull ProgressIndicator indicator, JSONObject jsonObject) {
        JSONArray array = mergeNonEmptyConfig(jsonObject);
        for (int i = 0; i < array.size(); i++) {
            JSONObject config = array.getJSONObject(i);
            // ��ȡ�ļ�����
            String path = (String) config.get("path");
            if (path.contains(".properties") || path.contains(".dat") || path.contains(".pfx")) {
                Pattern pattern = Pattern.compile("([^/]+)$");
                Matcher matcher = pattern.matcher(path);
                if (matcher.find()) {
                    String fileName = matcher.group(1);
                    indicator.setText("���ڱ������ã�" + fileName);
                    String content = (String) config.get("content");
                    // ������������
                    content = removeOrReplace(fileName, content);
                    saveFile(fileName, content);
                }
            }
        }
    }


    /**
     * ��ȡ���е�config�����ļ�
     *
     * @param jsonObject
     * @return
     */
    public JSONArray mergeNonEmptyConfig(JSONObject jsonObject) {
        JSONArray mergedConfigs = new JSONArray();

        if (jsonObject.containsKey("config")) {
            JSONArray configArray = jsonObject.getJSONArray("config");
            if (configArray != null && !configArray.isEmpty()) {
                mergedConfigs.addAll(configArray);
            }
        }

        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                JSONArray nestedNonEmptyConfigs = mergeNonEmptyConfig((JSONObject) value);
                if (!nestedNonEmptyConfigs.isEmpty()) {
                    mergedConfigs.addAll(nestedNonEmptyConfigs);
                }
            } else if (value instanceof JSONArray) {
                for (Object arrayElement : (JSONArray) value) {
                    if (arrayElement instanceof JSONObject) {
                        JSONArray nestedNonEmptyConfigs = mergeNonEmptyConfig((JSONObject) arrayElement);
                        if (!nestedNonEmptyConfigs.isEmpty()) {
                            mergedConfigs.addAll(nestedNonEmptyConfigs);
                        }
                    }
                }
            }
        }

        return mergedConfigs;
    }


    /**
     * �����ļ�
     *
     * @param fileName
     * @param content
     */
    public void saveFile(String fileName, String content) {
        VirtualFile[] modules = project.getBaseDir().getChildren();
        for (VirtualFile module : modules) {
            if (module.getPath().contains("deploy")) {
                try {
                    String resourceDirPath = "src/main/resources"; // ������Ŀ�ṹ�ʵ��޸�·��
                    // ʹ��LocalFileSystem������ԴĿ¼�ľ���·��
                    VirtualFile resourceDirectory = LocalFileSystem.getInstance().findFileByPath(module.getPath() + "/" + resourceDirPath);

                    File file = new File(resourceDirectory.getPath() + "/" + fileName);
                    // ����ļ����ڣ���ɾ����
                    if (file.exists()) {
                        file.delete();
                    }
                    if (fileName.contains(".dat")) {
                        // �����ļ�ת�������Ʊ���
                        byte[] bytes = Base64.decodeBase64(content);
                        FileUtils.writeByteArrayToFile(file, bytes);
                    } else {
                        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
                    }

                    // ˢ����ԴĿ¼���Ա���IDE����ʾ�ļ�
                    VfsUtil.markDirtyAndRefresh(true, true, true, resourceDirectory);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }

    /**
     * �����ļ����⴦��
     *
     * @param fileName
     * @param content
     * @return
     */
    public String removeOrReplace(String fileName, String content) {
        // log4j2.xml �Ƴ���kafka��ؽڵ�
//        if (fileName.equals("log4j2.xml")) {
//            Pattern pattern = Pattern.compile("<Kafka.*?</Kafka>", Pattern.DOTALL);
//            Matcher matcher = pattern.matcher(content);
//            content = matcher.replaceAll("");
//        }

        // application.properties
        if (fileName.equals("application.properties")) {
            // ����Ҫ��������ļ�
            content = content.replaceAll("\\./cust-config/emergency\\.properties,", "");

            // �滻�����ļ�·��
            content = content.replaceAll("\\./config/", "classpath:");

            // ע��app.host
            content = content.replaceAll("(app\\.host=)", "# $1");
        }

        if (fileName.equals("middleware.properties")) {
            // �滻�����ļ�·��
            content = content.replaceAll("files://\\./config/", "classpath:");
            content = content.replaceAll("\\./config/", "classpath:");
        }

        return content;
    }
}
