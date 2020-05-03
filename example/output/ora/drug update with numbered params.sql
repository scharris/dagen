-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- drug update with numbered params
update DRUG d
set
  name = ? || '-' || ?,
  compound_id = ?,
  mesh_id = ?
where (
  d.cid = ?
  and
  d.mesh_id = ?
  and
  (d.compound_id < 0)
)
