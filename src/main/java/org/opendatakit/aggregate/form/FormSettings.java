package org.opendatakit.aggregate.form;


/**
 * Created by Anna on 2015-07-27.
 */
public class FormSettings {

    public enum Recurrence {ONCE, DAILY, WEEKLY, MONTHLY}



    private Recurrence recurrence;

    private Boolean manualRecurrence;

    public FormSettings(Recurrence rec, Boolean man) {
        recurrence = rec;
        manualRecurrence = man;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public Boolean isManualRecurrence() {
        return manualRecurrence;
    }




}
