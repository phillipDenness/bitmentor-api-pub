CREATE TABLE bitmentor.tutor_project (
     id SERIAL not null
         constraint tutor_project_pkey
             primary key,
     tutor_id integer not null
         constraint tutor_id_fk
             references bitmentor.tutor ON DELETE CASCADE,
     title VARCHAR(80) not null,
     description VARCHAR(600) not null,
     link VARCHAR(600),
     last_modified timestamp not null
);