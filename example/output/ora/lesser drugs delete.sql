-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- lesser drugs delete
delete from DRUG
where (
  cid < :cidCond
)
