package gruBot.telegram.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import gruBot.telegram.bot.GruBot;
import gruBot.telegram.bot.GruBotConfig;
import gruBot.telegram.bot.GruBotPatterns;
import gruBot.telegram.logger.Logger;
import gruBot.telegram.objects.Group;
import gruBot.telegram.objects.User;
import gruBot.telegram.utils.Utils;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.api.objects.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Firestore {
    private com.google.cloud.firestore.Firestore db;

    public Firestore() {
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(GruBotConfig.PROJECT_ID)
                        .build();
        this.db = firestoreOptions.getService();
    }

    public boolean checkGroupExists(long chatId) throws ExecutionException, InterruptedException {
        Logger.log("Checking group exists in database...", Logger.INFO);
        Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
        ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();

        Logger.log("Group exists - " + !documents.isEmpty(), Logger.INFO);
        return !documents.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public void checkUserExistsInGroup(Update update, GruBot bot) throws ExecutionException, InterruptedException, NullPointerException {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        Query usersQuery = db.collection("users").whereEqualTo("userId", userId);
        ApiFuture<QuerySnapshot> snapshotApiFuture = usersQuery.get();
        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();

        if (documents.isEmpty()) {
            Logger.log("Creating new user...", Logger.INFO);
            createNewUser(update, bot);
        } else {
            Logger.log("Checking user group relations...", Logger.INFO);
            Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
            snapshotApiFuture = groupsQuery.get();
            documents = snapshotApiFuture.get().getDocuments();
            for (DocumentSnapshot document : documents) {
                Map<String, Boolean> users = (Map<String, Boolean>) document.get("users");
                Boolean value = users.get(String.valueOf(userId));

                if (value == null || (value != null && value == false)) {
                    Logger.log("Adding user to the group", Logger.INFO);
                    addUserToGroup(usersQuery, groupsQuery, chatId, userId);
                } else {
                    Logger.log("User is already in the group", Logger.INFO);
                }
            }
        }
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
        userMap.put("desc", user.getDesc());

        HashMap<String, Boolean> groups = new HashMap<>();
        groups.put(String.valueOf(message.getChatId()), true);

        userMap.put("groups", groups);

        db.collection("users").add(userMap);
        Logger.log(String.format("User %s created", username), Logger.INFO);
    }

    private void addUserToGroup(Query usersQuery, Query groupsQuery, long chatId, long userId) throws ExecutionException, InterruptedException, NullPointerException {
        ApiFuture<QuerySnapshot> snapshotApiFuture = usersQuery.get();
        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
        System.out.println(documents.size());
        for (DocumentSnapshot document : documents) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("groups." + chatId, true);

            ApiFuture<WriteResult> future = document.getReference().update(updates);
            Logger.log("User groups updated", Logger.INFO);
        }


        snapshotApiFuture = groupsQuery.get();
        documents = snapshotApiFuture.get().getDocuments();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("users." + userId, true);

            document.getReference().update(updates);
            Logger.log("Group users updated", Logger.INFO);
        }
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
            Logger.log(e.getMessage(), Logger.ERROR);
        }
        return null;
    }

    public void createNewGroup(Update update) {
        Logger.log("Creating new group...", Logger.INFO);
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
        Logger.log("Group created...", Logger.INFO);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object>  createNewAnnouncement(Update update) {
        Logger.log("Creating new announcement...", Logger.INFO);
        Message message = update.getMessage();
        long chatId = message.getChatId();

        Logger.log("Matching title to regexp...", Logger.INFO);
        Matcher m = Pattern.compile(GruBotPatterns.announcementTitle, Pattern.MULTILINE).matcher(message.getText());
        String announcementTitle = "";
        if(m.find()) {
            announcementTitle = m.group(0).replace("!", "");
            Logger.log("Title match found", Logger.INFO);
        } else {
            Logger.log("Title match not found", Logger.WARNING);
        }

        Logger.log("Matching text to regexp...", Logger.INFO);
        m = Pattern.compile(GruBotPatterns.announcementText, Pattern.MULTILINE).matcher(message.getText());
        String announcementText = "";
        if(m.find()) {
            announcementText = m.group(0);
            Logger.log("Text match found", Logger.INFO);
        } else {
            Logger.log("Text match not found", Logger.WARNING);
        }

        Logger.log("Matching finished", Logger.INFO);
        Logger.log("Getting group users...", Logger.INFO);

        Map<String, Boolean> groupUsers = new HashMap<>();
        try {
            Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
            ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

            List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
            for (DocumentSnapshot document : documents) {
                groupUsers = (Map<String, Boolean>) document.get("users");
            }

        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.ERROR);
        }

        Logger.log("Creating announcement...", Logger.INFO);
        HashMap<String, Object> announcement = new HashMap<>();
        announcement.put("group", message.getChatId());
        announcement.put("groupName", message.getChat().getTitle());
        announcement.put("author", message.getFrom().getId());
        announcement.put("authorName", message.getFrom().getFirstName() + " " + message.getFrom().getLastName());
        announcement.put("desc", announcementTitle);
        announcement.put("date", new Date());
        announcement.put("text", announcementText);
        HashMap<String, String> users = new HashMap<>();
        for (Map.Entry<String, Boolean> user : groupUsers.entrySet())
            users.put(user.getKey(), "new");
        announcement.put("users", users);

        db.collection("announcements").add(announcement);
        Logger.log("Announcement created", Logger.INFO);
        return announcement;
    }
}
