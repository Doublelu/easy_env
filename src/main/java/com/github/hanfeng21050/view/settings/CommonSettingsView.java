package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.google.common.collect.Maps;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.Vector;

/**
 * @Author hanfeng32305
 * @Date 2023/10/30 17:24
 */
public class CommonSettingsView extends AbstractTemplateSettingsView {
    private final JPanel panel;
    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    private final JTable customTable;

    public CommonSettingsView() {
        super();
        customTable = new JBTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        refreshCustomTable();

        panel = ToolbarDecorator.createDecorator(customTable)
                .setAddAction(anActionButton -> {
                    if (config != null) {
                        SettingAddView settingAddView = new SettingAddView();
                        if(settingAddView.showAndGet()) {
                            Map.Entry<String, EasyEnvConfig.CustomValue> entry = settingAddView.getEntry();
                            config.getCustomMap().put(entry.getKey(), entry.getValue());
                            refreshCustomTable();
                        }
                    }
                })
                .setRemoveAction(anActionButton -> {
                    if (config != null) {
                        Map<String, EasyEnvConfig.CustomValue> customMap = config.getCustomMap();
                        customMap.remove(customTable.getValueAt(customTable.getSelectedRow(), 0).toString());
                        refreshCustomTable();
                    }
                }).addExtraAction(new AnActionButton("���������ļ�") {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        // ������ִ���Զ������ð�ť�Ĳ���
                        // ���Դ��Զ������öԻ����ִ����������
                    }
                }) // ���Զ��尴ť��ӵ�������
                .createPanel();

    }


    public JComponent getComponent() {
        return panel;
    }

    public void refresh() {

    }

    /**
     * ˢ�±������
     */
    private void refreshCustomTable() {
        // ��ʼ���Զ���������
        Map<String, EasyEnvConfig.CustomValue> customMap = Maps.newHashMap();
        if (config != null && config.getCustomMap() != null) {
            customMap = config.getCustomMap();
        }
        DefaultTableModel customModel = getDefaultTableModel(customMap);
        customTable.setModel(customModel);
        customTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customTable.getColumnModel().getColumn(0).setPreferredWidth((int)(customTable.getWidth() * 0.3));
        customTable.getColumnModel().getColumn(0).setWidth(0);
        customTable.getColumnModel().getColumn(0).setMinWidth(0);
        customTable.getColumnModel().getColumn(0).setMaxWidth(0);
    }

    /**
     * �������ж�ȡ
     * @param customMap
     * @return
     */
    @NotNull
    private DefaultTableModel getDefaultTableModel(Map<String, EasyEnvConfig.CustomValue> customMap) {
        Vector<Vector<String>> customData = new Vector<>(customMap.size());
        for (Map.Entry<String, EasyEnvConfig.CustomValue> entry : customMap.entrySet()) {
            String key = entry.getKey();
            EasyEnvConfig.CustomValue value = entry.getValue();
            Vector<String> row = new Vector<>(5);
            row.add(key);
            row.add(value.getLabel());
            row.add(value.getAddress());
            row.add(value.getUsername());
            row.add(value.getPassword());
            customData.add(row);
        }
        return new DefaultTableModel(customData, customNames);
    }
}
