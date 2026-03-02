package com.example.dto;

public class LearningOverviewDTO {
    private Integer totalAnswers;
    private Integer correctAnswers;
    private Double accuracy;
    private Integer recommendedCount;

    public Integer getTotalAnswers() { return totalAnswers; }
    public void setTotalAnswers(Integer totalAnswers) { this.totalAnswers = totalAnswers; }
    public Integer getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(Integer correctAnswers) { this.correctAnswers = correctAnswers; }
    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    public Integer getRecommendedCount() { return recommendedCount; }
    public void setRecommendedCount(Integer recommendedCount) { this.recommendedCount = recommendedCount; }
}
