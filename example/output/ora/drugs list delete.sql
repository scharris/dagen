-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- drugs list delete
delete from DRUG
where (
  cid IN (:cidCond)
)
