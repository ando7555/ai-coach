package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMatchStat {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "STAT_FOR", direction = Relationship.Direction.OUTGOING)
    private Player player;

    @Relationship(type = "STAT_IN", direction = Relationship.Direction.OUTGOING)
    private Match match;

    private int minutesPlayed;
    private int goals;
    private int assists;
    private int yellowCards;
    private boolean redCard;
    private Double rating;
}
