package com.example.appbarpoc;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appbarpoc.adapter.ThreadAdapter;
import com.example.appbarpoc.model.Thread;
import com.example.appbarpoc.model.Reply;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class MessageDetailActivity extends AppCompatActivity {
    private EditText editTextThread;
    private Button buttonPostThread;
    private RecyclerView recyclerViewThreads;
    private ThreadAdapter threadAdapter;
    private List<Thread> threadList = new ArrayList<>();
    private MessageRepository repository;
    private FirebaseUser currentUser;
    private List<ListenerRegistration> replyListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_message_detail);
        editTextThread = findViewById(R.id.editTextThread);
        buttonPostThread = findViewById(R.id.buttonPostThread);
        recyclerViewThreads = findViewById(R.id.recyclerViewThreads);
        recyclerViewThreads.setLayoutManager(new LinearLayoutManager(this));
        repository = new MessageRepository();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        threadAdapter = new ThreadAdapter(threadList,
                (thread, replyContent, replyEditText) -> postReply(thread, replyContent, replyEditText),
                (thread, isLiked) -> {
                    if (currentUser == null) {
                        Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isLiked) {
                        repository.unlikeThread(thread.getId(), currentUser.getUid());
                    } else {
                        repository.likeThread(thread.getId(), currentUser.getUid());
                    }
                },
                currentUser != null ? currentUser.getUid() : null);
        recyclerViewThreads.setAdapter(threadAdapter);
        buttonPostThread.setOnClickListener(v -> postThread());
        listenThreads();
    }

    private void postThread() {
        String content = editTextThread.getText().toString().trim();
        if (TextUtils.isEmpty(content))
            return;
        if (currentUser == null) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            return;
        }
        repository.addThread(content, currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名");
        editTextThread.setText("");
    }

    private void postReply(Thread thread, String replyContent, EditText replyEditText) {
        if (TextUtils.isEmpty(replyContent))
            return;
        if (currentUser == null) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            return;
        }
        repository.addReply(thread.getId(), replyContent, currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名");
        replyEditText.setText("");
    }

    private void listenThreads() {
        repository.listenThreads((snapshots, e) -> {
            if (e != null || snapshots == null)
                return;
            threadList.clear();
            for (ListenerRegistration reg : replyListeners)
                reg.remove();
            replyListeners.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Thread thread = new Thread();
                thread.setId(doc.getId());
                thread.setAuthorId(doc.getString("authorId"));
                thread.setAuthorName(doc.getString("authorName"));
                thread.setContent(doc.getString("content"));
                thread.setTimestamp(doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0);
                thread.setReplies(new ArrayList<>());
                Long likeCount = doc.getLong("likeCount");
                thread.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
                List<String> likedUserIds = (List<String>) doc.get("likedUserIds");
                thread.setLikedUserIds(likedUserIds != null ? likedUserIds : new ArrayList<>());
                threadList.add(thread);
                ListenerRegistration reg = repository.listenReplies(thread.getId(), (replySnapshots, replyE) -> {
                    if (replyE != null || replySnapshots == null)
                        return;
                    List<Reply> replies = new ArrayList<>();
                    for (DocumentSnapshot replyDoc : replySnapshots.getDocuments()) {
                        Reply reply = new Reply();
                        reply.setId(replyDoc.getId());
                        reply.setAuthorId(replyDoc.getString("authorId"));
                        reply.setAuthorName(replyDoc.getString("authorName"));
                        reply.setContent(replyDoc.getString("content"));
                        reply.setTimestamp(replyDoc.getLong("timestamp") != null ? replyDoc.getLong("timestamp") : 0);
                        replies.add(reply);
                    }
                    thread.setReplies(replies);
                    threadAdapter.notifyDataSetChanged();
                });
                replyListeners.add(reg);
            }
            threadAdapter.setThreadList(threadList);
        });
    }

    public void Onback(View view) {
        finish();
    }
}
