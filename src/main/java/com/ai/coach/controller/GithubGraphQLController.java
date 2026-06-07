package com.ai.coach.controller;

import com.ai.coach.domain.dto.GithubCommit;
import com.ai.coach.domain.dto.GithubIssue;
import com.ai.coach.domain.entity.FatigueLevel;
import com.ai.coach.domain.entity.InjuryRisk;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.service.GithubService;
import com.ai.coach.service.MatchAnalysisService;
import com.ai.coach.service.SeasonPlanService;
import com.ai.coach.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GithubGraphQLController {

    private final GithubService githubService;
    private final MatchAnalysisService matchAnalysisService;
    private final SeasonPlanService seasonPlanService;
    private final TeamService teamService;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public GithubCommit exportMatchAnalysisToGithub(
            @Argument Long analysisId,
            @Argument String repoOwner,
            @Argument String repoName,
            @Argument String filePath,
            @Argument String commitMessage
    ) {
        log.info("GraphQL mutation: exportMatchAnalysisToGithub id={}, owner={}, repo={}",
                analysisId, repoOwner, repoName);

        MatchAnalysis analysis = matchAnalysisService.getById(analysisId);
        String markdown = matchAnalysisService.formatAsMarkdown(analysis);

        String msg = commitMessage != null && !commitMessage.isBlank()
                ? commitMessage
                : "Exported tactical match analysis (ID: " + analysisId + ")";

        return githubService.commitFile(repoOwner, repoName, filePath, markdown, msg);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public GithubIssue createGithubIssueFromWorkload(
            @Argument Long teamId,
            @Argument String repoOwner,
            @Argument String repoName
    ) {
        log.info("GraphQL mutation: createGithubIssueFromWorkload team={}, owner={}, repo={}",
                teamId, repoOwner, repoName);

        Team team = teamService.getTeam(teamId);
        var snapshots = seasonPlanService.getPlayerWorkloadSnapshots(teamId);

        StringBuilder sb = new StringBuilder();
        sb.append("# Player Workload & Injury Risk Alert for ").append(team.getName()).append("\n\n");

        long alertsCount = snapshots.stream()
                .filter(s -> s.getFatigueLevel() == FatigueLevel.TIRED
                        || s.getFatigueLevel() == FatigueLevel.EXHAUSTED
                        || s.getInjuryRisk() == InjuryRisk.MEDIUM
                        || s.getInjuryRisk() == InjuryRisk.HIGH)
                .count();

        if (alertsCount == 0) {
            sb.append("✅ **All players are currently in optimal condition.**\n\n");
            sb.append("No high fatigue levels or significant injury risks were detected in the squad over the last 28 days.\n");
        } else {
            sb.append("⚠️ **Attention required:** ").append(alertsCount).append(" player(s) flagged with elevated fatigue or injury risk.\n\n");
            sb.append("| Player Name | Position | Matches (Last 28d) | Minutes (Last 28d) | Fatigue Level | Injury Risk |\n");
            sb.append("|---|---|---|---|---|---|\n");
            for (var s : snapshots) {
                if (s.getFatigueLevel() == FatigueLevel.TIRED
                        || s.getFatigueLevel() == FatigueLevel.EXHAUSTED
                        || s.getInjuryRisk() == InjuryRisk.MEDIUM
                        || s.getInjuryRisk() == InjuryRisk.HIGH) {
                    sb.append("| ").append(s.getPlayer().getName())
                            .append(" | ").append(s.getPlayer().getPosition())
                            .append(" | ").append(s.getMatchesLast28Days())
                            .append(" | ").append(s.getMinutesLast28Days())
                            .append(" | **").append(s.getFatigueLevel()).append("**")
                            .append(" | **").append(s.getInjuryRisk()).append("** |\n");
                }
            }
            sb.append("\n**Recommendations:**\n");
            sb.append("- Consider resting flagged players in upcoming fixtures.\n");
            sb.append("- Adjust training intensity downward for players showing TIRED or EXHAUSTED fatigue levels.\n");
        }

        String title = "Squad Workload Alert - " + team.getName();
        return githubService.createIssue(repoOwner, repoName, title, sb.toString());
    }
}
