package com.joaoferraz.livara.studyplanner.domain;

import com.joaoferraz.livara.studyplanner.io.ScheduleStore;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScheduleService {
    private final ScheduleStore store;

    public ScheduleService(ScheduleStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public ScheduleTemplate loadOrCreate(Path path) throws IOException {
        if (!path.toFile().exists()) {
            ScheduleTemplate template = DefaultScheduleFactory.create(Cycle.A);
            save(path, template);
            return template;
        }
        ScheduleTemplate template = store.load(path);
        requireValid(template);
        return template;
    }

    public void save(Path path, ScheduleTemplate template) throws IOException {
        requireValid(template);
        store.save(path, template);
    }

    public List<String> validate(ScheduleTemplate template) {
        return ScheduleValidator.validate(template);
    }

    public ScheduleTemplate advanceCycle(ScheduleTemplate template) {
        requireValid(template);
        return template.withCycle(template.cycle().next());
    }

    public ScheduleTemplate editTemplate(ScheduleTemplate current, String name, Cycle activeCycle,
                                         WorkflowTemplate workflow, int pauseMinutes, String iconId,
                                         List<StudyBlock> sequence) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(activeCycle, "activeCycle");
        Objects.requireNonNull(workflow, "workflow");
        Objects.requireNonNull(sequence, "sequence");
        if (sequence.isEmpty()) {
            throw new IllegalArgumentException("A template needs at least one study block");
        }
        List<StudyBlock> normalized = new ArrayList<>();
        for (int index = 0; index < sequence.size(); index++) {
            StudyBlock block = sequence.get(index);
            normalized.add(new StudyBlock(index + 1, block.focus(), block.topic(), block.duration(), pauseMinutes));
        }
        Map<DayOfWeek, List<StudyBlock>> projected = projectSequence(normalized, current.blocks(current.cycle()));
        EnumMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> cycles = new EnumMap<>(Cycle.class);
        for (Cycle value : Cycle.values()) {
            cycles.put(value, value == activeCycle ? projected : current.blocks(value));
        }
        ScheduleTemplate edited = ScheduleTemplate.withCycles(2, name, activeCycle, workflow, pauseMinutes,
                iconId, cycles);
        requireValid(edited);
        return edited;
    }

    private static Map<DayOfWeek, List<StudyBlock>> projectSequence(List<StudyBlock> sequence,
                                                                      Map<DayOfWeek, List<StudyBlock>> previous) {
        EnumMap<DayOfWeek, List<StudyBlock>> result = new EnumMap<>(DayOfWeek.class);
        int cursor = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            List<StudyBlock> blocks = new ArrayList<>();
            int slots = previous.getOrDefault(day, List.of()).size();
            for (int index = 0; index < slots && cursor < sequence.size(); index++) {
                StudyBlock source = sequence.get(cursor++);
                blocks.add(new StudyBlock(blocks.size() + 1, source.focus(), source.topic(), source.duration(),
                        source.breakAfterMinutes()));
            }
            result.put(day, blocks);
        }
        List<StudyBlock> overflow = result.get(DayOfWeek.SATURDAY);
        while (cursor < sequence.size()) {
            StudyBlock source = sequence.get(cursor++);
            overflow = new ArrayList<>(overflow);
            overflow.add(new StudyBlock(overflow.size() + 1, source.focus(), source.topic(), source.duration(),
                    source.breakAfterMinutes()));
            result.put(DayOfWeek.SATURDAY, overflow);
        }
        return result;
    }

    private void requireValid(ScheduleTemplate template) {
        ScheduleValidator.requireValid(template);
    }
}
