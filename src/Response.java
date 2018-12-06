import com.google.gson.annotations.SerializedName;
import db_models.Conversation;
import db_models.Message;
import db_models.Spot;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    @SerializedName("code")
    int code;

    @SerializedName("conversations")
    List<Conversation> conversations;

    @SerializedName("spots")
    List<Spot> spots;

    @SerializedName("message")
    Message message;
}
