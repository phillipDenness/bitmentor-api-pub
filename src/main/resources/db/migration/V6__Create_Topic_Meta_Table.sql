CREATE TABLE bitmentor.topic_meta (
         topic_id integer constraint topic_id_fk
             references bitmentor.topic ON DELETE CASCADE,
         total_jobs jsonb not null,
         grad_jobs jsonb not null,
         date_updated timestamp not null
);