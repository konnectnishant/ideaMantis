package de.domjos.ideaMantis.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import de.domjos.ideaMantis.model.MantisProject;
import de.domjos.ideaMantis.model.MantisUser;
import de.domjos.ideaMantis.service.ConnectionSettings;
import de.domjos.ideaMantis.soap.MantisSoapAPI;
import de.domjos.ideaMantis.soap.ObjectRef;
import de.domjos.ideaMantis.utils.Helper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class IdeaMantisConfigurable implements SearchableConfigurable {
    private JComponent component;
    private ConnectionSettings settings;
    private JBTextField txtHostName, txtUserName, txtProjectName, txtIssuesPerPage, txtReloadTime;
    private JTextArea txtProjectDescription;
    private JBPasswordField txtPassword;
    private JBCheckBox chkProjectEnabled, chkFastTrackEnabled, chkReloadAutomatically;
    private java.awt.Label lblConnectionState, lblProjectState;
    private JButton cmdTestConnection;
    private JPanel newProjectPanel;
    private ComboBox<String> cmbProjects, cmbNewProjectProjects, cmbProjectViewState;
    private int projectID = 0;
    private ToolWindowManager manager;
    private Task task;
    private GridBagConstraints labelConstraint, txtConstraint;

    public IdeaMantisConfigurable(@NotNull Project project) {
        this.settings = ConnectionSettings.getInstance(project);
        this.manager = ToolWindowManager.getInstance(project);
        this.task = new Task.Backgroundable(project, "Load Data...") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    settings.setHostName(txtHostName.getText());
                    settings.setUserName(txtUserName.getText());
                    StringBuilder buf = new StringBuilder();
                    for(char ch : txtPassword.getPassword()) {
                        buf.append(ch);
                    }
                    settings.setPassword(buf.toString());
                    int itemsPerPage;
                    try {
                        itemsPerPage = Integer.parseInt(txtIssuesPerPage.getText());
                    } catch (Exception ex) {
                        itemsPerPage = -1;
                    }
                    settings.setItemsPerPage(itemsPerPage);
                    settings.setProjectID(projectID);
                    ApplicationManager.getApplication().invokeLater(()->{
                        ToolWindow window = manager.getToolWindow("Show MantisBT-Issues");
                        ContentImpl content = new ContentImpl(null, "", true);
                        content.setDescription("reload comboBoxes");
                        window.getContentManager().addContent(content);
                    });
                    settings.setFastTrack(chkFastTrackEnabled.isSelected());
                    settings.setReload(chkReloadAutomatically.isSelected());

                    int reloadTime;
                    try {
                        reloadTime = Integer.parseInt(txtReloadTime.getText());
                    } catch (Exception ex) {
                        reloadTime = 300;
                    }
                    settings.setReloadTime(reloadTime);
                } catch (Exception ex) {
                    Helper.printException(ex);
                } finally {
                    component.setCursor(Cursor.getDefaultCursor());
                }
            }
        };
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
        return "IdeaMantis-Settings";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        this.labelConstraint = new GridBagConstraints();
        this.labelConstraint.anchor = GridBagConstraints.EAST;
        this.labelConstraint.insets = JBUI.insets(5, 10);
        this.txtConstraint = new GridBagConstraints();
        this.txtConstraint.weightx = 2.0;
        this.txtConstraint.fill = GridBagConstraints.HORIZONTAL;
        this.txtConstraint.gridwidth = GridBagConstraints.REMAINDER;

        this.txtHostName = new JBTextField();
        this.txtHostName.setName("txtHostName");
        this.txtUserName = new JBTextField();
        this.txtUserName.setName("txtUserName");
        this.txtPassword = new JBPasswordField();
        this.txtPassword.setName("txtPassword");
        this.txtProjectName = new JBTextField();
        this.txtProjectName.setName("txtProjectName");
        this.txtProjectDescription = new JTextArea();
        this.txtProjectDescription.setName("txtProjectDescription");

        this.txtIssuesPerPage = new JBTextField();
        this.txtIssuesPerPage.setName("txtIssuesPerPage");
        this.txtIssuesPerPage.setText("-1");

        java.awt.Label lblHostName = new java.awt.Label("Host-Name");
        java.awt.Label lblUserName = new java.awt.Label("User-Name");
        java.awt.Label lblPassword = new java.awt.Label("Password");
        java.awt.Label lblProjects = new java.awt.Label("Choose Project");
        java.awt.Label lblIssuesPerPage = new java.awt.Label("Issues per page (-1 for all)");
        java.awt.Label lblProjectName = new java.awt.Label("Name" + "*");
        java.awt.Label lblProjectDescription = new java.awt.Label("Description");
        java.awt.Label lblProjectFastTrack = new java.awt.Label("Copy and Paste Bugs faster");
        java.awt.Label lblProjectViewState = new java.awt.Label("State");
        this.lblConnectionState = new Label("Not Connected");
        this.changeConnectionLabel(null);


        this.cmdTestConnection = new JButton("Test Connection");
        this.cmdTestConnection.addActionListener(e -> {
            String oldSettings = this.temporarilyChangeSettings();

            MantisSoapAPI connection = new MantisSoapAPI(this.settings);
            if(this.changeConnectionLabel(connection)) {
                java.util.List<MantisProject> projects = connection.getProjects();
                cmbProjects.removeAllItems();
                cmbNewProjectProjects.removeAllItems();
                for(MantisProject project : projects) {
                    this.addProjectToComboBox(project, ": ");
                }
                cmbNewProjectProjects.addItem("");
                cmbNewProjectProjects.setSelectedItem("");

                cmbProjectViewState.removeAllItems();
                for(ObjectRef item : connection.getEnum("view_states")) {
                    this.cmbProjectViewState.addItem(item.getName());
                }

                for(int i = 0; i<=cmbProjects.getItemCount()-1; i++) {
                    if(cmbProjects.getItemAt(i).startsWith(settings.getProjectID() + ":")) {
                        cmbProjects.setSelectedItem(cmbProjects.getItemAt(i));
                        break;
                    }
                }
            } else {
                cmbProjects.removeAllItems();
            }

            this.temporarilyChangeSettingsBack(oldSettings);
        });

        this.chkFastTrackEnabled = new JBCheckBox("Enable Fast-Track-Mode");

        JPanel connPanel = new JPanel(new GridBagLayout());
        this.addFieldsToPanel(connPanel, Arrays.asList(lblHostName, txtHostName, lblUserName, txtUserName, lblPassword, txtPassword));
        connPanel.add(lblConnectionState, txtConstraint);
        connPanel.add(cmdTestConnection, txtConstraint);
        connPanel.setBorder(IdeBorderFactory.createTitledBorder("Connection"));

        JButton cmdCreateNewProject = new JButton("New Project");
        cmdCreateNewProject.setName("cmdCreateNewProject");

        this.cmbProjects = new ComboBox<>();
        this.cmbProjects.addItemListener(event->{
            if(event.getItem()!=null) {
                projectID = Integer.parseInt(event.getItem().toString().split(":")[0]);
            }
        });

        this.chkReloadAutomatically = new JBCheckBox("Reload bugs after some time!");
        this.chkReloadAutomatically.addActionListener(e -> this.txtReloadTime.setEnabled(this.chkReloadAutomatically.isSelected()));
        this.txtReloadTime = new JBTextField();
        this.txtReloadTime.setName("txtReloadTime");
        this.txtReloadTime.setToolTipText("Time (in s)");
        this.txtReloadTime.setText("300");
        this.txtReloadTime.setEnabled(false);

        JPanel projectPanel = new JPanel(new GridBagLayout());
        this.addFieldsToPanel(projectPanel, Arrays.asList(lblProjects, cmbProjects, lblIssuesPerPage, txtIssuesPerPage, lblProjectFastTrack));
        projectPanel.add(chkFastTrackEnabled, txtConstraint);
        projectPanel.add(cmdCreateNewProject, txtConstraint);
        projectPanel.setBorder(IdeBorderFactory.createTitledBorder("Project"));

        JButton cmdProjectAdd = new JButton(("Add Project"));
        cmdProjectAdd.setName("cmdProjectAdd");

        this.cmbProjectViewState = new ComboBox<>();
        this.chkProjectEnabled = new JBCheckBox("enabled");

        this.cmbNewProjectProjects = new ComboBox<>();
        this.cmbNewProjectProjects.setVisible(false);

        this.lblProjectState = new Label();

        newProjectPanel = new JPanel(new GridBagLayout());
        newProjectPanel.setVisible(false);
        this.addFieldsToPanel(newProjectPanel, Arrays.asList(lblProjectName, txtProjectName, lblProjects, cmbNewProjectProjects, lblProjectDescription, txtProjectDescription, lblProjectViewState, cmbProjectViewState));
        newProjectPanel.add(chkProjectEnabled, txtConstraint);
        newProjectPanel.add(lblProjectState, txtConstraint);
        newProjectPanel.add(cmdProjectAdd, txtConstraint);

        cmdProjectAdd.addActionListener(e -> {
            String oldSettings = this.temporarilyChangeSettings();

            MantisProject project = new MantisProject(txtProjectName.getText());
            project.setDescription(txtProjectDescription.getText());
            if(cmbProjectViewState.getSelectedItem()!=null)
                project.setView_state(cmbProjectViewState.getSelectedItem().toString());
            project.setEnabled(chkProjectEnabled.isSelected());
            MantisSoapAPI connection = new MantisSoapAPI(this.settings);
            int id = 0;
            if(!cmbNewProjectProjects.getSelectedItem().toString().equals("")) {
                if(cmbNewProjectProjects.getSelectedItem()!=null)
                    id = Integer.parseInt(cmbNewProjectProjects.getSelectedItem().toString().split(": ")[0].trim());

                MantisProject parent = getProject(id, connection.getProjects());
                if(parent!=null) {
                    parent.addSubProject(project);
                    project = parent;
                }
            }

            String state = connection.addProject(project);
            if(!state.equals("true")) {
                lblProjectState.setForeground(JBColor.RED);
                lblProjectState.setText(state);
            } else {
                txtProjectDescription.setText("");
                txtProjectName.setText("");
                cmbProjectViewState.setSelectedIndex(0);
                newProjectPanel.setVisible(false);
                cmdTestConnection.doClick();
                for(int i = 0; i<=cmbProjects.getItemCount()-1; i++) {
                    if(cmbProjects.getItemAt(i).endsWith(project.getName())) {
                        cmbProjects.setSelectedItem(cmbProjects.getItemAt(i));
                        break;
                    }
                }
            }

            this.temporarilyChangeSettingsBack(oldSettings);
        });
        newProjectPanel.setBorder(IdeBorderFactory.createTitledBorder("New Project"));

        cmdCreateNewProject.addActionListener(e -> newProjectPanel.setVisible(true));

        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = Helper.getRootConstraint();
        root.add(connPanel, constraints);
        constraints.weighty = 2.0;
        root.add(projectPanel,constraints);
        constraints.weighty = 2.0;
        root.add(newProjectPanel,constraints);
        this.component = root;
        return root;
    }



    private void addProjectToComboBox(MantisProject project, String splitter) {
        cmbProjects.addItem(project.getId() + splitter + project.getName());
        cmbNewProjectProjects.addItem(project.getId() + splitter + project.getName());
        for(MantisProject subProject : project.getSubProjects()) {
            this.addProjectToComboBox(subProject, splitter + "-> ");
        }
    }

    @Override
    public boolean isModified() {
        if(this.settings!=null) {

            StringBuilder buf = new StringBuilder();
            for(char ch : txtPassword.getPassword()) {
                buf.append(ch);
            }

            return
                !this.settings.getHostName().equals(txtHostName.getText()) ||
                !this.settings.getUserName().equals(txtUserName.getText()) ||
                !this.settings.getPassword().equals(buf.toString()) ||
                !String.valueOf(this.settings.getItemsPerPage()).equals(txtIssuesPerPage.getText()) ||
                (this.settings.getProjectID()!=projectID && projectID!=0) ||
                !this.settings.isFastTrack()==chkFastTrackEnabled.isSelected() ||
                !this.settings.isReload()==chkReloadAutomatically.isSelected() ||
                !String.valueOf(this.settings.getReloadTime()).equals(txtReloadTime.getText());
        } else {
            return false;
        }
    }

    @Override
    public void apply() {
        if(this.settings!=null) {
            this.component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            ProgressManager.getInstance().run(this.task);
        }
    }

    @Override
    public void reset() {
        txtHostName.setText(this.settings.getHostName());
        txtUserName.setText(this.settings.getUserName());
        txtPassword.setText(this.settings.getPassword());
        chkFastTrackEnabled.setSelected(this.settings.isFastTrack());
        chkReloadAutomatically.setSelected(this.settings.isReload());
        txtReloadTime.setEnabled(chkReloadAutomatically.isSelected());
        txtReloadTime.setText(String.valueOf(this.settings.getReloadTime()));
        txtIssuesPerPage.setText(String.valueOf(this.settings.getItemsPerPage()));
        cmdTestConnection.doClick();
        for(int i = 0; i<=cmbProjects.getItemCount()-1; i++) {
            if(Integer.parseInt(cmbProjects.getItemAt(i).split(":")[0])==this.settings.getProjectID()) {
                cmbProjects.setSelectedIndex(i);
            }
        }
    }

    @Override
    public void disposeUIResources() {
        UIUtil.dispose(txtHostName);
        UIUtil.dispose(txtUserName);
        UIUtil.dispose(txtPassword);
        UIUtil.dispose(txtIssuesPerPage);
        UIUtil.dispose(cmbProjects);
        UIUtil.dispose(cmdTestConnection);
        UIUtil.dispose(chkFastTrackEnabled);
        UIUtil.dispose(chkReloadAutomatically);
        UIUtil.dispose(txtReloadTime);
    }

    private boolean changeConnectionLabel(MantisSoapAPI api) {
        if(api!=null) {
            MantisUser user = api.testConnection();
            if(user==null) {
                if(api.getCurrentError()!=null) {
                    this.lblConnectionState.setText(api.getCurrentError().faultstring);
                } else {
                    this.lblConnectionState.setText("Not connected!");
                }
                this.lblConnectionState.setForeground(JBColor.RED);
                return false;
            } else {
                this.lblConnectionState.setText(String.format("Connected as %s! Version: %s", user.getUserName(), api.getVersion()));
                this.lblConnectionState.setForeground(JBColor.GREEN);
                return true;
            }
        }
        return false;
    }

    private MantisProject getProject(int id, java.util.List<MantisProject> projects) {
        if(projects.isEmpty()) {
            return null;
        } else {
            if(projects.get(0).getId()==id) {
                return projects.get(0);
            }
            return getProject(id, projects.get(0).getSubProjects());
        }
    }

    private String temporarilyChangeSettings() {
        StringBuilder pwd = new StringBuilder();
        for(char ch : txtPassword.getPassword()) {
            pwd.append(ch);
        }

        String  hostName = this.settings.getHostName(),
                userName = this.settings.getUserName(),
                password = this.settings.getPassword();
        this.settings.setHostName(txtHostName.getText());
        this.settings.setUserName(txtUserName.getText());
        this.settings.setPassword(pwd.toString());

        return String.format("%s;-;%s;-;%s", hostName, userName, password);
    }

    private void temporarilyChangeSettingsBack(String oldSettings) {
        String[] data = oldSettings.split(";-;");
        if(data.length>=3) {
            this.settings.setHostName(data[0].trim());
            this.settings.setUserName(data[1].trim());
            this.settings.setPassword(data[2].trim());
        }
    }

    private void addFieldsToPanel(JPanel panel, java.util.List<Component> components) {
        for(Component component : components) {
            if(component instanceof java.awt.Label) {
                panel.add(component, this.labelConstraint);
            } else {
                panel.add(component, this.txtConstraint);
            }
        }
    }
}

