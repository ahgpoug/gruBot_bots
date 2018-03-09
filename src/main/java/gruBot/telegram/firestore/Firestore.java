package gruBot.telegram.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import gruBot.telegram.bot.GruBot;
import gruBot.telegram.objects.Group;
import gruBot.telegram.objects.User;
import gruBot.telegram.utils.Utils;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Firestore {
    private com.google.cloud.firestore.Firestore db;

    public Firestore() {
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId("grubot-7d217")
                        .build();
        this.db = firestoreOptions.getService();
    }

    public boolean checkGroupExists(long chatId) throws ExecutionException, InterruptedException {
        Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
        ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();

        return !documents.isEmpty();
    }

    public void checkUserExistsInGroup(Update update, GruBot bot) throws ExecutionException, InterruptedException {
        long chatId = update.getMessage().getChatId();

        Query usersQuery = db.collection("users").whereEqualTo("groups." + chatId, true);
        ApiFuture<QuerySnapshot> snapshotApiFuture = usersQuery.get();
        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();

        if (documents.isEmpty())
            createNewUser(update, bot);
    }

    private void createNewUser(Update update, GruBot bot) {
        Message message = update.getMessage();

        long userId = message.getFrom().getId();
        String username = message.getFrom().getUserName();
        String fullname = message.getFrom().getFirstName() + " " + message.getFrom().getLastName();
        String phoneNumber = "Unknown";

        GetUserProfilePhotos getUserProfilePhotosRequest = new GetUserProfilePhotos()
                .setUserId((int) userId)
                .setOffset(0);
        String imgUrl = Utils.createUrlForTelegramFile(getUserProfilePhotoPath(getUserProfilePhotosRequest, bot));

        User user = new User(userId, username, fullname, phoneNumber, "", imgUrl);

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("fullname", user.getFullname());
        userMap.put("phoneNumber", user.getPhoneNumber());
        userMap.put("imgUrl", user.getAvatar());

        HashMap<String, Boolean> groups = new HashMap<>();
        groups.put(String.valueOf(message.getChatId()), true);

        userMap.put("groups", groups);

        db.collection("users").add(userMap);
    }

    private String getUserProfilePhotoPath(GetUserProfilePhotos request, GruBot bot) {
        UserProfilePhotos photos;

        try {
            photos = bot.getUserPhotos(request);
            PhotoSize photo = photos.getPhotos().get(0)
                    .stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null);

            if (photo != null) {
                if (photo.hasFilePath()) {
                    return photo.getFilePath();
                } else {
                    GetFile getFileMethod = new GetFile();
                    getFileMethod.setFileId(photo.getFileId());
                    File file = bot.getFileByRequest(getFileMethod);
                    return file.getFilePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void createNewGroup(Update update) {
        Message message = update.getMessage();
        long chatId = message.getChatId();
        String chatName = message.getChat().getTitle();

        String imgFileId = "";//message.getChat().getPhoto().getBigFileId();
        String imgUrl = "";//Utils.createUrlForTelegramFile(imgFileId);

        Group group = new Group(chatId, chatName, (new HashMap<>()), imgUrl);

        HashMap<String, Object> groupMap = new HashMap<>();
        groupMap.put("chatId", group.getId());
        groupMap.put("imgUrl", group.getImgURL());
        groupMap.put("name", group.getName());
        groupMap.put("users", group.getUsers());

        db.collection("groups").add(groupMap);
    }

    public void createNewAnnouncement(long groupId) {

    }
}
