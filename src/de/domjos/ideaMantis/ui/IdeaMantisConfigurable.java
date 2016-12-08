package de.domjos.ideaMantis.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import de.domjos.ideaMantis.model.MantisProject;
import de.domjos.ideaMantis.model.MantisUser;
import de.domjos.ideaMantis.service.ConnectionSettings;
import de.domjos.ideaMantis.soap.MantisSoapAPI;
import de.domjos.ideaMantis.utils.Helper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class IdeaMantisConfigurable implements SearchableConfigurable {
    private JBTextField txtHostName, txtUserName;
    private JBPasswordField txtPassword;
    private java.awt.Label lblConnectionState;
    private JButton cmdTestConnection;
    private ComboBox<String> cmbProjects;
    private int projectID = 0;
    private Project project;
    private ResourceBundle bundle;

    public IdeaMantisConfigurable(@NotNull Project project) {
        this.project = project;
        this.bundle = Helper.getBundle();
    }

    @NotNull
    @Override
    public String getId() {
        return getClass().getName();
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return bundle.getString("settings.header");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        GridBagConstraints labelConstraint = new GridBagConstraints();
        labelConstraint.anchor = GridBagConstraints.EAST;
        labelConstraint.insets = JBUI.insets(5, 10);
        GridBagConstraints txtConstraint = new GridBagConstraints();
        txtConstraint.weightx = 2.0;
        txtConstraint.fill = GridBagConstraints.HORIZONTAL;
        txtConstraint.gridwidth = GridBagConstraints.REMAINDER;

        this.txtHostName = new JBTextField();
        this.txtHostName.setName("txtHostName");
        this.txtUserName = new JBTextField();
        this.txtUserName.setName("txtHUserName");
        this.txtPassword = new JBPasswordField();
        this.txtPassword.setName("txtPassword");

        java.awt.Label lblHostName = new java.awt.Label(bundle.getString("settings.hostName"));
        java.awt.Label lblUserName = new java.awt.Label(bundle.getString("settings.userName"));
        java.awt.Label lblPassword = new java.awt.Label(bundle.getString("settings.password"));
        java.awt.Label lblProjects = new java.awt.Label(bundle.getString("settings.chooseProject"));
        this.lblConnectionState = new Label(bundle.getString("settings.connection.notConnected"));
        this.changeConnectionLabel(null);

        this.cmdTestConnection = new JButton(bundle.getString("settings.connection.test"));
        this.cmdTestConnection.addActionListener(e -> {
            MantisSoapAPI connection = new MantisSoapAPI(ConnectionSettings.getInstance(this.project));
            String pwd = "";
            for(char ch : txtPassword.getPassword()) {
                pwd += ch;
            }
            if(this.changeConnectionLabel(connection.testConnection(txtHostName.getText(), txtUserName.getText(), pwd))) {
                java.util.List<MantisProject> projects = connection.getProjects();
                cmbProjects.removeAllItems();
                for(MantisProject project : projects) {
                    cmbProjects.addItem(project.getId() + ": " + project.getName());
                    for(MantisProject subProject : project.getSubProjects()) {
                        cmbProjects.addItem(subProject.getId() + ": -> " + subProject.getName());
                    }
                }
            } else {
                cmbProjects.removeAllItems();
            }
        });

        this.cmbProjects = new ComboBox<>();
        this.cmbProjects.addItemListener(event->{
            if(event.getItem()!=null) {
                projectID = Integer.parseInt(event.getItem().toString().split(":")[0]);
            }
        });

        JPanel connPanel = new JPanel(new GridBagLayout());
        connPanel.add(lblHostName, labelConstraint);
        connPanel.add(txtHostName, txtConstraint);
        connPanel.add(lblUserName, labelConstraint);
        connPanel.add(txtUserName, txtConstraint);
        connPanel.add(lblPassword, labelConstraint);
        connPanel.add(txtPassword, txtConstraint);
        connPanel.add(lblConnectionState, txtConstraint);
        connPanel.add(cmdTestConnection, txtConstraint);
        connPanel.setBorder(IdeBorderFactory.createTitledBorder(bundle.getString("settings.connection.header")));

        JPanel projectPanel = new JPanel(new GridBagLayout());
        projectPanel.add(lblProjects, labelConstraint);
        projectPanel.add(cmbProjects, txtConstraint);
        projectPanel.setBorder(IdeBorderFactory.createTitledBorder(bundle.getString("settings.connection.project")));

        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 2.0;
        constraints.weighty = 0.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;

        root.add(connPanel, constraints);
        constraints.weighty = 2.0;
        root.add(projectPanel,constraints);
        return root;
    }

    @Override
    public boolean isModified() {
        ConnectionSettings connection = ConnectionSettings.getInstance(this.project);
        if(connection!=null) {

            StringBuffer buf = new StringBuffer();
            for(char ch : txtPassword.getPassword()) {
                buf.append(ch);
            }
            return
                    !connection.getHostName().equals(txtHostName.getText()) ||
                            !connection.getUserName().equals(txtUserName.getText()) ||
                            !connection.getPassword().equals(buf.toString()) ||
                            (connection.getProjectID()!=projectID && projectID!=0);
        } else {
            return false;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        ConnectionSettings connection = ConnectionSettings.getInstance(this.project);
        if(connection!=null) {
            connection.setHostName(txtHostName.getText());
            connection.setUserName(txtUserName.getText());
            StringBuffer buf = new StringBuffer();
            for(char ch : txtPassword.getPassword()) {
                buf.append(ch);
            }
            connection.setPassword(buf.toString());
            connection.setProjectID(projectID);
        }
    }

    @Override
    public void reset() {
        ConnectionSettings connection = ConnectionSettings.getInstance(this.project);
        txtHostName.setText(connection.getHostName());
        txtUserName.setText(connection.getUserName());
        txtPassword.setText(connection.getPassword());
        cmdTestConnection.doClick();
        for(int i = 0; i<=cmbProjects.getItemCount()-1; i++) {
            if(Integer.parseInt(cmbProjects.getItemAt(i).split(":")[0])==connection.getProjectID()) {
                cmbProjects.setSelectedIndex(i);
            }
        }
    }

    @Override
    public void disposeUIResources() {
        UIUtil.dispose(txtHostName);
        UIUtil.dispose(txtUserName);
        UIUtil.dispose(txtPassword);
        UIUtil.dispose(cmbProjects);
        UIUtil.dispose(cmdTestConnection);
    }

    private boolean changeConnectionLabel(MantisUser user) {
        if(user==null) {
            this.lblConnectionState.setText(bundle.getString("settings.connection.notConnected"));
            this.lblConnectionState.setForeground(JBColor.RED);
            return false;
        } else {
            this.lblConnectionState.setText(String.format(bundle.getString("settings.connection.connected"), user.getName()));
            this.lblConnectionState.setForeground(JBColor.GREEN);
            return true;
        }
    }
}
