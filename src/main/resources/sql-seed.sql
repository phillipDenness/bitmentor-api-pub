INSERT INTO codetutorclub.user_account
VALUES (1,'dennessphillip@gmail.com','2020-10-17','Phillip','Phillip','Denness',now(),now(),'https://storage.googleapis.com/download/storage/v1/b/code-tutor-club-dev-display-images/o/user1-image.jpg?generation=1603392386505402&alt=media','{"city": "Leeds", "country": "United Kingdom", "postcode": "LS10 2NG", "firstLineAddress": "1 Arthington Street", "secondLineAddress": ""}','$1$CkgoOEEL$yYQZRiHlUWYniYghxYwVc.',null);

INSERT INTO codetutorclub.user_account
VALUES (2,'1992wildfire@googlemail.com','2020-10-17','Barry','Barry','Brown',now(),now(),'https://storage.googleapis.com/download/storage/v1/b/code-tutor-club-dev-display-images/o/user1-image.jpg?generation=1603392386505402&alt=media','{"city": "Leeds", "country": "West Yorkshire", "postcode": "LS10 2NG", "firstLineAddress": "1 Arthington Street", "secondLineAddress": ""}','$1$CkgoOEEL$yYQZRiHlUWYniYghxYwVc.',null);

insert into codetutorclub.tutor
values (1,1,true,true,now(),'fwfw2523','fwfw','{"friday": {"evening": false, "morning": false, "afternoon": false}, "monday": {"evening": false, "morning": false, "afternoon": true}, "sunday": {"evening": false, "morning": false, "afternoon": false}, "tuesday": {"evening": false, "morning": true, "afternoon": false}, "saturday": {"evening": false, "morning": false, "afternoon": false}, "thursday": {"evening": false, "morning": false, "afternoon": false}, "wednesday": {"evening": false, "morning": false, "afternoon": false}}',now(),null,0);

insert into codetutorclub.tutor
values (2,2,true,true,now(),'fwfw2523','fwfw','{"friday": {"evening": false, "morning": false, "afternoon": false}, "monday": {"evening": false, "morning": false, "afternoon": true}, "sunday": {"evening": false, "morning": false, "afternoon": false}, "tuesday": {"evening": false, "morning": true, "afternoon": false}, "saturday": {"evening": false, "morning": false, "afternoon": false}, "thursday": {"evening": false, "morning": false, "afternoon": false}, "wednesday": {"evening": false, "morning": false, "afternoon": false}}',now(),null,0);

insert into codetutorclub.skill
values (1,1,'ANGULAR',30);

insert into codetutorclub.skill
values (2,2,'REACT',25);

insert into codetutorclub.enquiry
values (1,1,2);

insert into codetutorclub.message
values (1,2,1,1,now(),'hello');

insert into codetutorclub.message
values (2,1,2,1,now(),'Yes hello. Shall i book a lesson');

insert into codetutorclub.lesson
values (1,1,2,now(),now(),30,'ANGULAR',now(),1);

insert into codetutorclub.lesson_state
values (1,'PENDING',now(),1);

insert into codetutorclub.lesson_state
values (2,'CONFIRMED',now(),1);


ALTER SEQUENCE codetutorclub.user_account_id_seq INCREMENT BY 5;
ALTER SEQUENCE codetutorclub.tutor_id_seq INCREMENT BY 5;
ALTER SEQUENCE codetutorclub.skill_id_seq INCREMENT BY 5;
ALTER SEQUENCE codetutorclub.enquiry_id INCREMENT BY 5;
ALTER SEQUENCE codetutorclub.message_id_seq INCREMENT BY 5;
ALTER SEQUENCE codetutorclub.lesson_id_seq INCREMENT BY 5;
ALTER SEQUENCE codetutorclub.lesson_state_id_seq INCREMENT BY 5;