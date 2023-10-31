package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;

import javax.swing.*;
import java.util.Vector;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 9:49
 */
public abstract class AbstractTemplateSettingsView {
    protected static Vector<String> customNames;

    static {
        customNames = new Vector<>(3);
        customNames.add("��ַ");
        customNames.add("�û���");
        customNames.add("����");
    }

    protected EasyEnvConfig config;

    public AbstractTemplateSettingsView() {
    }

    public AbstractTemplateSettingsView(EasyEnvConfig config) {
        this.config = config;
    }

    public abstract JComponent getComponent();
}
