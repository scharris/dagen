-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- drug insert with named params
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
    :id,
    :namePrefix || ':' || :nameSuffix,
    2 - 1,
    :meshId,
    :registeredBy
  )
