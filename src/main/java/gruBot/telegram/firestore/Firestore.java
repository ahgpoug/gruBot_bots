package gruBot.telegram.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import gruBot.telegram.bot.GruBotTelegram;
import gruBot.telegram.bot.GruBotConfig;
import gruBot.telegram.bot.GruBotPatterns;
import gruBot.telegram.bot.GruBotVK;
import gruBot.telegram.logger.Logger;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Firestore {
    private com.google.cloud.firestore.Firestore db;
    private GruBotTelegram telegramBot;
    private GruBotVK vkBot;

    public Firestore() {
        Logger.log("Initializing Firestore...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(GruBotConfig.PROJECT_ID)
                        .build();
        this.db = firestoreOptions.getService();
        setPollUpdatesListener();
    }

    public void setTelegramBot(GruBotTelegram telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void setVkBot(GruBotVK vkBot) {
        this.vkBot = vkBot;
    }

    private void setPollUpdatesListener() {
        Logger.log("Setting polls update listener...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Query pollsQuery = db.collection("votes");
        pollsQuery.addSnapshotListener((snapshots, error) -> {
            if (error == null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            break;
                        case MODIFIED:
                            DocumentSnapshot document = dc.getDocument();
                            EditMessageText editMessageText = getMessageText(document)
                                    .setChatId(Long.valueOf(document.get("group").toString()))
                                    .setMessageId(Integer.valueOf(document.get("messageId").toString()));
                            telegramBot.updatePoll(editMessageText);
                            break;
                        case REMOVED:
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    public boolean checkGroupExists(long chatId) throws ExecutionException, InterruptedException {
        Logger.log("Checking group exists in database...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
        ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();

        Logger.log("Group exists - " + !documents.isEmpty(), Logger.Type.INFO, Logger.Source.FIRESTORE);
        return !documents.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public void checkUserExistsInGroup(Update update) throws ExecutionException, InterruptedException, NullPointerException {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        Logger.log("Checking user group relations...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
        ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();
        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
        for (DocumentSnapshot document : documents) {
            Map<String, Boolean> users = (Map<String, Boolean>) document.get("users");
            Boolean value = users.get(String.valueOf(userId));

            if (value == null || (value != null && value == false)) {
                Logger.log("Adding user to the group", Logger.Type.INFO, Logger.Source.FIRESTORE);
                addUserToGroup(groupsQuery, userId);
            } else {
                Logger.log("User is already in the group", Logger.Type.INFO, Logger.Source.FIRESTORE);
            }
        }
    }

    private void addUserToGroup(Query groupsQuery, long userId) throws ExecutionException, InterruptedException, NullPointerException {
        ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();
        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("users." + userId, true);

            document.getReference().update(updates);
            Logger.log("Group users updated", Logger.Type.INFO, Logger.Source.FIRESTORE);
        }
    }

    public void createNewGroup(Update update) {
        Logger.log("Creating new group...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Message message = update.getMessage();
        long chatId = message.getChatId();
        String chatName = message.getChat().getTitle();

        HashMap<String, Object> groupMap = new HashMap<>();
        groupMap.put("chatId", chatId);
        groupMap.put("name", chatName);
        groupMap.put("users", new HashMap<>());

        db.collection("groups").add(groupMap);
        Logger.log("Group created...", Logger.Type.INFO, Logger.Source.FIRESTORE);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object> createNewAnnouncement(Update update) {
        Logger.log("Creating new announcement...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Message message = update.getMessage();
        long chatId = message.getChatId();

        Logger.log("Matching title to regexp...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Matcher m = Pattern.compile(GruBotPatterns.announcementTitle, Pattern.MULTILINE).matcher(message.getText());
        String announcementTitle = "";
        if (m.find()) {
            announcementTitle = m.group(0).replace("!", "");
            Logger.log("Title match is found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        } else {
            Logger.log("Title match is not found", Logger.Type.WARNING, Logger.Source.FIRESTORE);
        }

        Logger.log("Matching text to regexp...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        m = Pattern.compile(GruBotPatterns.announcementText, Pattern.MULTILINE).matcher(message.getText());
        String announcementText = "";
        if (m.find()) {
            announcementText = m.group(0);
            Logger.log("Text match is found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        } else {
            Logger.log("Text match is not found", Logger.Type.WARNING, Logger.Source.FIRESTORE);
        }

        Logger.log("Matching finished", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Logger.log("Getting group users...", Logger.Type.INFO, Logger.Source.FIRESTORE);

        Map<String, Boolean> groupUsers = new HashMap<>();
        try {
            Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
            ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

            List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
            for (DocumentSnapshot document : documents) {
                groupUsers = (Map<String, Boolean>) document.get("users");
            }

        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.Type.ERROR, Logger.Source.FIRESTORE);
        }

        Logger.log("Creating announcement...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        HashMap<String, Object> announcement = new HashMap<>();
        announcement.put("group", message.getChatId());
        announcement.put("groupName", message.getChat().getTitle());
        announcement.put("messageId", -1);
        announcement.put("author", message.getFrom().getId());
        announcement.put("authorName", message.getFrom().getFirstName() + " " + message.getFrom().getLastName());
        announcement.put("desc", announcementTitle);
        announcement.put("date", new Date());
        announcement.put("type", "TELEGRAM");
        announcement.put("text", announcementText);
        HashMap<String, String> users = new HashMap<>();
        for (Map.Entry<String, Boolean> user : groupUsers.entrySet())
            users.put(user.getKey(), "new");
        announcement.put("users", users);

        ApiFuture<DocumentReference> reference = db.collection("announcements").add(announcement);
        announcement.put("reference", reference);
        Logger.log("Announcement created", Logger.Type.INFO, Logger.Source.FIRESTORE);
        return announcement;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object> createNewArticle(Update update) {
        Logger.log("Creating new article...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Message message = update.getMessage();
        long chatId = message.getChatId();

        Logger.log("Matching title to regexp...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Matcher m = Pattern.compile(GruBotPatterns.articleTitle, Pattern.MULTILINE).matcher(message.getText());
        String announcementTitle = "";
        if (m.find()) {
            announcementTitle = m.group(0).replace("*", "");
            Logger.log("Title match is found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        } else {
            Logger.log("Title match is not found", Logger.Type.WARNING, Logger.Source.FIRESTORE);
        }

        Logger.log("Matching text to regexp...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        m = Pattern.compile(GruBotPatterns.articleText, Pattern.MULTILINE).matcher(message.getText());
        String announcementText = "";
        if (m.find()) {
            announcementText = m.group(0);
            Logger.log("Text match is found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        } else {
            Logger.log("Text match is not found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        }

        Logger.log("Matching finished", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Logger.log("Getting group users...", Logger.Type.INFO, Logger.Source.FIRESTORE);

        Map<String, Boolean> groupUsers = new HashMap<>();
        try {
            Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
            ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

            List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
            for (DocumentSnapshot document : documents) {
                groupUsers = (Map<String, Boolean>) document.get("users");
            }

        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.Type.ERROR, Logger.Source.FIRESTORE);
        }

        Logger.log("Creating article...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        HashMap<String, Object> article = new HashMap<>();
        article.put("group", message.getChatId());
        article.put("groupName", message.getChat().getTitle());
        article.put("messageId", -1);
        article.put("author", message.getFrom().getId());
        article.put("authorName", message.getFrom().getFirstName() + " " + message.getFrom().getLastName());
        article.put("desc", announcementTitle);
        article.put("date", new Date());
        article.put("type", "TELEGRAM");
        article.put("text", announcementText);
        HashMap<String, String> users = new HashMap<>();
        for (Map.Entry<String, Boolean> user : groupUsers.entrySet())
            users.put(user.getKey(), "new");
        article.put("users", users);

        ApiFuture<DocumentReference> reference = db.collection("articles").add(article);
        article.put("reference", reference);
        Logger.log("Article created", Logger.Type.INFO, Logger.Source.FIRESTORE);
        return article;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object> createNewPoll(Update update) {
        Logger.log("Creating new poll...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Message message = update.getMessage();
        long chatId = message.getChatId();

        Logger.log("Matching title to regexp...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Matcher m = Pattern.compile(GruBotPatterns.voteTitle, Pattern.MULTILINE).matcher(message.getText());
        String voteTitle = "";
        if(m.find()) {
            voteTitle = m.group(0).replace("?", "").replaceAll("\r", "");
            Logger.log("Title match is found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        } else {
            Logger.log("Title match is not found", Logger.Type.WARNING, Logger.Source.FIRESTORE);
        }

        Logger.log("Matching text to regexp...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        m = Pattern.compile(GruBotPatterns.voteText, Pattern.MULTILINE).matcher(message.getText());

        HashMap<String, String> voteOptions = new HashMap<>();
        int i = 0;
        while (m.find()) {
            voteOptions.put(String.valueOf(i + 1), m.group().replaceFirst(GruBotPatterns.voteOptionTextOnly, "").replaceAll("\r", "").replaceAll("\n", ""));
            i++;
            Logger.log("Text match is found", Logger.Type.INFO, Logger.Source.FIRESTORE);
        }
        if (i == 0)
            Logger.log("Text match is not found", Logger.Type.WARNING, Logger.Source.FIRESTORE);

        Logger.log("Matching finished", Logger.Type.INFO, Logger.Source.FIRESTORE);
        Logger.log("Getting group users...", Logger.Type.INFO, Logger.Source.FIRESTORE);

        Map<String, Boolean> groupUsers = new HashMap<>();
        try {
            Query groupsQuery = db.collection("groups").whereEqualTo("chatId", chatId);
            ApiFuture<QuerySnapshot> snapshotApiFuture = groupsQuery.get();

            List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
            for (DocumentSnapshot document : documents) {
                groupUsers = (Map<String, Boolean>) document.get("users");
            }

        } catch (Exception e) {
            Logger.log(e.getMessage(), Logger.Type.ERROR, Logger.Source.FIRESTORE);
        }

        Logger.log("Creating poll...", Logger.Type.INFO, Logger.Source.FIRESTORE);
        HashMap<String, Object> vote = new HashMap<>();
        vote.put("group", message.getChatId());
        vote.put("messageId", -1);
        vote.put("groupName", message.getChat().getTitle());
        vote.put("author", message.getFrom().getId());
        vote.put("authorName", message.getFrom().getFirstName() + " " + message.getFrom().getLastName());
        vote.put("desc", voteTitle);
        vote.put("date", new Date());
        vote.put("type", "TELEGRAM");
        vote.put("voteOptions", voteOptions);
        HashMap<String, String> users = new HashMap<>();
        for (Map.Entry<String, Boolean> user : groupUsers.entrySet())
            users.put(user.getKey(), "new");
        vote.put("users", users);

        ApiFuture<DocumentReference> reference = db.collection("votes").add(vote);
        vote.put("reference", reference);
        Logger.log("Poll created", Logger.Type.INFO, Logger.Source.FIRESTORE);
        return vote;
    }

    public void setMessageIdToAction(int messageId, ApiFuture<DocumentReference> referenceApiFuture) throws ExecutionException, InterruptedException, NullPointerException {
        DocumentReference document = referenceApiFuture.get();
        Map<String, Object> updates = new HashMap<>();
        updates.put("messageId", messageId);

        document.update(updates);
    }

    @SuppressWarnings("unchecked")
    public EditMessageText updatePollAnswer(int userId, int pollOptionNumber, int pollMessageId) throws ExecutionException, InterruptedException, NullPointerException {
        EditMessageText editMessageText = null;

        Query pollQuery = db.collection("votes").whereEqualTo("messageId", pollMessageId);
        ApiFuture<QuerySnapshot> snapshotApiFuture = pollQuery.get();
        List<QueryDocumentSnapshot> documents = snapshotApiFuture.get().getDocuments();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("users." + userId, pollOptionNumber);

            document.getReference().update(updates);
        }

        pollQuery = db.collection("votes").whereEqualTo("messageId", pollMessageId);
        snapshotApiFuture = pollQuery.get();
        documents = snapshotApiFuture.get().getDocuments();
        for (DocumentSnapshot document : documents) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("users." + userId, pollOptionNumber);

            document.getReference().update(updates);

            editMessageText = getMessageText(document);
        }


        return editMessageText;
    }

    @SuppressWarnings("unchecked")
    private EditMessageText getMessageText(DocumentSnapshot document) {
        String title = (String) document.get("desc");
        HashMap<String, String> voteOptions = (HashMap<String, String>) document.get("voteOptions");
        HashMap<String, String> users = (HashMap<String, String>) document.get("users");

        String newMessageText = title;

        HashMap<String, Integer> voteCounts = new HashMap<>();

        for (Map.Entry<String, String> entry : users.entrySet()) {
            Integer currentValue = voteCounts.get(String.valueOf(entry.getValue()));
            if (currentValue == null)
                currentValue = 0;

            voteCounts.put(String.valueOf(entry.getValue()), currentValue + 1);
        }

        StringBuilder builder = new StringBuilder(newMessageText);
        for (Map.Entry<String, String> entry : voteOptions.entrySet()) {
            Integer value = voteCounts.get(entry.getKey().trim());
            if (value == null)
                value = 0;

            builder.append("\r\n")
                    .append(entry.getKey())
                    .append(". ")
                    .append(entry.getValue())
                    .append(" [")
                    .append(value)
                    .append("]");
        }
        Integer value = voteCounts.get("new");
        if (value == null)
            value = 0;
        builder.append("\r\n\r\n")
                .append("Не проголосовало [")
                .append(value)
                .append("]");

        newMessageText = builder.toString();

        return new EditMessageText()
                .setText(newMessageText)
                .setReplyMarkup(telegramBot.getVoteKeyboard(voteOptions));
    }
}
