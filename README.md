# one2onechat
onw to one chating system using websocket in reactive application


CREATE TABLE ct_meassages (
    chatid varchar NOT NULL,
    user1 int4 NULL,
    user2 int4 NULL,
    messages jsonb NULL DEFAULT '[]'::jsonb,
    CONSTRAINT ct_meassages_pkey PRIMARY KEY (chatid)
);
