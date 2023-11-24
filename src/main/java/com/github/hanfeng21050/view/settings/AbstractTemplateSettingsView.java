package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;

import javax.swing.*;
import java.util.Vector;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 9:49
 */
public abstract class AbstractTemplateSettingsView {
    protected static Vector<String> headers1;
    protected static Vector<String> headers2;

    static {
        headers1 = new Vector<>(5);
        headers1.add("uuid");
        headers1.add("����");
        headers1.add("��ַ");
        headers1.add("�û���");
        headers1.add("����");

        headers2 = new Vector<>(4);
        headers2.add("uuid");
        headers2.add("�ļ���");
        headers2.add("������ʽ");
        headers2.add("�滻�ı� ");

    }

    protected EasyEnvConfig config;

    public AbstractTemplateSettingsView() {
    }

    public AbstractTemplateSettingsView(EasyEnvConfig config) {
        this.config = config;
    }

    public abstract JComponent getComponent();
}
