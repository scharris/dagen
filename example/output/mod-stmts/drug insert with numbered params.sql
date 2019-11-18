-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- drug insert with numbered params
insert into drug
  (
    id,
    name,
    compound_id,
    mesh_id,
    registered_by
  )
values
  (
    ?,
    (? || ':' || ?),
    (2 - 1),
    ?,
    ?
  )
