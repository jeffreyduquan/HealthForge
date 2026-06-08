-- V20: Fügt CONTRIBUTOR-Rolle zum group_members CHECK-Constraint hinzu.
-- Siehe User-Spec: 3 Rollen – Admin/Owner, Contributor, Member.
-- OWNER und ADMIN sind gleichberechtigt (volle Verwaltung).
-- CONTRIBUTOR darf Gruppen-Rezepte anlegen (wie Member), zusätzlich
--   explizit "Rezepte der Gruppe hinzufügen".

ALTER TABLE group_members DROP CONSTRAINT IF EXISTS group_members_role_check;
ALTER TABLE group_members ADD CONSTRAINT group_members_role_check
    CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'CONTRIBUTOR'));
