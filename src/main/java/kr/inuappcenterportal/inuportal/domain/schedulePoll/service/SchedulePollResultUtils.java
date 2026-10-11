package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.AvailableRangeDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.DurationMode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 결과 계산(히트맵, 추천 후보). DB와 무관한 계산만 한다.
//
// 칸: 조율 시간대를 30분씩 자른 것. heatmap[열][칸]의 칸 r은 rangeStart + 30분 × r 부터 30분이다.
// 참여자 번호(index): 넘겨받은 목록의 순서(0부터). 히트맵 숫자에서 index번째 자리가 1이면 그 사람이 가능하다.
public class SchedulePollResultUtils {

    public static final int MAX_CANDIDATES = 3;

    private SchedulePollResultUtils() {
    }

    public record Result(int[][] heatmap, List<Candidate> candidates) {
    }

    // durationMin: FIXED면 회의 길이, AT_LEAST면 그 구간의 실제 길이
    public record Candidate(int rank, String key, String start, String end, int durationMin,
                            int availableMask, int availableCount, boolean allAvailable) {
    }

    // 후보를 고르기 전 단계의 구간. 칸 번호 [startRow, endRow)
    private record Window(int column, int startRow, int endRow, int mask) {

        int length() {
            return endRow - startRow;
        }
    }

    // participantRanges: 참여자 순서대로, 각자 저장된(검증·정리된) 가능 시간 구간 목록
    public static Result calculate(SchedulePoll poll, List<List<AvailableRangeDto>> participantRanges) {
        List<String> columns = SchedulePollSlotUtils.columnKeys(poll);
        int rows = (poll.getRangeEndMinutes() - poll.getRangeStartMinutes()) / SchedulePollSlotUtils.SLOT_MINUTES;

        int[][] heatmap = buildHeatmap(poll, columns, rows, participantRanges);

        List<Window> windows = poll.getDurationMode() == DurationMode.FIXED
                ? fixedWindows(heatmap, rows, poll.getDurationMinutes() / SchedulePollSlotUtils.SLOT_MINUTES)
                : atLeastWindows(heatmap, rows, poll.getDurationMinutes() / SchedulePollSlotUtils.SLOT_MINUTES);

        List<Window> ranked = sort(mergeSamePeople(windows), poll.getDurationMode());

        int allMask = (1 << participantRanges.size()) - 1;
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_CANDIDATES, ranked.size()); i++) {
            Window w = ranked.get(i);
            int durationMin = poll.getDurationMode() == DurationMode.FIXED
                    ? poll.getDurationMinutes()
                    : w.length() * SchedulePollSlotUtils.SLOT_MINUTES;
            candidates.add(new Candidate(
                    i + 1,
                    columns.get(w.column()),
                    toTime(poll, w.startRow()),
                    toTime(poll, w.endRow()),
                    durationMin,
                    w.mask(),
                    Integer.bitCount(w.mask()),
                    w.mask() == allMask));
        }
        return new Result(heatmap, candidates);
    }

    // 사람마다 칠한 칸의 자리(1 << index)를 칸에 더한다. 열마다 따로 계산하므로 한 열 최대 30칸이다.
    private static int[][] buildHeatmap(SchedulePoll poll, List<String> columns, int rows,
                                        List<List<AvailableRangeDto>> participantRanges) {
        int[][] heatmap = new int[columns.size()][rows];
        for (int index = 0; index < participantRanges.size(); index++) {
            int bit = 1 << index;
            for (AvailableRangeDto range : participantRanges.get(index)) {
                int column = columns.indexOf(range.key());
                if (column < 0) {
                    throw new IllegalStateException("조율에 없는 열이 저장돼 있습니다: " + range.key());
                }
                int startRow = toRow(poll, range.start());
                int endRow = toRow(poll, range.end());
                for (int row = startRow; row < endRow; row++) {
                    heatmap[column][row] |= bit;
                }
            }
        }
        return heatmap;
    }

    // FIXED: 시작 칸마다 회의 길이만큼의 창을 만들고, 창 안 모든 칸에 가능한 사람(교집합)을 구한다. 0명은 버린다.
    private static List<Window> fixedWindows(int[][] heatmap, int rows, int length) {
        List<Window> windows = new ArrayList<>();
        for (int column = 0; column < heatmap.length; column++) {
            for (int start = 0; start + length <= rows; start++) {
                int mask = ~0;
                for (int row = start; row < start + length; row++) {
                    mask &= heatmap[column][row];
                }
                if (mask != 0) {
                    windows.add(new Window(column, start, start + length, mask));
                }
            }
        }
        return windows;
    }

    // AT_LEAST: 같은 사람들이 내내 가능한 가장 긴 구간을 찾는다(다른 사람이 더 있어도 이어진다).
    // 양옆으로 한 칸 늘리면 그 사람들 중 누군가 안 되는 구간만 남기고, N분보다 짧으면 버린다.
    private static List<Window> atLeastWindows(int[][] heatmap, int rows, int minLength) {
        List<Window> windows = new ArrayList<>();
        for (int column = 0; column < heatmap.length; column++) {
            int[] cells = heatmap[column];
            for (int start = 0; start < rows; start++) {
                int mask = ~0;
                for (int end = start + 1; end <= rows; end++) {
                    mask &= cells[end - 1];
                    if (mask == 0) {
                        break;
                    }
                    boolean leftClosed = start == 0 || (cells[start - 1] & mask) != mask;
                    boolean rightClosed = end == rows || (cells[end] & mask) != mask;
                    if (leftClosed && rightClosed && end - start >= minLength) {
                        windows.add(new Window(column, start, end, mask));
                    }
                }
            }
        }
        return windows;
    }

    // 같은 열에서 같은 사람들(mask 동일)의 후보가 겹치거나 붙어 있으면 가장 이른 것 하나만 남긴다.
    // 10:00~11:00, 10:30~11:30, 11:00~12:00처럼 줄줄이 이어진 것도 한 묶음으로 본다. 구간을 늘리지는 않는다.
    private static List<Window> mergeSamePeople(List<Window> windows) {
        Map<String, List<Window>> groups = new LinkedHashMap<>();
        for (Window w : windows) {
            groups.computeIfAbsent(w.column() + ":" + w.mask(), k -> new ArrayList<>()).add(w);
        }
        List<Window> kept = new ArrayList<>();
        for (List<Window> group : groups.values()) {
            group.sort(Comparator.comparingInt(Window::startRow));
            int chainEnd = -1;
            for (Window w : group) {
                if (w.startRow() <= chainEnd) {
                    chainEnd = Math.max(chainEnd, w.endRow());
                    continue;
                }
                kept.add(w);
                chainEnd = w.endRow();
            }
        }
        return kept;
    }

    // 가능 인원 많은 순 → (AT_LEAST면 긴 순) → 앞 열 → 이른 시작
    private static List<Window> sort(List<Window> windows, DurationMode mode) {
        Comparator<Window> order = Comparator.comparingInt((Window w) -> Integer.bitCount(w.mask())).reversed();
        if (mode == DurationMode.AT_LEAST) {
            order = order.thenComparing(Comparator.comparingInt(Window::length).reversed());
        }
        order = order.thenComparingInt(Window::column).thenComparingInt(Window::startRow);
        return windows.stream().sorted(order).toList();
    }

    private static int toRow(SchedulePoll poll, String time) {
        return (SchedulePollSlotUtils.toMinutes(time) - poll.getRangeStartMinutes()) / SchedulePollSlotUtils.SLOT_MINUTES;
    }

    private static String toTime(SchedulePoll poll, int row) {
        return SchedulePollSlotUtils.formatMinutes(poll.getRangeStartMinutes() + row * SchedulePollSlotUtils.SLOT_MINUTES);
    }
}
