CREATE TABLE bitmentor.student_preference(
     id SERIAL not null
         constraint student_preference_pkey
             primary key,
     user_id integer not null
         unique
         constraint user_id_fk
             references bitmentor.user_account ON DELETE CASCADE ,
     topic_ids integer[],
     interests text[],
     other text,
     date_created timestamp not null
)