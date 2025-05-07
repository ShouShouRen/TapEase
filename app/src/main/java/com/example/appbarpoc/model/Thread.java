package com.example.appbarpoc.model;

import java.util.List;

public class Thread {
    private String id;
    private String authorId;
    private String authorName;
    private String content;
    private long timestamp;
    private List<Reply> replies; // 可選，載入時用
    private int likeCount;
    private List<String> likedUserIds;

    public Thread() {
    }

    public Thread(String id, String authorId, String authorName, String content, long timestamp) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Reply> getReplies() {
        return replies;
    }

    public void setReplies(List<Reply> replies) {
        this.replies = replies;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public List<String> getLikedUserIds() {
        return likedUserIds;
    }

    public void setLikedUserIds(List<String> likedUserIds) {
        this.likedUserIds = likedUserIds;
    }
}
