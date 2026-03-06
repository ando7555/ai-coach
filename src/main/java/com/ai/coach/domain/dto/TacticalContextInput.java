package com.ai.coach.domain.dto;

public class TacticalContextInput {

    private Long matchId;
    private String focusArea;
    private String style;
    private String riskLevel;

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public String getFocusArea() { return focusArea; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}
