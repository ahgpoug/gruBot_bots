package gruBot.telegram.objects;

import java.io.Serializable;
import java.util.Map;

public class Group implements Serializable {
    private long id;
    private String name;
    private Map<String, Boolean> users;
    private String imgURL;

    public Group(long id, String name, Map<String, Boolean> users, String imgURL) {
        this.id = id;
        this.name = name;
        this.users = users;
        this.imgURL = imgURL;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImgURL() {
        return imgURL;
    }

    public Map<String, Boolean> getUsers() {
        return users;
    }
}
