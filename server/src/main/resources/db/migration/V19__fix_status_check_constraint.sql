-- V19: Erweitert recipes.status CHECK-Constraint um PENDING_REVIEW + REJECTED
-- Der RecipeService.setStatus() verwendet diese Werte für Rezept-Review (REQ-ADMIN-007).
-- Der alte Constraint erlaubte nur 'PUBLISHED' oder 'REMOVED'.

ALTER TABLE recipes DROP CONSTRAINT IF EXISTS recipes_status_check;
ALTER TABLE recipes ADD CONSTRAINT recipes_status_check
    CHECK (status IN ('PUBLISHED', 'REMOVED', 'PENDING_REVIEW', 'REJECTED'));
