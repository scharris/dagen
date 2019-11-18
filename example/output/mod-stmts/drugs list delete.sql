-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- drugs list delete
delete from drug
where (
  cid IN (:cidCond)
)
