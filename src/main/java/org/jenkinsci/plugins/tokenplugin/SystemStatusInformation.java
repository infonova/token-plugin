package org.jenkinsci.plugins.tokenplugin;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class SystemStatusInformation implements Serializable {

    private static final long serialVersionUID = -6080786364592349305L;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public enum Status {
        UNLOCKED, LOCKED;
    }

    private String userId;
    private String headerLink;
    private Status status;
    private Date changeDate;

    public SystemStatusInformation(final String userId, final Status status) {
        this.status = status;
        this.userId = userId;
        this.changeDate = new Date();
    }

    public SystemStatusInformation(SystemStatusInformation systemInformation) {
        this.userId = systemInformation.getUserId();
        this.headerLink = systemInformation.getHeaderLink();
        this.status = systemInformation.getStatus();
        this.changeDate = new Date();
    }

    public Status getStatus() {
        return status;
    }

    public String getUserId() {
        return userId;
    }

    public Date getChangeDate() {
        return changeDate;
    }

    public String getHeaderLink() {
        return headerLink;
    }

    public void setHeaderLink(final String headerLink) {
        this.headerLink = headerLink;
    }

    public String getChangeDateString() {
        return DATE_FORMAT.format(changeDate);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

}
