package com.joaoferraz.livara.studyplanner.ui;

import java.util.Objects;

record StudyTask(String id, String title, String description) {

    StudyTask {
        id = Objects.requireNonNull(id, "id").trim();
        title = Objects.requireNonNull(title, "title").trim();
        description = Objects.requireNonNull(description, "description").trim();
        if (id.isBlank() || title.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Study task fields cannot be blank");
        }
    }
}
