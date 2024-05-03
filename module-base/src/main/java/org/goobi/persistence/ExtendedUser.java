package org.goobi.persistence;

import java.util.Date;
import java.util.Map;

import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Institution;
import org.goobi.beans.User;

import lombok.Getter;
import lombok.Setter;

public class ExtendedUser implements DatabaseObject {

    private static final long serialVersionUID = 8661576682486678861L;

    @Getter
    private User user;
    @Getter
    private Institution institution;

    @Getter
    @Setter
    private Date lastUploadDate;

    @Getter
    @Setter
    private int numberOfUploads;

    @Getter @Setter
    private boolean dnbUser;

    public ExtendedUser(User user) {
        super();
        this.user = user;
        this.institution = user.getInstitution();
    }

    @Override
    public void lazyLoad() {
        // nothing
    }

    public Map<String, String> getAdditionalData() {
        return institution.getAdditionalData();
    }

    public String getLongName() {
        return institution.getLongName();
    }

    public void setLongName(String value) {
        institution.setLongName(value);
    }

    public String getShortName() {
        return institution.getShortName();
    }

    public void setShortName(String value) {
        institution.setShortName(value);
    }

    public Integer getId() {
        return institution.getId();
    }

    public boolean isActive() {
        return user.isActive();
    }

}
