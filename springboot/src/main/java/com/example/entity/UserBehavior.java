package com.example.entity;

public class UserBehavior {
    private Integer id;
    private Integer userId;
    private Integer exerciseId;
    private String actionType;
    private Integer isCorrect;
    private Integer score;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getExerciseId() { return exerciseId; }
    public void setExerciseId(Integer exerciseId) { this.exerciseId = exerciseId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public Integer getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Integer isCorrect) { this.isCorrect = isCorrect; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
