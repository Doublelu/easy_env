package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.github.hanfeng21050.utils.MyPluginLoader;
import com.google.common.collect.Maps;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * CommonSettingsView �ṩ��һ��ͨ��������ͼ��������ӡ�ɾ�����������úͲ������ӵȲ�����
 *
 * @Author hanfeng32305
 * @Date 2023/10/30 17:24
 */
public class EasyEnvSettingsView extends AbstractTemplateSettingsView {
    private final JPanel panel;
    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    private final JTable customTable;

    private boolean isModify = false;

    /**
     * CommonSettingsView ���캯������ʼ����ͼ����ز�����
     */
    public EasyEnvSettingsView() {
        super();
        customTable = new JBTable();
        refreshCustomTable();

        panel = ToolbarDecorator.createDecorator(customTable)
                .setAddAction(anActionButton -> addSetting())
                .setRemoveAction(anActionButton -> removeSetting())
                .addExtraAction(createActionButton("��������", "/META-INF/icon-gen.png", this::generateConfiguration))
                .addExtraAction(createActionButton("��������", "/META-INF/icon-test.png", this::testConnection))
                .createPanel();
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
                refreshCustomTable();
                this.isModify = true;
            }
        }
    }

    /**
     * ɾ�����õķ�������������ɾ��ѡ�������á�
     */
    private void removeSetting() {
        if (config != null) {
            int selectedRow = customTable.getSelectedRow();
            if (selectedRow != -1) {
                Map<String, EasyEnvConfig.SeeConnectInfo> customMap = config.getSeeConnectInfoMap();
                customMap.remove(customTable.getValueAt(selectedRow, 0).toString());
                refreshCustomTable();
                this.isModify = true;
            } else {
                showInfoMessage("��ѡ��һ��");
            }
        }
    }

    /**
     * �������õķ�����ִ���롰�������á���ť��ص��߼���
     */
    private void generateConfiguration(AnActionEvent e) {
        int selectedRow = customTable.getSelectedRow();
        if (selectedRow != -1) {
            // ������ִ�С��������ӡ���ť���߼�
            String address = (String) customTable.getValueAt(selectedRow, 2);
            String username = (String) customTable.getValueAt(selectedRow, 3);
            String password = (String) customTable.getValueAt(selectedRow, 4);

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
            int selectedRow = customTable.getSelectedRow();
            if (selectedRow != -1) {
                // ������ִ�С��������ӡ���ť���߼�
                String address = (String) customTable.getValueAt(selectedRow, 2);
                String username = (String) customTable.getValueAt(selectedRow, 3);
                String password = (String) customTable.getValueAt(selectedRow, 4);

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
     * ��ʾ��Ϣ��ʾ��ķ�����
     */
    private void showInfoMessage(String message) {
        Messages.showInfoMessage(message, "��ʾ");
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
     * ��ȡ��ͼ����ķ�����
     *
     * @return ��ͼ���
     */
    public JComponent getComponent() {
        return panel;
    }

    /**
     * ˢ����ͼ�ķ�����
     */
    public void refresh() {
        // �������������ˢ���߼�
    }

    /**
     * ˢ�±�����ݵķ������������ж�ȡ�Զ�����������±��
     */
    private void refreshCustomTable() {
        Map<String, EasyEnvConfig.SeeConnectInfo> customMap = Maps.newHashMap();
        if (config != null && config.getSeeConnectInfoMap() != null) {
            customMap = config.getSeeConnectInfoMap();
        }
        DefaultTableModel customModel = getDefaultTableModel(customMap);
        customTable.setModel(customModel);
        customTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customTable.getColumnModel().getColumn(0).setPreferredWidth((int) (customTable.getWidth() * 0.3));
        customTable.getColumnModel().getColumn(0).setWidth(0);
        customTable.getColumnModel().getColumn(0).setMinWidth(0);
        customTable.getColumnModel().getColumn(0).setMaxWidth(0);

        customModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                if (e.getType() == TableModelEvent.UPDATE) {
                    String key = (String) customModel.getValueAt(row, 0);
                    String label = (String) customModel.getValueAt(row, 1);
                    String address = (String) customModel.getValueAt(row, 2);
                    String username = (String) customModel.getValueAt(row, 3);
                    String password = (String) customModel.getValueAt(row, 4);

                    EasyEnvConfig.SeeConnectInfo seeConnectInfo = new EasyEnvConfig.SeeConnectInfo(label, address, username, password);
                    if (config != null) {
                        config.getSeeConnectInfoMap().put(key, seeConnectInfo);
                    }
                }
            }
        });
    }

    /**
     * �������ж�ȡ���ݵķ���������Ĭ�ϱ��ģ�͡�
     *
     * @param customMap �Զ������ӳ��
     * @return ���ģ��
     */
    @NotNull
    private DefaultTableModel getDefaultTableModel(Map<String, EasyEnvConfig.SeeConnectInfo> customMap) {
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
     * �ж��޸�״̬
     * @return
     */
    public boolean isModified() {
        return isModify;
    }
}