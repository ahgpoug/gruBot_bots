package gruBot.telegram.objects;

import java.io.Serializable;
import java.util.Map;

public class Group implements Serializable {
    private long id;
    private String name;
    private Map<String, Boolean> users;

    public Group(long id, String name, Map<String, Boolean> users) {
        this.id = id;
        this.name = name;
        this.users = users;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Boolean> getUsers() {
        return users;
    }
}
