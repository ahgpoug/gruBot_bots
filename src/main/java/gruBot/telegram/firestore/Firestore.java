package gruBot.telegram.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.ExecutionError;

import java.util.List;
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

    public void createNewGroup()

    public void createNewAnnouncement(long groupId) {

    }
}
