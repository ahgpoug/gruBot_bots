package gruBot.telegram.objects;

import java.util.Date;
import java.util.Map;

public class ActionAnnouncement extends Action {
    private String text;

    public ActionAnnouncement(String id, String group, String groupName, String author, String authorName, String desc, Date date, String text, Map<String, String> users) {
        super(id, group, groupName, author, authorName, desc, date, users);

        this.text = text;
    }

    public String getText() {
        return text;
    }
}
