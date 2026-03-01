package com.example.dto;

import com.example.entity.Exercise;

public class RecommendationItem {
    private Exercise exercise;
    private Double cfScore;
    private Double kgScore;
    private Double hybridScore;
    private String reason;

    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }
    public Double getCfScore() { return cfScore; }
    public void setCfScore(Double cfScore) { this.cfScore = cfScore; }
    public Double getKgScore() { return kgScore; }
    public void setKgScore(Double kgScore) { this.kgScore = kgScore; }
    public Double getHybridScore() { return hybridScore; }
    public void setHybridScore(Double hybridScore) { this.hybridScore = hybridScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
