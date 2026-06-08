-- V21: Neue group_recipes Join-Tabelle (M:N Beziehung)
-- Rezepte gehören nicht mehr "einer" Gruppe, sondern werden via Join referenziert.
-- visibility='GROUP' wird deprecated – alle Rezepte bleiben PUBLIC nach Peer Review.

CREATE TABLE group_recipes (
    group_id    UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    recipe_id   UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    added_by    UUID REFERENCES users(id),
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, recipe_id)
);

CREATE INDEX idx_group_recipes_recipe ON group_recipes(recipe_id);
