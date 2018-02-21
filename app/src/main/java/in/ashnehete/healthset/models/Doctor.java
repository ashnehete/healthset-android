package in.ashnehete.healthset.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by Aashish Nehete on 07-Feb-18.
 */

@IgnoreExtraProperties
public class Doctor {
    String id;
    String name;

    public Doctor() {
    }

    public Doctor(String id, String name) {

        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
