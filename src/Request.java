import com.google.gson.annotations.SerializedName;
import db_models.Message;
import db_models.Spot;

import java.io.Serializable;

public class Request implements Serializable {
    @SerializedName("clientUsername")
    String clientUsername;

    @SerializedName("type")
    RequestType type;

    @SerializedName("password")
    String password;

    @SerializedName("message")
    Message message;

    @SerializedName("spot")
    Spot spot;
}

enum RequestType {
    LOGIN, SENDMESSAGE, GETCONVERSATIONS, GETSPOTS, CREATESPOT
}
