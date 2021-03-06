package de.domjos.ideaMantis.service;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import de.domjos.ideaMantis.model.MantisUser;
import de.domjos.ideaMantis.soap.MantisSoapAPI;
import de.domjos.ideaMantis.utils.Helper;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "ideaMantisSettings",
    storages = {
        @Storage(file = "$PROJECT_FILE$"),
        @Storage(file = "$PROJECT_CONFIG_DIR$/ideaMantis.xml")
    }
)
public class ConnectionSettings implements PersistentStateComponent<Element> {
    private String hostName = "";
    private String userName = "";
    private String password = "";
    private int reloadTime = 300;
    private boolean fastTrack = false, reload = false;
    private int itemsPerPage = 0;
    private int projectID = 0;

    @NotNull
    public static ConnectionSettings getInstance(Project project) {
        return ServiceManager.getService(project, ConnectionSettings.class);
    }


    @Nullable
    @Override
    public Element getState() {
        Element connection = new Element("connection");
        connection.setAttribute("hostName", this.hostName);
        connection.setAttribute("userName", this.userName);
        Helper.setPassword(this.getPassword());
        connection.setAttribute("itemsPerPage", String.valueOf(this.itemsPerPage));
        connection.setAttribute("projectID", String.valueOf(this.getProjectID()));
        connection.setAttribute("fastTrack", String.valueOf(this.fastTrack));
        connection.setAttribute("reload", String.valueOf(this.reload));
        connection.setAttribute("reloadTime", String.valueOf(this.reloadTime));
        return connection;
    }

    @Override
    public void loadState(Element element) {
        this.hostName = element.getAttributeValue("hostName");
        this.userName = element.getAttributeValue("userName");
        this.password = Helper.getPassword();
        this.itemsPerPage = Integer.parseInt(element.getAttributeValue("itemsPerPage", "-1"));
        if(element.getAttributeValue("projectID")!=null) {
            String content = element.getAttributeValue("projectID");
            if(content.equals("")) {
                this.setProjectID(0);
            } else {
                this.setProjectID(Integer.parseInt(content));
            }
        }
        this.fastTrack = Boolean.parseBoolean(element.getAttributeValue("fastTrack"));
        this.reload = Boolean.parseBoolean(element.getAttributeValue("reload"));
        this.reloadTime = Integer.parseInt(element.getAttributeValue("reloadTime"));
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        if(password==null) {
            return "";
        } else {
            return password;
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getProjectID() {
        return projectID;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getItemsPerPage() {
        return this.itemsPerPage;
    }

    public void setProjectID(int projectID) {
        this.projectID = projectID;
    }

    public boolean isFastTrack() {
        return this.fastTrack;
    }

    public void setFastTrack(boolean fastTrack) {
        this.fastTrack = fastTrack;
    }

    public boolean isReload() {
        return this.reload;
    }

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    public int getReloadTime() {
        return this.reloadTime;
    }

    public void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public boolean validateSettings() {
        MantisSoapAPI api = new MantisSoapAPI(this);
        MantisUser user = api.testConnection();
        return user != null && projectID != 0;
    }
}

