package com.example.entity;

public class RecommendationLog {
    private Integer id;
    private Integer userId;
    private Integer exerciseId;
    private Double cfScore;
    private Double kgScore;
    private Double hybridScore;
    private String reason;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getExerciseId() { return exerciseId; }
    public void setExerciseId(Integer exerciseId) { this.exerciseId = exerciseId; }
    public Double getCfScore() { return cfScore; }
    public void setCfScore(Double cfScore) { this.cfScore = cfScore; }
    public Double getKgScore() { return kgScore; }
    public void setKgScore(Double kgScore) { this.kgScore = kgScore; }
    public Double getHybridScore() { return hybridScore; }
    public void setHybridScore(Double hybridScore) { this.hybridScore = hybridScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
