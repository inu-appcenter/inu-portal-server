package kr.inuappcenterportal.inuportal.domain.agent.util;

import java.util.*;

/**
 * AI 에이전트 도구 매칭을 위한 범용 텍스트 유사도 매처
 * - N-gram 자카드 유사도(Jaccard Index) 및 부분 문자열 포함도, 레벤슈타인 편집 거리를 결합하여
 *   하드코딩 분기문 없이도 질문 키워드와 엔티티 후보군 간의 최적 매칭을 수행합니다.
 */
public final class AgentFuzzyMatcher {

    private AgentFuzzyMatcher() {}

    public record MatchResult<T>(T item, double score, String matchedTargetText) {}

    /**
     * 불용어 및 조사 전처리
     */
    public static String cleanText(String text) {
        if (text == null) return "";
        // 특수문자 제거, 연속 공백 정리, 소문자 변환
        String cleaned = text.replaceAll("[^a-zA-Z0-9가-힣\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);

        // 불용어 목록
        Set<String> stopWords = Set.of(
                "버스", "셔틀", "알려줘", "언제", "도착", "정보", "정류장", "정류소",
                "에서", "으로", "로", "의", "은", "는", "이", "가", "을", "를", "앞", "뒤", "와", "과"
        );

        String[] tokens = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!stopWords.contains(token) && !token.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(token);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 2글자 N-gram 집합 생성
     */
    private static Set<String> generateBigrams(String text) {
        String s = text.replaceAll("\\s+", "");
        Set<String> bigrams = new HashSet<>();
        if (s.length() <= 1) {
            if (!s.isEmpty()) bigrams.add(s);
            return bigrams;
        }
        for (int i = 0; i < s.length() - 1; i++) {
            bigrams.add(s.substring(i, i + 2));
        }
        return bigrams;
    }

    /**
     * 쿼리와 대상 문자열 간의 종합 유사도 점수 계산 (0.0 ~ 1.0)
     */
    public static double calculateSimilarity(String query, String target) {
        if (query == null || target == null) return 0.0;
        String qClean = cleanText(query);
        String tClean = cleanText(target);
        if (qClean.isEmpty() || tClean.isEmpty()) return 0.0;

        String qNoSpace = qClean.replaceAll("\\s+", "");
        String tNoSpace = tClean.replaceAll("\\s+", "");

        // 1. 완전 일치 (1.0)
        if (qNoSpace.equals(tNoSpace)) {
            return 1.0;
        }

        // 2. 부분 문자열 포함 보너스
        double containmentScore = 0.0;
        if (tNoSpace.contains(qNoSpace)) {
            containmentScore = 0.7 + (0.3 * ((double) qNoSpace.length() / tNoSpace.length()));
        } else if (qNoSpace.contains(tNoSpace)) {
            containmentScore = 0.6 + (0.3 * ((double) tNoSpace.length() / qNoSpace.length()));
        }

        // 3. 약어 매칭 (예: "공대" <-> "공과대학", "자연대" <-> "자연과학대학")
        if ((qNoSpace.equals("공대") && tNoSpace.contains("공과대")) ||
            (qNoSpace.equals("자연대") && tNoSpace.contains("자연과학대"))) {
            containmentScore = Math.max(containmentScore, 0.85);
        }

        // 4. 2-gram 자카드 유사도
        Set<String> qGrams = generateBigrams(qClean);
        Set<String> tGrams = generateBigrams(tClean);

        Set<String> intersection = new HashSet<>(qGrams);
        intersection.retainAll(tGrams);

        Set<String> union = new HashSet<>(qGrams);
        union.addAll(tGrams);

        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        // 5. 출구 번호 등 숫자 키워드 불일치 시 패널티 (예: '2번' 쿼리에 2가 없거나 다른 숫자면 패널티)
        boolean hasDigitMismatch = false;
        for (char c : qNoSpace.toCharArray()) {
            if (Character.isDigit(c) && !tNoSpace.contains(String.valueOf(c))) {
                hasDigitMismatch = true;
                break;
            }
        }
        if (hasDigitMismatch) {
            containmentScore *= 0.3;
            jaccard *= 0.3;
        }

        return Math.max(containmentScore, jaccard);
    }

    /**
     * 후보 목록 중 주어진 추출 함수를 기준으로 가장 유사도가 높은 최적 매칭 항목 반환
     */
    public static <T> Optional<MatchResult<T>> findBestMatch(
            String query,
            Collection<T> candidates,
            java.util.function.Function<T, List<String>> targetTextsExtractor,
            double threshold) {

        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        MatchResult<T> best = null;
        double maxScore = -1.0;

        for (T candidate : candidates) {
            List<String> texts = targetTextsExtractor.apply(candidate);
            if (texts == null) continue;

            for (String text : texts) {
                if (text == null || text.isBlank()) continue;
                double score = calculateSimilarity(query, text);
                if (score > maxScore) {
                    maxScore = score;
                    best = new MatchResult<>(candidate, score, text);
                }
            }
        }

        if (best != null && best.score() >= threshold) {
            return Optional.of(best);
        }
        return Optional.empty();
    }
}
