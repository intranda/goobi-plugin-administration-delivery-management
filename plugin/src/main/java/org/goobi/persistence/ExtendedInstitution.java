package org.goobi.persistence;

import java.util.Date;
import java.util.Map;

import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Institution;

import lombok.Getter;
import lombok.Setter;

public class ExtendedInstitution implements DatabaseObject{

    @Getter
    private Institution institution;

    @Getter
    @Setter
    private Date lastUploadDate;

    @Getter
    @Setter
    private int numberOfUploads;

    public ExtendedInstitution(Institution institution) {
        super();
        this.institution = institution;

    }

    @Override
    public void lazyLoad() {
        // nothing
    }


    public Map<String, String>getAdditionalData() {
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



}
