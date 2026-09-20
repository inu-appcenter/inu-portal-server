package kr.inuappcenterportal.inuportal.domain.agent.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentFuzzyMatcherTest {

    @Test
    @DisplayName("'2번 출구 버스' 질의 시 '인천대입구역 2번출구'가 최고 점수로 매칭된다")
    void matchesExit2Correctly() {
        List<String> candidates = List.of(
                "인천대입구역 1번출구",
                "인천대입구역 2번출구",
                "인천대입구역.롯데몰",
                "지식정보단지역 3번출구",
                "인천대 정문(길 건너)",
                "인천대 공과대학"
        );

        Optional<AgentFuzzyMatcher.MatchResult<String>> match = AgentFuzzyMatcher.findBestMatch(
                "2번 출구 버스",
                candidates,
                List::of,
                0.4
        );

        assertTrue(match.isPresent());
        assertEquals("인천대입구역 2번출구", match.get().item());
        assertTrue(match.get().score() >= 0.6);
    }

    @Test
    @DisplayName("'롯데몰 버스' 질의 시 '인천대입구역.롯데몰'이 정확히 매칭된다")
    void matchesLotteMallCorrectly() {
        List<String> candidates = List.of(
                "인천대입구역 1번출구",
                "인천대입구역 2번출구",
                "인천대입구역.롯데몰",
                "지식정보단지역 3번출구"
        );

        Optional<AgentFuzzyMatcher.MatchResult<String>> match = AgentFuzzyMatcher.findBestMatch(
                "롯데몰 버스 언제 와?",
                candidates,
                List::of,
                0.4
        );

        assertTrue(match.isPresent());
        assertEquals("인천대입구역.롯데몰", match.get().item());
    }

    @Test
    @DisplayName("'공대 앞' 질의 시 '인천대 공과대학'이 매칭된다")
    void matchesEngineeringCollege() {
        List<String> candidates = List.of(
                "인천대입구역 2번출구",
                "인천대 정문",
                "인천대 공과대학",
                "인천대 자연과학대학"
        );

        Optional<AgentFuzzyMatcher.MatchResult<String>> match = AgentFuzzyMatcher.findBestMatch(
                "공대 앞",
                candidates,
                List::of,
                0.3
        );

        assertTrue(match.isPresent());
        assertEquals("인천대 공과대학", match.get().item());
    }

    @Test
    @DisplayName("숫자가 불일치할 경우(예: 1번출구 vs 2번출구) 패널티가 부여되어 다른 출구로 오매칭되지 않는다")
    void digitMismatchPenalizesScore() {
        double scoreDiffExit = AgentFuzzyMatcher.calculateSimilarity("1번 출구", "인천대입구역 2번출구");
        double scoreSameExit = AgentFuzzyMatcher.calculateSimilarity("1번 출구", "인천대입구역 1번출구");

        assertTrue(scoreSameExit > scoreDiffExit);
        assertTrue(scoreSameExit >= 0.7);
        assertTrue(scoreDiffExit < 0.4);
    }
}
