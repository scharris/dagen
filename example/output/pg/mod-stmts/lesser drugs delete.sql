-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- lesser drugs delete
delete from drug
where (
  cid < :cidCond
)
