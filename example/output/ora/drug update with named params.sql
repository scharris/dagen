-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- drug update with named params
update DRUG d
set
  name = :namePartOne || '-' || :namePartTwo,
  compound_id = :compoundId,
  mesh_id = :meshId
where (
  d.cid = :cidCond
  and
  d.mesh_id = :oldMeshId
  and
  (d.compound_id < 0)
)
