package in.ashnehete.healthset.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static in.ashnehete.healthset.AppConstants.DATE_FORMAT;

/**
 * Created by Aashish Nehete on 09-Feb-18.
 */

@IgnoreExtraProperties
public class Record {
    private String device;
    private String timestamp;
    private String value;

    public Record(String device) {
        this.device = device;
        this.setTimestampNow();
    }

    public Record(String device, String value) {
        this.device = device;
        this.value = value;
        this.setTimestampNow();
    }

    public Record(String device, String timestamp, String value) {
        this.device = device;
        this.timestamp = timestamp;
        this.value = value;
    }

    public Record() {

    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setTimestampNow() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        this.setTimestamp(dateFormat.format(new Date()));
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
