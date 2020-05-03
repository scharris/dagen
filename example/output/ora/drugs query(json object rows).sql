-- [ THIS QUERY WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- JSON_OBJECT_ROWS results representation for drugs query
select
  json_object(
    'name' value q."name",
    'meshId' value q."meshId",
    'cid' value q."cid",
    'registered' value q."registered",
    'marketEntryDate' value q."marketEntryDate",
    'therapeuticIndications' value q."therapeuticIndications",
    'cidPlus1000' value q."cidPlus1000",
    'references' value treat(q."references" as json),
    'brands' value treat(q."brands" as json),
    'advisories' value treat(q."advisories" as json),
    'functionalCategories' value treat(q."functionalCategories" as json),
    'registeredByAnalyst' value q."registeredByAnalyst",
    'compound' value q."compound"
  ) json
from (
  select
    d.name "name",
    d.mesh_id "meshId",
    d.cid "cid",
    d.registered "registered",
    d.market_entry_date "marketEntryDate",
    d.therapeutic_indications "therapeuticIndications",
    d.cid + 1000 "cidPlus1000",
    (
      select
        coalesce(json_arrayagg(json_object(
          'publication' value q."publication"
        ) returning clob), to_clob('[]')) json
      from (
        select
          q."publication" "publication"
        from
          DRUG_REFERENCE dr
          left join (
            select
              r.ID "_ID",
              r.publication "publication"
            from
              REFERENCE r
          ) q on dr.REFERENCE_ID = q."_ID"
        where (
          dr.DRUG_ID = d.ID
        )
      ) q
    ) "references",
    (
      select
        coalesce(json_arrayagg(json_object(
          'brandName' value q."brandName",
          'manufacturer' value q."manufacturer"
        ) returning clob), to_clob('[]')) json
      from (
        select
          b.brand_name "brandName",
          q."manufacturer" "manufacturer"
        from
          BRAND b
          left join (
            select
              m.ID "_ID",
              m.name "manufacturer"
            from
              MANUFACTURER m
          ) q on b.MANUFACTURER_ID = q."_ID"
        where (
          b.DRUG_ID = d.ID
        )
      ) q
    ) "brands",
    (
      select
        coalesce(json_arrayagg(json_object(
          'advisoryText' value q."advisoryText",
          'advisoryType' value q."advisoryType",
          'exprYieldingTwo' value q."exprYieldingTwo",
          'authorityName' value q."authorityName",
          'authorityUrl' value q."authorityUrl",
          'authorityDescription' value q."authorityDescription"
        ) returning clob), to_clob('[]')) json
      from (
        select
          a.text "advisoryText",
          q."advisoryType" "advisoryType",
          q."exprYieldingTwo" "exprYieldingTwo",
          q."authorityName" "authorityName",
          q."authorityUrl" "authorityUrl",
          q."authorityDescription" "authorityDescription"
        from
          ADVISORY a
          left join (
            select
              at.ID "_ID",
              at.name "advisoryType",
              (1 + 1) "exprYieldingTwo",
              q."authorityName" "authorityName",
              q."authorityUrl" "authorityUrl",
              q."authorityDescription" "authorityDescription"
            from
              ADVISORY_TYPE at
              left join (
                select
                  a.ID "_ID",
                  a.name "authorityName",
                  a.url "authorityUrl",
                  a.description "authorityDescription"
                from
                  AUTHORITY a
              ) q on at.AUTHORITY_ID = q."_ID"
          ) q on a.ADVISORY_TYPE_ID = q."_ID"
        where (
          a.DRUG_ID = d.ID
        )
      ) q
    ) "advisories",
    (
      select
        coalesce(json_arrayagg(json_object(
          'categoryName' value q."categoryName",
          'description' value q."description",
          'authorityName' value q."authorityName",
          'authorityUrl' value q."authorityUrl",
          'authorityDescription' value q."authorityDescription"
        ) returning clob), to_clob('[]')) json
      from (
        select
          q."categoryName" "categoryName",
          q."description" "description",
          q1."authorityName" "authorityName",
          q1."authorityUrl" "authorityUrl",
          q1."authorityDescription" "authorityDescription"
        from
          DRUG_FUNCTIONAL_CATEGORY dfc
          left join (
            select
              fc.ID "_ID",
              fc.name "categoryName",
              fc.description "description"
            from
              FUNCTIONAL_CATEGORY fc
          ) q on dfc.FUNCTIONAL_CATEGORY_ID = q."_ID"
          left join (
            select
              a.ID "_ID",
              a.name "authorityName",
              a.url "authorityUrl",
              a.description "authorityDescription"
            from
              AUTHORITY a
          ) q1 on dfc.AUTHORITY_ID = q1."_ID"
        where (
          dfc.DRUG_ID = d.ID
        )
      ) q
    ) "functionalCategories",
    (
      select
        json_object(
          'id' value q."id",
          'shortName' value q."shortName"
        ) json
      from (
        select
          a.id "id",
          a.short_name "shortName"
        from
          ANALYST a
        where (
          d.REGISTERED_BY = a.ID
        )
      ) q
    ) "registeredByAnalyst",
    (
      select
        json_object(
          'displayName' value q."displayName",
          'nctrIsisId' value q."nctrIsisId",
          'cas' value q."cas",
          'entered' value q."entered",
          'enteredByAnalyst' value q."enteredByAnalyst"
        ) json
      from (
        select
          c.display_name "displayName",
          c.nctr_isis_id "nctrIsisId",
          c.cas "cas",
          c.entered "entered",
          (
            select
              json_object(
                'id' value q."id",
                'shortName' value q."shortName"
              ) json
            from (
              select
                a.id "id",
                a.short_name "shortName"
              from
                ANALYST a
              where (
                c.ENTERED_BY = a.ID
              )
            ) q
          ) "enteredByAnalyst"
        from
          COMPOUND c
        where (
          d.COMPOUND_ID = c.ID
        )
      ) q
    ) "compound"
  from
    DRUG d
  where (
    d.mesh_id IN (:drugMeshIdList)
    and
    (not d.id = 2)
  )
) q
