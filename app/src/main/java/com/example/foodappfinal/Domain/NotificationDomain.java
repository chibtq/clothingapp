package com.example.foodappfinal.Domain;

import java.util.Date;

public class NotificationDomain {
    private String notification_id;
    private String user_id;
    private String content;
    private Date created_at;
    private boolean is_read;

    // Constructor
    public NotificationDomain() {
    }

    // Getters and Setters
    public String getId() {
        return notification_id;
    }

    public void setId(String notification_id) {
        this.notification_id = notification_id;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public boolean isRead() {
        return is_read;
    }

    public void setRead(boolean is_read) {
        this.is_read = is_read;
    }
}