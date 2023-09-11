CREATE TABLE bitmentor.paypal_payment (
        order_id TEXT not null
            constraint paypal_payment_pkey
                primary key,
        lesson_id integer not null
            constraint lesson_id_fk
                references bitmentor.lesson ON DELETE CASCADE,
        status TEXT not null,
        gross_amount TEXT,
        net_amount TEXT,
        paypal_fee TEXT,
        capture_id TEXT,
        date_updated timestamp not null
);