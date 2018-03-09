package gruBot.telegram.objects;

import java.io.Serializable;

public class User implements Serializable {

    private long id;
    private String username;
    private String fullname;
    private String phoneNumber;
    private String desc;
    private String imgUrl;

    public User(long id, String username, String fullname, String phoneNumber, String desc, String imgUrl) {
        this.id = id;
        this.username = username;
        this.fullname = fullname;
        this.phoneNumber = phoneNumber;
        this.desc = desc;
        this.imgUrl = imgUrl;
    }

    public String getAvatar() {
        return imgUrl;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return fullname;
    }

    public String getUsername() {
        return username;
    }

    public String getFullname() {
        return fullname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDesc() {
        return desc;
    }
}
