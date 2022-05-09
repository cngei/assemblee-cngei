-- Assemblea prova nel futuro
insert into assemblea
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
        ARRAY [1, 30413],
        0);

insert into assemblea
values (2,
        '2022-05-10T12:00:00',
        'Assemblea di prova con una votazione preimpostata',
        null,
        null,
        null,
        1,
        true,
        'Assemblea di sezione - CNGEI Brescia',
        ARRAY ['Nomina di presidente e segretario', 'Presentazione candidature a CdS e PSez', 'Elezione PSez', 'Elezione CdS', 'Varie ed eventuali', 'Lettura e approvazione del verbale'],
        ARRAY [1, 30413],
        2);

-- Assemblea prova nel passato
insert into assemblea
values (3,
        '2021-05-10T12:00:00',
        'descrizione',
        '2021-05-10T13:00:00',
        null,
        null,
        1,
        false,
        'Prova',
        ARRAY ['Prova'],
        ARRAY [1, 30413],
        1);

insert into votazione
values (1,
        2,
        '2022-05-10T12:00:00',
        'Elezione CdS',
        ARRAY ['Mario Balotelli', 'Roberto Baggio', 'Scheda bianca', 'Scheda nulla'],
        false,
        1)