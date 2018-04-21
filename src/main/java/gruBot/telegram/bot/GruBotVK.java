package gruBot.telegram.bot;

import com.petersamokhin.bots.sdk.clients.User;
import com.petersamokhin.bots.sdk.utils.vkapi.API;
import gruBot.telegram.firestore.Firestore;
import gruBot.telegram.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class GruBotVK {
    private Firestore firestore;
    private User user;

    public GruBotVK(Firestore firestore, User user) {
        this.user = user;
        this.firestore = firestore;
        this.firestore.setVkBot(this);
        Logger.log("VK Bot Started", Logger.Type.INFO, Logger.Source.TELEGRAM);
    }

    public void start() {
        user.onMessage(message -> {
            if (message.isMessageFromChat()) {
                try {
                    firestore.checkGroupExists(message.getChatIdLong());
                } catch (Exception e) {
                    Logger.log(e.getMessage(), Logger.Type.ERROR, Logger.Source.VK);
                }
                //onGroupMessage(message);
            }
        });

        user.api().call("messages.getDialogs", "{}", o -> {
            JSONArray response = ((JSONObject) o).getJSONArray("items");
            parseChatUsers(response);
        });

        user.onChatCreated((s, integer, integer2) -> {

        });
    }

    private void parseChatUsers(JSONArray response){
        /*for (int i = 0; i < response.length(); i++) {
            JSONObject message = response.getJSONObject(i).getJSONObject("message");
            if (message.has("chat_id")) {
                HashMap<String, Boolean> users = new HashMap();
                try {
                    JSONArray usersJson = new JSONObject(getUsersOfChat(String.valueOf(message.getInt("chat_id")))).getJSONArray("response");
                    for (int k = 0; k < usersJson.length(); k++) {
                        users.put(usersJson.get(k).toString(), true);
                    }

                    Group group = new Group(message.getLong("id"),
                            message.getString("title"),
                            users, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQRclj05KyPN2qgZEZuSRMoI5qJ_GCeXnGKJggVQsH7biQGza1Lyw");

                    addGroupInDataBaseIfNeed(group);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }*/
    }

    private String getUsersOfChat(String chat_id) {
        return new API(user).callSync("messages.getChatUsers", "{chat_id:" + chat_id + "}");
    }
}
