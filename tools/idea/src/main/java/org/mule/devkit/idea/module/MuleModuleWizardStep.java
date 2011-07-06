package org.mule.devkit.idea.module;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.indices.MavenIndicesManager;
import org.jetbrains.idea.maven.model.MavenArchetype;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.navigator.SelectMavenProjectDialog;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MuleModuleWizardStep extends ModuleWizardStep {
    private static final Icon WIZARD_ICON = IconLoader.getIcon("/addmodulewizard.png");

    private static final String INHERIT_GROUP_ID_KEY = "MavenModuleWizard.inheritGroupId";
    private static final String INHERIT_VERSION_KEY = "MavenModuleWizard.inheritVersion";

    private final Project myProjectOrNull;
    private final MuleModuleBuilder myBuilder;
    private MavenProject myAggregator;
    private MavenProject myParent;

    private String myInheritedGroupId;
    private String myInheritedVersion;

    private JPanel myMainPanel;

    private JLabel myAggregatorLabel;
    private JLabel myAggregatorNameLabel;
    private JButton mySelectAggregator;

    private JLabel myParentLabel;
    private JLabel myParentNameLabel;
    private JButton mySelectParent;

    private JTextField myGroupIdField;
    private JCheckBox myInheritGroupIdCheckBox;
    private JTextField myArtifactIdField;
    private JTextField myVersionField;
    private JCheckBox myInheritVersionCheckBox;

    private JTextField myModuleName;
    private JTextField myModulePackage;

    public MuleModuleWizardStep(@Nullable Project project, MuleModuleBuilder builder) {
        myProjectOrNull = project;
        myBuilder = builder;

        initComponents();
        loadSettings();
    }

    private void initComponents() {
        mySelectAggregator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myAggregator = doSelectProject(myAggregator);
                updateComponents();
            }
        });

        mySelectParent.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myParent = doSelectProject(myParent);
                updateComponents();
            }
        });

        ActionListener updatingListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        };
        myInheritGroupIdCheckBox.addActionListener(updatingListener);
        myInheritVersionCheckBox.addActionListener(updatingListener);

        myModuleName.addKeyListener( new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {
            }

            public void keyPressed(KeyEvent keyEvent) {
            }

            public void keyReleased(KeyEvent keyEvent) {
                myArtifactIdField.setText("mule-module-" + myModuleName.getText().toLowerCase());
                myModulePackage.setText("org.mule.modules." + myModuleName.getText().toLowerCase());
            }
        });
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myGroupIdField;
    }

    private MavenProject doSelectProject(MavenProject current) {
        assert myProjectOrNull != null : "must not be called when creating a new project";

        SelectMavenProjectDialog d = new SelectMavenProjectDialog(myProjectOrNull, current);
        d.show();
        if (!d.isOK()) return current;
        return d.getResult();
    }

    @Override
    public void onStepLeaving() {
        saveSettings();
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
    }

    private void loadSettings() {
        myBuilder.setInheritedOptions(getSavedValue(INHERIT_GROUP_ID_KEY, true),
                getSavedValue(INHERIT_VERSION_KEY, true));
    }

    private void saveSettings() {
        saveValue(INHERIT_GROUP_ID_KEY, myInheritGroupIdCheckBox.isSelected());
        saveValue(INHERIT_VERSION_KEY, myInheritVersionCheckBox.isSelected());
    }

    private boolean getSavedValue(String key, boolean defaultValue) {
        return getSavedValue(key, String.valueOf(defaultValue)).equals(String.valueOf(true));
    }

    private String getSavedValue(String key, String defaultValue) {
        String value = PropertiesComponent.getInstance().getValue(key);
        return value == null ? defaultValue : value;
    }

    private void saveValue(String key, boolean value) {
        saveValue(key, String.valueOf(value));
    }

    private void saveValue(String key, String value) {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(key, value);
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    @Override
    public void updateStep() {
        if (isMavenizedProject()) {
            MavenProject parent = myBuilder.findPotentialParentProject(myProjectOrNull);
            myAggregator = parent;
            myParent = parent;
        }

        myArtifactIdField.setText(myBuilder.getDefaultProjectId().getArtifactId());
        myGroupIdField.setText(myParent == null ? myBuilder.getDefaultProjectId().getGroupId() : myParent.getMavenId().getGroupId());
        myVersionField.setText(myParent == null ? myBuilder.getDefaultProjectId().getVersion() : myParent.getMavenId().getVersion());

        myInheritGroupIdCheckBox.setSelected(myBuilder.isInheritGroupId());
        myInheritVersionCheckBox.setSelected(myBuilder.isInheritVersion());

        updateComponents();
    }

    private boolean isMavenizedProject() {
        return myProjectOrNull != null && MavenProjectsManager.getInstance(myProjectOrNull).isMavenizedProject();
    }

    private void updateComponents() {
        if (!isMavenizedProject()) {
            myAggregatorLabel.setEnabled(false);
            myAggregatorNameLabel.setEnabled(false);
            mySelectAggregator.setEnabled(false);

            myParentLabel.setEnabled(false);
            myParentNameLabel.setEnabled(false);
            mySelectParent.setEnabled(false);
        }
        myAggregatorNameLabel.setText(formatProjectString(myAggregator));
        myParentNameLabel.setText(formatProjectString(myParent));

        if (myParent == null) {
            myGroupIdField.setEnabled(true);
            myVersionField.setEnabled(true);
            myInheritGroupIdCheckBox.setEnabled(false);
            myInheritVersionCheckBox.setEnabled(false);
        } else {
            myGroupIdField.setEnabled(!myInheritGroupIdCheckBox.isSelected());
            myVersionField.setEnabled(!myInheritVersionCheckBox.isSelected());

            if (myInheritGroupIdCheckBox.isSelected()
                    || myGroupIdField.getText().equals(myInheritedGroupId)) {
                myGroupIdField.setText(myParent.getMavenId().getGroupId());
            }
            if (myInheritVersionCheckBox.isSelected()
                    || myVersionField.getText().equals(myInheritedVersion)) {
                myVersionField.setText(myParent.getMavenId().getVersion());
            }
            myInheritedGroupId = myGroupIdField.getText();
            myInheritedVersion = myVersionField.getText();

            myInheritGroupIdCheckBox.setEnabled(true);
            myInheritVersionCheckBox.setEnabled(true);
        }
    }

    private String formatProjectString(MavenProject project) {
        if (project == null) return "<none>";
        return project.getMavenId().getDisplayString();
    }

    @Override
    public void updateDataModel() {
        myBuilder.setAggregatorProject(myAggregator);
        myBuilder.setParentProject(myParent);

        myBuilder.setProjectId(new MavenId(myGroupIdField.getText(),
                myArtifactIdField.getText(),
                myVersionField.getText()));
        myBuilder.setInheritedOptions(myInheritGroupIdCheckBox.isSelected(),
                myInheritVersionCheckBox.isSelected());

        myBuilder.setModuleName(myModuleName.getText());
        myBuilder.setModulePackage(myModulePackage.getText());
    }

    @Override
    public Icon getIcon() {
        return WIZARD_ICON;
    }

    @Override
    public String getHelpId() {
        return "reference.dialogs.new.project.fromScratch.maven";
    }
}

