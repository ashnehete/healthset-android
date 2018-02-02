package in.ashnehete.healthset.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String username;
    public String email;
    public String name;
    public int age;
    public double weight;
    public double height;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email, String name, int age, double weight, double height) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.age = age;
        this.weight = weight;
        this.height = height;
    }
}