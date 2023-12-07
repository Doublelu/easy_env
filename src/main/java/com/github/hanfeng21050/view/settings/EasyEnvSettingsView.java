package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.github.hanfeng21050.utils.MyPluginLoader;
import com.google.common.collect.Maps;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ReflectionUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * ��ʾ������EasyEnv������õ���ͼ��
 */
public class EasyEnvSettingsView extends AbstractTemplateSettingsView {
    private final EasyEnvConfig config;
    private JButton importButton;
    private JButton exportButton;
    private JPanel envPanel;
    private JPanel panel;
    private JTable envTable;
    private JBList<Map.Entry<String, EasyEnvConfig.SeeConnectInfo>> seeConnectInfoMapList;

    private boolean isModify = false;

    /**
     * ���캯��������EasyEnv���ò���ʼ����ͼ��
     *
     * @param easyEnvConfig EasyEnv����
     */
    public EasyEnvSettingsView(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    /**
     * �����Զ�������ķ�����
     */
    private void createUIComponents() {
        // ��ʼ�� importButton
        importButton = new JButton("��������");
        importButton.addActionListener(e -> importConfiguration());

        // ��ʼ�� exportButton
        exportButton = new JButton("��������");
        exportButton.addActionListener(e -> exportConfiguration());

        // ��ʼ�����
        envTable = new JBTable();
        refreshEnvTable();

        envPanel = ToolbarDecorator.createDecorator(envTable)
                .setAddAction(anActionButton -> addSetting())
                .setRemoveAction(anActionButton -> removeSetting())
                .addExtraAction(createActionButton("��������", "/META-INF/icon-gen.png", this::generateConfiguration))
                .addExtraAction(createActionButton("��������", "/META-INF/icon-test.png", this::testConnection))
                .createPanel();
    }

    /**
     * �������õķ�����ִ���롰�������á���ť��ص��߼���
     */
    private void importConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("XML files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlFilter);
        fileChooser.setDialogTitle("ѡ�������ļ�");
        int result = fileChooser.showOpenDialog(panel);

        if (result == JFileChooser.APPROVE_OPTION) {
            // �û�ѡ�����ļ�
            java.io.File selectedFile = fileChooser.getSelectedFile();
            // ������ִ�н� XML �ļ����뵽 config ���߼�
            importConfigFromXml(selectedFile);
        }
    }

    /**
     * �� XML �ļ��������õķ�����
     *
     * @param file Ҫ����� XML �ļ�
     */
    private void importConfigFromXml(java.io.File file) {
        if (file.exists() && file.isFile()) {
            try {
                JAXBContext context = JAXBContext.newInstance(EasyEnvConfig.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                EasyEnvConfig importedConfig = (EasyEnvConfig) unmarshaller.unmarshal(file);
                // �����ﴦ��������ö���
                handleImportedConfig(importedConfig);
                this.isModify = true;
                // ����ɹ�����ʾ��ʾ��Ϣ
                Messages.showInfoMessage("����ɹ�", "�ɹ�");
            } catch (JAXBException e) {
                // ����ʧ�ܣ���ʾ������Ϣ
                Messages.showErrorDialog(e.getMessage(), "ʧ��");
                throw new RuntimeException(e);
            }
        } else {
            // �ļ������ڻ����ļ�
            String errorMessage = "ѡ����ļ���Ч����ѡ��һ����Ч�� XML �ļ���";
            Messages.showErrorDialog(errorMessage, "����ʧ��");
        }
    }

    /**
     * ����������ö���ķ�����
     *
     * @param importedConfig ��������ö���
     */
    private void handleImportedConfig(EasyEnvConfig importedConfig) {
        config.setSeeConnectInfoMap(importedConfig.getSeeConnectInfoMap());
        config.setConfigReplaceRuleMap(importedConfig.getConfigReplaceRuleMap());
        config.setExcludedFileMap(importedConfig.getExcludedFileMap());
        refreshEnvTable();

        EasyEnvRuleSettingsView instance = EasyEnvRuleSettingsView.getInstance(config);
        instance.refreshReplaceRuleTable();
        instance.refreshExcludedFileTable();
    }

    /**
     * �������õķ�����ִ���롰�������á���ť��ص��߼���
     */
    private void exportConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("ѡ�񵼳�Ŀ¼");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showSaveDialog(panel);

        if (result == JFileChooser.APPROVE_OPTION) {
            // �û�ѡ����Ŀ¼
            java.io.File selectedDirectory = fileChooser.getSelectedFile();

            // ��ʾ�û������ļ���������ʹ��Ĭ���ļ���
            String defaultFileName = "easyEnv";
            String fileName = Messages.showInputDialog(
                    "�������ļ�����������չ��������ȷ��ʹ��Ĭ���ļ���:",
                    "�����ļ���",
                    Messages.getQuestionIcon(),
                    defaultFileName,
                    null);

            if (fileName != null && !fileName.trim().isEmpty()) {
                // �û��ṩ���ļ�������� .xml ��չ��
                fileName += ".xml";
                // �û��ṩ���ļ���
                java.io.File outputFile = new java.io.File(selectedDirectory, fileName);
                exportConfigToDirectory(outputFile);
            }
        }
    }

    /**
     * �����õ�����Ŀ¼�ķ�����
     *
     * @param outputFile Ҫ��������Ŀ¼
     */
    private void exportConfigToDirectory(java.io.File outputFile) {
        if (outputFile.exists()) {
            // �ļ��Ѵ��ڣ�ѯ���û��Ƿ񸲸�
            int result = Messages.showOkCancelDialog(
                    "�ļ��Ѵ��ڣ��Ƿ񸲸ǣ�",
                    "�ļ��Ѵ���",
                    Messages.getQuestionIcon());

            if (result != Messages.OK) {
                // �û�ȡ���˸��ǲ���
                return;
            }
        }

        JAXBContext context = null;
        try {
            context = JAXBContext.newInstance(EasyEnvConfig.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(config, outputFile);

            // �����ɹ�����ʾ��ʾ��Ϣ
            String successMessage = "���óɹ�������: " + outputFile.getAbsolutePath();
            Messages.showInfoMessage(successMessage, "�����ɹ�");
        } catch (JAXBException e) {
            // ����ʧ�ܣ���ʾ������Ϣ
            String errorMessage = "��������ʱ��������: " + e.getMessage();
            Messages.showErrorDialog(errorMessage, "����ʧ��");
            throw new RuntimeException(e);
        }
    }

    /**
     * ������õķ�������ʾ���öԻ��򲢽���������ӵ������С�
     */
    private void addSetting() {
        if (config != null) {
            SettingAddView settingAddView = new SettingAddView();
            if (settingAddView.showAndGet()) {
                Map.Entry<String, EasyEnvConfig.SeeConnectInfo> entry = settingAddView.getEntry();
                config.getSeeConnectInfoMap().put(entry.getKey(), entry.getValue());
                refreshEnvTable();
                this.isModify = true;
            }
        }
    }

    /**
     * ɾ�����õķ�������������ɾ��ѡ�������á�
     */
    private void removeSetting() {
        if (config != null) {
            int selectedRow = envTable.getSelectedRow();
            if (selectedRow != -1) {
                Map<String, EasyEnvConfig.SeeConnectInfo> customMap = config.getSeeConnectInfoMap();
                customMap.remove(envTable.getValueAt(selectedRow, 0).toString());
                refreshEnvTable();
                this.isModify = true;
            } else {
                showInfoMessage("��ѡ��һ��");
            }
        }
    }

    /**
     * ��������ͼ��� AnActionButton �ķ�����
     */
    private AnActionButton createActionButton(String text, String iconPath, Consumer<AnActionEvent> action) {
        return new AnActionButton(text, IconLoader.getIcon(iconPath, Objects.requireNonNull(ReflectionUtil.getGrandCallerClass()))) {

            @Override
            public void actionPerformed(AnActionEvent e) {
                action.accept(e);
            }
        };
    }

    /**
     * �������õķ�����ִ���롰�������á���ť��ص��߼���
     */
    private void generateConfiguration(AnActionEvent e) {
        int selectedRow = envTable.getSelectedRow();
        if (selectedRow != -1) {
            // ������ִ�С��������ӡ���ť���߼�
            String address = (String) envTable.getValueAt(selectedRow, 2);
            String username = (String) envTable.getValueAt(selectedRow, 3);
            String password = (String) envTable.getValueAt(selectedRow, 4);

            SeeConfig seeConfig = new SeeConfig(address, username, password);
            Project project = e.getProject();
            MyPluginLoader myPluginLoader = new MyPluginLoader(project, seeConfig);
            myPluginLoader.startBlockingLoadingProcess();
        } else {
            showInfoMessage("��ѡ��һ��");
        }
    }

    /**
     * �������ӵķ�����ִ���롰�������ӡ���ť��ص��߼���
     */
    private void testConnection(AnActionEvent e) {
        try {
            int selectedRow = envTable.getSelectedRow();
            if (selectedRow != -1) {
                // ������ִ�С��������ӡ���ť���߼�
                String address = (String) envTable.getValueAt(selectedRow, 2);
                String username = (String) envTable.getValueAt(selectedRow, 3);
                String password = (String) envTable.getValueAt(selectedRow, 4);

                SeeConfig seeConfig = new SeeConfig(address, username, password);
                SeeRequest.login(seeConfig);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage("���ӳɹ�", "��ʾ");
                });
            } else {
                showInfoMessage("��ѡ��һ��");
            }
        } catch (Exception ex) {
            String errMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog("����ʧ�ܣ����顣" + errMsg, "����");
            });
            throw new RuntimeException(ex);
        }
    }

    /**
     * ˢ�»������ķ�����
     */
    private void refreshEnvTable() {
        Map<String, EasyEnvConfig.SeeConnectInfo> customMap = Maps.newHashMap();
        if (config != null && config.getSeeConnectInfoMap() != null) {
            customMap = config.getSeeConnectInfoMap();
        }

        DefaultTableModel customModel = getEnvTableModel(customMap);
        envTable.setModel(customModel);
        envTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        envTable.getColumnModel().getColumn(0).setPreferredWidth((int) (envTable.getWidth() * 0.3));
        envTable.getColumnModel().getColumn(0).setWidth(0);
        envTable.getColumnModel().getColumn(0).setMinWidth(0);
        envTable.getColumnModel().getColumn(0).setMaxWidth(0);
    }

    /**
     * ��ȡ�������ģ�͵ķ�����
     */
    private DefaultTableModel getEnvTableModel(Map<String, EasyEnvConfig.SeeConnectInfo> customMap) {
        Vector<Vector<String>> customData = new Vector<>(customMap.size());
        for (Map.Entry<String, EasyEnvConfig.SeeConnectInfo> entry : customMap.entrySet()) {
            String key = entry.getKey();
            EasyEnvConfig.SeeConnectInfo value = entry.getValue();
            Vector<String> row = new Vector<>(5);
            row.add(key);
            row.add(value.getLabel());
            row.add(value.getAddress());
            row.add(value.getUsername());
            row.add(value.getPassword());
            customData.add(row);
        }
        return new DefaultTableModel(customData, headers1);
    }

    /**
     * �ж��Ƿ����޸ġ�
     *
     * @return ������޸ģ�����true�����򣬷���false��
     */
    public boolean isModified() {
        return isModify;
    }

    /**
     * ��ʾ��Ϣ��ʾ��ķ�����
     *
     * @param message Ҫ��ʾ����Ϣ��
     */
    private void showInfoMessage(String message) {
        Messages.showInfoMessage(message, "��ʾ");
    }
}
