-- -- Assemblea prova nel futuro
insert into assemblea(id, convocazione, descrizione, fine, id_covepo, id_presidente, id_proprietario, in_corso, nome, odg, partecipanti, step_odg)
values (1,
        '2022-05-10T12:00:00',
        'Assemblea di prova vuota',
        null,
        null,
        null,
        1,
        false,
        'Prova vuota',
        ARRAY ['Nomina di presidente e segretario', 'Presentazione candidature a CdS e PSez', 'Elezione PSez', 'Elezione CdS', 'Varie ed eventuali', 'Lettura e approvazione del verbale'],
        ARRAY [33468, 30413],
        0);

insert into assemblea(id, convocazione, descrizione, fine, id_covepo, id_presidente, id_proprietario, in_corso, nome, odg, partecipanti, step_odg)
values (2,
        '2022-05-10T12:00:00',
        'Assemblea di prova con una votazione preimpostata',
        null,
        null,
        null,
        30413,
        true,
        'Assemblea di sezione - CNGEI Brescia',
        ARRAY ['Nomina di presidente e segretario', 'Presentazione candidature a CdS e PSez', 'Elezione PSez', 'Elezione CdS', 'Varie ed eventuali', 'Lettura e approvazione del verbale'],
        ARRAY [33468, 30413],
        2);

insert into votazione
values (0,
        2,
        1,
        0,
        'Elezione CdS',
        ARRAY ['Mario Balotelli', 'Roberto Baggio', 'Astenuto'],
        false,
        1);