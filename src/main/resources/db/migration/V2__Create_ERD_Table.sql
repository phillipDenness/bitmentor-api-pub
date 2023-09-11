CREATE TABLE bitmentor.user_account (
    id SERIAL not null
        constraint user_pkey
            primary key,
    title TEXT,
    email TEXT unique not null,
    join_date DATE not null,
    display_name TEXT,
    first_name TEXT,
    middle_name TEXT,
    last_name TEXT,
    last_online TIMESTAMP not null,
    last_modified timestamp not null ,
    profile_image_url TEXT,
    password TEXT not null,
    password_reset TEXT
);

CREATE TABLE bitmentor.tutor (
    id SERIAL not null
         constraint tutor_pkey
             primary key,
    user_id integer not null
        unique
        constraint user_id_fk
        references bitmentor.user_account ON DELETE CASCADE ,
    is_active boolean not null,
    id_verification_state text not null,
    dbs_verification_state text not null,
    tutor_join_date timestamp not null ,
    tagline text not null,
    about text not null,
    experience text,
    github text,
    availability jsonb not null,
    promotions text[],
    rating decimal,
    ratings integer not null
);

CREATE TABLE bitmentor.tutor_detail (
    id SERIAL not null
        constraint tutor_detail_pkey
            primary key,
    tutor_id integer not null
        unique
        constraint tutor_id_fk
            references bitmentor.tutor ON DELETE CASCADE ,
    date_of_birth DATE,
    phone_number TEXT,
    business_name TEXT,
    payee_uuid TEXT,
    last_modified timestamp not null ,
    location jsonb
);


CREATE TABLE bitmentor.discipline (
     id SERIAL not null
         constraint discipline_pkey
             primary key,
     name TEXT not null
);

CREATE TABLE bitmentor.topic (
     id SERIAL not null
         constraint topic_pkey
             primary key,
     name TEXT not null,
     searches int not null ,
     language_id integer constraint topic_id_fk
         references bitmentor.topic ON DELETE CASCADE ,
     discipline_id integer constraint discipline_id_fk not null
         references bitmentor.discipline ON DELETE CASCADE ,
     date_updated timestamp not null
);

CREATE TABLE bitmentor.tutor_topic (
     id SERIAL not null
         constraint tutor_topic_pkey
             primary key,
     tutor_id integer not null
         constraint tutor_id_fk
             references bitmentor.tutor ON DELETE CASCADE ,
     topic_id integer not null
         constraint topic_id_fk
             references bitmentor.topic ON DELETE CASCADE ,
     cost decimal not null
);

CREATE INDEX tutor_id_topic_index ON bitmentor.tutor_topic
    (tutor_id);

CREATE TABLE bitmentor.enquiry (
   id SERIAL not null
       constraint enquiry_pkey
           primary key,
   tutor_user_id integer not null
       constraint tutor_user_id_fk
           references bitmentor.user_account ON DELETE CASCADE ,
   tutor_id integer not null
       constraint tutor_id_fk
           references bitmentor.tutor ON DELETE CASCADE ,
   topic_id integer not null
       constraint topic_id_fk
           references bitmentor.topic ON DELETE CASCADE ,
   student_user_id integer not null
       constraint student_user_id_fk
           references bitmentor.user_account ON DELETE CASCADE ,
   date_created timestamp not null ,
   unique (tutor_user_id, student_user_id)
);


CREATE TABLE bitmentor.message (
   id SERIAL not null
       constraint message_pkey
           primary key,
   sender_id integer not null
       constraint sender_id_fk
           references bitmentor.user_account ON DELETE CASCADE ,
   recipient_id integer not null
       constraint recipient_id_fk
           references bitmentor.user_account ON DELETE CASCADE ,
   enquiry_id integer not null
       constraint thread_id_fk
           references bitmentor.enquiry ON DELETE CASCADE ,
   date_created timestamp not null ,
   message_content TEXT not null
);

CREATE INDEX user_id_message_index ON bitmentor.message
    (
     sender_id,
     recipient_id
);

CREATE TABLE bitmentor.notification (
    id SERIAL not null
        constraint notification_pkey
            primary key,
    user_id integer not null
        constraint user_id_fk
            references bitmentor.user_account ON DELETE CASCADE ,
    type text not null,
    date_created timestamp not null
);

CREATE INDEX user_id_notification_index ON bitmentor.notification
    (
     user_id
);

CREATE TABLE bitmentor.reminder (
    id SERIAL not null
        constraint reminder_pkey
            primary key,
    reminder_type TEXT not null ,
    reminder_payload jsonb not null,
    trigger_date timestamp not null,
    date_created timestamp not null
);

CREATE TABLE bitmentor.lesson (
      id SERIAL not null
          constraint lesson_pkey
              primary key,
      tutor_user_id integer not null
          constraint tutor_id_fk
              references bitmentor.user_account ON DELETE CASCADE ,
      student_id integer not null
          constraint student_id_fk
              references bitmentor.user_account ON DELETE CASCADE ,
      date_lesson timestamp not null,
      end_date_lesson timestamp not null,
      cost decimal not null,
      topic_id integer not null
          constraint topic_id_fk
              references bitmentor.topic ON DELETE CASCADE ,
      date_created timestamp not null,
      enquiry_id integer not null
          constraint enquiry_id_fk
              references bitmentor.enquiry ON DELETE CASCADE ,
      reminder_ids integer[] not null
);

CREATE TABLE bitmentor.lesson_state (
        id SERIAL not null
            constraint lesson_state_pkey
                primary key,
        status TEXT not null,
        date_created timestamp not null,
        lesson_id integer not null
            constraint lesson_id_fk
                references bitmentor.lesson ON DELETE CASCADE
);

CREATE TABLE bitmentor.review (
      id SERIAL not null
          constraint review_pkey
              primary key,
      tutor_id integer not null
          constraint tutor_id_fk
              references bitmentor.tutor ON DELETE CASCADE ,
      student_display_name TEXT not null,
      student_id integer not null,
      topic_name TEXT not null,
      overall_rating integer not null,
      date_created timestamp not null ,
      reason TEXT
);

CREATE TABLE bitmentor.user_file (
     id SERIAL not null
         constraint user_file_pkey
             primary key,
     user_id integer not null
         constraint user_id_fk
             references bitmentor.user_account ON DELETE CASCADE ,
     file_type text not null ,
     file_location text not null ,
     blob_name text not null ,
     bucket text not null ,
     date_created timestamp not null
);

CREATE TABLE bitmentor.bbb_meeting (
       id SERIAL not null
           constraint bbb_pkey
               primary key,
       lesson_id integer not null unique
           constraint lesson_id_fk
               references bitmentor.lesson ON DELETE CASCADE ,
       meeting_id text not null ,
       moderator_pw text not null ,
       duration integer not null ,
       date_created timestamp not null
);

CREATE TABLE bitmentor.payment (
       id SERIAL not null
           constraint payment_pkey
               primary key,
       user_id Int not null
           constraint user_id_fk
               references bitmentor.user_account ON DELETE CASCADE ,
       tutor_user_id Int not null
           constraint tutor_id_fk
               references bitmentor.user_account ON DELETE CASCADE ,
       lesson_id integer not null
           constraint lesson_id_fk
               references bitmentor.lesson ON DELETE CASCADE ,
       payment_id text,
       external_id text not null,
       amount Int not null,
       processing_fee Int not null,
       refund_id TEXT,
       date_updated timestamp not null,
       date_created timestamp not null
);

CREATE TABLE bitmentor.payment_status (
      id SERIAL not null
          constraint payment_status_pkey
              primary key,
      payment_id integer not null
          constraint payment_id_fk
              references bitmentor.payment ON DELETE CASCADE ,
      status TEXT not null,
      date_created timestamp not null
);

CREATE TABLE bitmentor.payout (
      id SERIAL not null
          constraint payout_pkey
              primary key,
      payment_id integer not null
          constraint payment_id_fk
              references bitmentor.payment ON DELETE CASCADE ,
      date_created timestamp not null
);

CREATE TABLE bitmentor.payout_state (
    id SERIAL not null
        constraint payout_state_pkey
            primary key,
    status TEXT not null,
    date_created timestamp not null,
    payout_id integer not null
        constraint payout_id_fk
            references bitmentor.payout ON DELETE CASCADE
);

CREATE TABLE bitmentor.admin (
     id SERIAL not null
         constraint admin_pkey
             primary key,
     user_id integer not null
         constraint user_id_fk
             references bitmentor.user_account ON DELETE CASCADE ,
     date_created timestamp not null
);

INSERT INTO bitmentor.discipline (id, name) VALUES (1, 'Web Development');
INSERT INTO bitmentor.discipline (id, name) VALUES (2, 'Backend Development');
INSERT INTO bitmentor.discipline (id, name) VALUES (3, 'Mobile App Development');
INSERT INTO bitmentor.discipline (id, name) VALUES (4, 'DevOps / Database');
INSERT INTO bitmentor.discipline (id, name) VALUES (5, 'Testing');
INSERT INTO bitmentor.discipline (id, name) VALUES (6, 'Project Management');

INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (1, 'JAVA', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (2, 'JAVASCRIPT', 0, now(), null, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (3, 'KOTLIN', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (4, 'PYTHON', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (5, 'C', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (6, 'C++', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (7, 'C SHARP', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (8, 'OBJECTIVE C', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (9, 'PHP', 0, now(), null, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (10, 'GO', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (11, 'SWIFT', 0, now(), null, 3);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (12, 'SCALA', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (13, 'SQL', 0, now(), null,4);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (14, 'HTML', 0, now(), null, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (15, 'CSS', 0, now(), null, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (16, 'RUBY', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (17, 'R', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (18, 'RUST', 0, now(), null, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (19, 'REACT', 0, now(), 2, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (20, 'ANGULAR', 0, now(), 2, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (21, 'SPRING BOOT', 0, now(), 1, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (22, 'SPRING FRAMEWORK', 0, now(), 1, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (23, 'DJANGO', 0, now(), 4, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (24, 'FLASK', 0, now(), 4, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (25, 'KTOR', 0, now(), 3, 2);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (26, 'VUE', 0, now(), 2, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (27, 'REACT-NATIVE', 0, now(), 2, 3);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (28, 'TYPESCRIPT', 0, now(), 2, 1);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (29, 'SELENIUM', 0, now(), null, 5);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (30, 'CUCUMBER', 0, now(), null, 5);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (31, 'JIRA', 0, now(), null, 6);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (32, 'CONFLUENCE', 0, now(), null, 6);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (33, 'MANUAL TESTING', 0, now(), null, 5);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (34, 'PERFORMANCE TESTING', 0, now(), null, 5);
INSERT INTO bitmentor.topic (id, name, searches, date_updated, language_id, discipline_id) VALUES (35, 'AUTOMATION TESTING', 0, now(), null, 5);


ALTER SEQUENCE bitmentor.user_account_id_seq RESTART WITH 100034;
ALTER SEQUENCE bitmentor.tutor_id_seq RESTART WITH 200101;
ALTER SEQUENCE bitmentor.lesson_id_seq RESTART WITH 300201;
ALTER SEQUENCE bitmentor.enquiry_id_seq RESTART WITH 400301;
ALTER SEQUENCE bitmentor.review_id_seq RESTART WITH 500401;
ALTER SEQUENCE bitmentor.message_id_seq RESTART WITH 600501;
