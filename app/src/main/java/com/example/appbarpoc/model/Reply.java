package com.example.appbarpoc.model;

public class Reply {
    private String id;
    private String authorId;
    private String authorName;
    private String content;
    private long timestamp;

    public Reply() {}

    public Reply(String id, String authorId, String authorName, String content, long timestamp) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getter & Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
