package com.example.peter.graddraft.data;

/**
 * Created by Peter on 5/6/2016.
 */
public class NodeModel {

    public String dateCreated,type,owner;
    public Boolean active,alarm;
    public  int id;

    public void setCreated(String cre) {
        this.dateCreated = cre;
    }
    public void setType(String typ) {
        this.type = typ;
    }
    public void setOwner(String own) {
        this.owner = own;
    }
    public void setActive(Boolean act) {
        this.active = act;
    }
    public void setAlarm(Boolean alrm) {
        this.alarm = alrm;
    }
    public void setId(int iD) {
        this.id= iD;
    }

}
