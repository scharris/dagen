/* Database setup (Postgres)
postgres=# create user drugsuser with password 'drugspassword';
postgres=# create database drugs owner drugsuser;
^D
psql -U drugsuser drugs
drugs> \i schema-setup.sql
(Or just paste the file contents.)
 */

create table compound
(
    id                 integer not null
        constraint compound_pk
            primary key,
    display_name       varchar(50),
    nctr_isis_id       varchar(100),
    smiles             varchar(2000),
    canonical_smiles   varchar(2000),
    cas                varchar(50),
    mol_formula        varchar(2000),
    mol_weight         numeric,
    mol_file           text,
    inchi              varchar(2000),
    inchi_key          varchar(27),
    standard_inchi     varchar(2000),
    standard_inchi_key varchar(27)
);

create table drug
(
    id                      integer      not null
        constraint drug_pk
            primary key,
    name                    varchar(500) not null
        constraint drug_name_un
            unique,
    compound_id             integer      not null
        constraint drug_compound_fk
            references compound,
    mesh_id                 varchar(7)
        constraint drug_meshid_un
            unique,
    drugbank_id             varchar(7)
        constraint drug_drugbankid_un
            unique,
    cid                     integer,
    therapeutic_indications varchar(4000),
    spl                     xml
);

create index drug_compoundid_ix
    on drug (compound_id);

create index compound_inchikey_ix
    on compound (inchi_key);

create index compound_stdinchikey_ix
    on compound (standard_inchi_key);

create index compound_canonsmiles_ix
    on compound (canonical_smiles);

create table reference
(
    id          integer       not null
        constraint reference_pk
            primary key,
    publication varchar(2000) not null
);

create table drug_reference
(
    drug_id      integer not null
        constraint drug_reference_drug_fk
            references drug
            on delete cascade,
    reference_id integer not null
        constraint drug_reference_reference_fk
            references reference
            on delete cascade,
    priority     integer,
    constraint drug_reference_pk
        primary key (drug_id, reference_id)
);

create index drug_reference_referenceid_ix
    on drug_reference (reference_id);

create table authority
(
    id          integer      not null
        constraint authority_pk
            primary key,
    name        varchar(200) not null
        constraint authority_name_un
            unique,
    url         varchar(500),
    description varchar(2000)
);

create table advisory_type
(
    id           integer     not null
        constraint advisory_type_pk
            primary key,
    name         varchar(50) not null
        constraint advisory_type_name_un
            unique,
    authority_id integer     not null
        constraint advisory_type_authority_fk
            references authority
);

create table advisory
(
    id               integer       not null
        constraint advisory_pk
            primary key,
    drug_id          integer       not null
        constraint advisory_drug_fk
            references drug
            on delete cascade,
    advisory_type_id integer       not null
        constraint advisory_advisory_type_fk
            references advisory_type,
    text             varchar(2000) not null
);

create index advisory_advtype_ix
    on advisory (advisory_type_id);

create index advisory_drug_ix
    on advisory (drug_id);

create table functional_category
(
    id                            integer      not null
        constraint category_pk
            primary key,
    name                          varchar(500) not null
        constraint functional_category_name_un
            unique,
    description                   varchar(2000),
    parent_functional_category_id integer
        constraint funcat_funcat_fk
            references functional_category
);

create table drug_functional_category
(
    drug_id                integer not null
        constraint drug_category_drug_fk
            references drug
            on delete cascade,
    functional_category_id integer not null
        constraint drug_funcat_funcat_fk
            references functional_category,
    authority_id           integer not null
        constraint drugfuncat_authority_fk
            references authority,
    seq                    integer,
    constraint drugfuncat_pk
        primary key (drug_id, functional_category_id, authority_id)
);

create index drugfuncat_funcat_ix
    on drug_functional_category (functional_category_id);

create index drugfuncat_authority_ix
    on drug_functional_category (authority_id);

create index funcat_parentfuncat_ix
    on functional_category (parent_functional_category_id);

create table manufacturer
(
    id   integer      not null
        constraint manufacturer_pk
            primary key,
    name varchar(200) not null
        constraint manufacturer_name_un
            unique
);

create table brand
(
    drug_id         integer      not null
        constraint brand_drug_fk
            references drug
            on delete cascade,
    brand_name      varchar(200) not null,
    language_code   varchar(10),
    manufacturer_id integer
        constraint brand_manufacturer_fk
            references manufacturer,
    constraint brand_pk
        primary key (drug_id, brand_name)
);

create index brand_mfr_ix
    on brand (manufacturer_id);


-- Test data

insert into authority(id, name, url, description)
  values(1, 'FDA', 'http://www.fda.gov', 'Food and Drug Administration');
insert into advisory_type(id, name, authority_id)
 values(1, 'Boxed Warning', 1);
insert into advisory_type(id, name, authority_id)
 values(2, 'Caution', 1);
insert into functional_category(id, name, description, parent_functional_category_id)
  values(1, 'Category A', 'Top level category A', null);
insert into functional_category(id, name, description, parent_functional_category_id)
  values(2, 'Category A.1', 'sub category 1 of A', 1);
insert into functional_category(id, name, description, parent_functional_category_id)
  values(3, 'Category A.1.1', 'sub category 1 of A.1', 2);
insert into functional_category(id, name, description, parent_functional_category_id)
  values(4, 'Category B', 'Top level category B', null);
insert into functional_category(id, name, description, parent_functional_category_id)
  values(5, 'Category B.1', 'sub category 1 of B', 4);
insert into functional_category(id, name, description, parent_functional_category_id)
  values(6, 'Category B.1.1', 'sub category 1 of B.1', 5);
insert into manufacturer(id, name)
  values(1, 'Acme Drug Co');
insert into manufacturer(id, name)
  values(2, 'PharmaCorp');
insert into manufacturer(id, name)
  values(3, 'SellsAll Drug Co.');


insert into compound(id, display_name, nctr_isis_id, cas)
  select n,
    'Test Compound ' || n ,
    'ISIS-' || n ,
    '5'||n||n||n||n||'-'||n||n
  from generate_series(1,5) n
;

insert into drug(id, name, compound_id, therapeutic_indications, spl)
  select
    generate_series,
    'Test Drug ' || generate_series, generate_series,
    'Indication ' || generate_series,
    xmlparse(document '<document><gen-name>drug ' || generate_series || '</gen-name></document>')
  from generate_series(1,5)
;


insert into reference(id, publication)
 select 100*generate_series + 1, 'Publication 1 about drug # ' || generate_series
 from generate_series(1,5)
;

insert into reference(id, publication)
 select 100*generate_series + 2, 'Publication 2 about drug # ' || generate_series
 from generate_series(1,5)
;

insert into reference(id, publication)
 select 100*generate_series + 3, 'Publication 3 about drug # ' || generate_series
 from generate_series(1,5)
;

insert into drug_reference (drug_id, reference_id, priority)
 select generate_series, 100*generate_series + 1, generate_series
 from generate_series(1,5)
;

insert into drug_reference (drug_id, reference_id, priority)
 select generate_series, 100*generate_series + 2, generate_series
 from generate_series(1,5)
;

insert into drug_reference (drug_id, reference_id, priority)
 select generate_series, 100*generate_series + 3, generate_series
 from generate_series(1,5)
;

insert into drug_functional_category(drug_id, functional_category_id, authority_id, seq)
 select generate_series, mod(generate_series,3)+1, 1, 1
 from generate_series(1,5)
;

insert into drug_functional_category(drug_id, functional_category_id, authority_id, seq)
 select generate_series, mod(generate_series,3)+4, 1, 2
 from generate_series(1,5)
;

insert into brand(drug_id, brand_name, language_code, manufacturer_id)
 select generate_series, 'Brand'||generate_series||'(TM)', 'EN', mod(generate_series,3)+1
 from generate_series(1,5)
;

insert into advisory(id, drug_id, advisory_type_id, text)
 select 100*generate_series+1, generate_series, 1, 'Advisory concerning drug ' || generate_series
 from generate_series(1,5)
;

insert into advisory(id, drug_id, advisory_type_id, text)
 select 100*generate_series+2, generate_series, 2, 'Caution concerning drug ' || generate_series
 from generate_series(1,5)
;
