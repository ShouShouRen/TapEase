package com.example.appbarpoc.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appbarpoc.R;
import com.example.appbarpoc.model.Thread;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ThreadAdapter extends RecyclerView.Adapter<ThreadAdapter.ThreadViewHolder> {
    public interface OnReplyPostListener {
        void onReplyPost(Thread thread, String replyContent, EditText replyEditText);
    }

    public interface OnLikeClickListener {
        void onLikeClick(Thread thread, boolean isLiked);
    }

    private List<Thread> threadList;
    private OnReplyPostListener replyPostListener;
    private OnLikeClickListener likeClickListener;
    private String currentUserId;

    public ThreadAdapter(List<Thread> threadList, OnReplyPostListener replyPostListener,
            OnLikeClickListener likeClickListener, String currentUserId) {
        this.threadList = threadList;
        this.replyPostListener = replyPostListener;
        this.likeClickListener = likeClickListener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thread, parent, false);
        return new ThreadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
        Thread thread = threadList.get(position);
        holder.textAuthor.setText(thread.getAuthorName());
        holder.textContent.setText(thread.getContent());
        holder.textTimestamp.setText(formatTime(thread.getTimestamp()));
        // Like 狀態
        int likeCount = thread.getLikeCount();
        holder.textLikeCount.setText(String.valueOf(likeCount));
        final boolean isLiked = thread.getLikedUserIds() != null && currentUserId != null
                && thread.getLikedUserIds().contains(currentUserId);
        holder.buttonLike.setImageResource(isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
        holder.buttonLike.setOnClickListener(v -> {
            if (likeClickListener != null) {
                likeClickListener.onLikeClick(thread, isLiked);
            }
        });
        // 回覆串
        ReplyAdapter replyAdapter = new ReplyAdapter(thread.getReplies());
        holder.recyclerViewReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerViewReplies.setAdapter(replyAdapter);
        // 發佈回覆
        holder.buttonPostReply.setOnClickListener(v -> {
            String replyContent = holder.editTextReply.getText().toString().trim();
            if (!TextUtils.isEmpty(replyContent) && replyPostListener != null) {
                replyPostListener.onReplyPost(thread, replyContent, holder.editTextReply);
            }
        });
    }

    @Override
    public int getItemCount() {
        return threadList == null ? 0 : threadList.size();
    }

    public void setThreadList(List<Thread> threadList) {
        this.threadList = threadList;
        notifyDataSetChanged();
    }

    static class ThreadViewHolder extends RecyclerView.ViewHolder {
        TextView textAuthor, textContent, textTimestamp, textLikeCount;
        RecyclerView recyclerViewReplies;
        EditText editTextReply;
        Button buttonPostReply;
        android.widget.ImageButton buttonLike;

        ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            textContent = itemView.findViewById(R.id.textContent);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            recyclerViewReplies = itemView.findViewById(R.id.recyclerViewReplies);
            editTextReply = itemView.findViewById(R.id.editTextReply);
            buttonPostReply = itemView.findViewById(R.id.buttonPostReply);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            textLikeCount = itemView.findViewById(R.id.textLikeCount);
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
