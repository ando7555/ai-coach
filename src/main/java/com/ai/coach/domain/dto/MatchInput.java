// MatchInput.java
package com.ai.coach.domain.dto;

public class MatchInput {

    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeGoals;
    private Integer awayGoals;
    private String date; // ISO-8601

    public Long getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(Long homeTeamId) { this.homeTeamId = homeTeamId; }

    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    public Integer getHomeGoals() { return homeGoals; }
    public void setHomeGoals(Integer homeGoals) { this.homeGoals = homeGoals; }

    public Integer getAwayGoals() { return awayGoals; }
    public void setAwayGoals(Integer awayGoals) { this.awayGoals = awayGoals; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
