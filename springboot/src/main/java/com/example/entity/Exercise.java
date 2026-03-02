package com.example.entity;

public class Exercise {
    private Integer id;
    private String title;
    private String content;
    private String difficulty;
    private Integer knowledgePointId;
    private String knowledgePointName;
    private String fileUrl;
    private String subject;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Integer knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}
