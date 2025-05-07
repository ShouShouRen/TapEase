package com.example.appbarpoc.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appbarpoc.R;
import com.example.appbarpoc.model.Reply;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    private List<Reply> replyList;

    public ReplyAdapter(List<Reply> replyList) {
        this.replyList = replyList;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replyList.get(position);
        holder.textReplyAuthor.setText(reply.getAuthorName());
        holder.textReplyContent.setText(reply.getContent());
        holder.textReplyTimestamp.setText(formatTime(reply.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return replyList == null ? 0 : replyList.size();
    }

    public void setReplyList(List<Reply> replyList) {
        this.replyList = replyList;
        notifyDataSetChanged();
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView textReplyAuthor, textReplyContent, textReplyTimestamp;
        ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            textReplyAuthor = itemView.findViewById(R.id.textReplyAuthor);
            textReplyContent = itemView.findViewById(R.id.textReplyContent);
            textReplyTimestamp = itemView.findViewById(R.id.textReplyTimestamp);
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
