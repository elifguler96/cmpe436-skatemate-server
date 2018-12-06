package db_models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    @SerializedName("username")
    public String username;

    @SerializedName("password")
    public String password;

    @SerializedName("conversations")
    public List<Conversation> conversations;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        conversations = new ArrayList<>();
    }
}
