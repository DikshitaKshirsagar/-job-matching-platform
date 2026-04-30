package com.jobmatch.backend.dto;

import java.util.List;

public class MatchResponse {

    private double matchScore;
    private List<String> skillsMatched;
    private List<String> skillsMissing;

    public MatchResponse() {
    }

    public MatchResponse(double matchScore, List<String> skillsMatched, List<String> skillsMissing) {
        this.matchScore = matchScore;
        this.skillsMatched = skillsMatched;
        this.skillsMissing = skillsMissing;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getSkillsMatched() {
        return skillsMatched;
    }

    public void setSkillsMatched(List<String> skillsMatched) {
        this.skillsMatched = skillsMatched;
    }

    public List<String> getSkillsMissing() {
        return skillsMissing;
    }

    public void setSkillsMissing(List<String> skillsMissing) {
        this.skillsMissing = skillsMissing;
    }
}
