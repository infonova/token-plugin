package org.jenkinsci.plugins.tokenplugin;

import java.io.Serializable;
import java.util.Date;

public final class SystemStatusInformation implements Serializable {

    private static final long serialVersionUID = -6080786364592349305L;

    public enum Status {
        UNLOCKED, LOCKED;
    }

    private final String userId;
    private final Status status;
    private final Date changeDate;

    public SystemStatusInformation(final String userId, final Status status) {
        this.status = status;
        this.userId = userId;
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

}
