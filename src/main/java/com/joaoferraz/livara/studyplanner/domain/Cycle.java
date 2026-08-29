package com.joaoferraz.livara.studyplanner.domain;

public enum Cycle {
    A("A", "Physics + Biology"),
    B("B", "Chemistry + Mathematics");

    private final String label;
    private final String subjects;

    Cycle(String label, String subjects) {
        this.label = label;
        this.subjects = subjects;
    }

    public String label() {
        return label;
    }

    public String subjects() {
        return subjects;
    }

    public Cycle next() {
        return this == A ? B : A;
    }
}
