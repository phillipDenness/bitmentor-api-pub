INSERT INTO bitmentor.discipline (id, name) VALUES (7, 'Statistics');
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (36, 'SAS', 0, now(), null, 7);
UPDATE bitmentor.topic SET discipline_id = 7 WHERE id = 17;
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (37, 'MATLAB', 0, now(), null, 7);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (38, 'DART', 0, now(), null,2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (39, 'PERL', 0, now(), null,2);
