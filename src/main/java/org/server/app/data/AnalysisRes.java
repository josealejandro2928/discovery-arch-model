package org.server.app.data;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

@Entity("analysis_res")
public class AnalysisRes {
    @Id
    private ObjectId id;
    private List<String> dataLogs;
    private List<String> errorLogs;
    @Reference
    private UserModel user;

    public Date createdAt;
    public Date editedAt;


    public AnalysisRes() {

    }

    public String getId() {
        return id.toString();
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<String> getDataLogs() {
        return dataLogs;
    }

    public void setDataLogs(List<String> dataLogs) {
        this.dataLogs = dataLogs;
    }

    public List<String> getErrorLogs() {
        return errorLogs;
    }

    public void setErrorLogs(List<String> errorLogs) {
        this.errorLogs = errorLogs;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    @PrePersist
    public void prePersist() {
        createdAt = (createdAt == null) ? new Date() : createdAt;
        editedAt = (editedAt == null) ? createdAt : new Date();
    }

}
