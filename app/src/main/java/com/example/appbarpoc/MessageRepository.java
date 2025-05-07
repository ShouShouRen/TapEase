package com.example.appbarpoc;

import androidx.annotation.NonNull;
import com.example.appbarpoc.model.Reply;
import com.example.appbarpoc.model.Thread;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference threadsRef = db.collection("threads");

    public ListenerRegistration listenThreads(EventListener<QuerySnapshot> listener) {
        return threadsRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void addThread(String content, String authorId, String authorName) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("authorId", authorId);
        data.put("authorName", authorName);
        data.put("timestamp", System.currentTimeMillis());
        threadsRef.add(data);
    }

    public ListenerRegistration listenReplies(String threadId, EventListener<QuerySnapshot> listener) {
        return threadsRef.document(threadId)
                .collection("replies")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public void addReply(String threadId, String content, String authorId, String authorName) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("authorId", authorId);
        data.put("authorName", authorName);
        data.put("timestamp", System.currentTimeMillis());
        threadsRef.document(threadId).collection("replies").add(data);
    }

    public void likeThread(String threadId, String userId) {
        DocumentReference threadRef = threadsRef.document(threadId);
        threadRef.update(
                "likedUserIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                "likeCount", com.google.firebase.firestore.FieldValue.increment(1));
    }

    public void unlikeThread(String threadId, String userId) {
        DocumentReference threadRef = threadsRef.document(threadId);
        threadRef.update(
                "likedUserIds", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                "likeCount", com.google.firebase.firestore.FieldValue.increment(-1));
    }
}
