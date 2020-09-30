-- [ THIS QUERY WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
-- JSON_OBJECT_ROWS results representation for drugs query
select
  jsonb_build_object(
    'name', q.name,
    'meshId', q."meshId",
    'cid', q.cid,
    'registered', q.registered,
    'marketEntryDate', q."marketEntryDate",
    'therapeuticIndications', q."therapeuticIndications",
    'cidPlus1000', q."cidPlus1000",
    'references', q.references,
    'brands', q.brands,
    'advisories', q.advisories,
    'functionalCategories', q."functionalCategories",
    'registeredByAnalyst', q."registeredByAnalyst",
    'compound', q.compound
  ) json
from (
  select
    d.name as name,
    d.mesh_id "meshId",
    d.cid as cid,
    d.registered as registered,
    d.market_entry_date "marketEntryDate",
    d.therapeutic_indications "therapeuticIndications",
    d.cid + 1000 "cidPlus1000",
    (
      select
        coalesce(jsonb_agg(jsonb_build_object(
          'publication', q.publication
        )),'[]'::jsonb) json
      from (
        select
          q.publication as publication
        from
          drug_reference dr
          left join (
            select
              r.id "_id",
              r.publication as publication
            from
              reference r
          ) q on dr.reference_id = q."_id"
        where (
          dr.drug_id = d.id
        )
      ) q
    ) as references,
    (
      select
        coalesce(jsonb_agg(jsonb_build_object(
          'brandName', q."brandName",
          'manufacturer', q.manufacturer
        )),'[]'::jsonb) json
      from (
        select
          b.brand_name "brandName",
          q.manufacturer as manufacturer
        from
          brand b
          left join (
            select
              m.id "_id",
              m.name as manufacturer
            from
              manufacturer m
          ) q on b.manufacturer_id = q."_id"
        where (
          b.drug_id = d.id
        )
      ) q
    ) as brands,
    (
      select
        coalesce(jsonb_agg(jsonb_build_object(
          'advisoryText', q."advisoryText",
          'advisoryType', q."advisoryType",
          'exprYieldingTwo', q."exprYieldingTwo",
          'authorityName', q."authorityName",
          'authorityUrl', q."authorityUrl",
          'authorityDescription', q."authorityDescription"
        )),'[]'::jsonb) json
      from (
        select
          a.text "advisoryText",
          q."advisoryType" "advisoryType",
          q."exprYieldingTwo" "exprYieldingTwo",
          q."authorityName" "authorityName",
          q."authorityUrl" "authorityUrl",
          q."authorityDescription" "authorityDescription"
        from
          advisory a
          left join (
            select
              at.id "_id",
              at.name "advisoryType",
              (1 + 1) "exprYieldingTwo",
              q."authorityName" "authorityName",
              q."authorityUrl" "authorityUrl",
              q."authorityDescription" "authorityDescription"
            from
              advisory_type at
              left join (
                select
                  a.id "_id",
                  a.name "authorityName",
                  a.url "authorityUrl",
                  a.description "authorityDescription"
                from
                  authority a
              ) q on at.authority_id = q."_id"
          ) q on a.advisory_type_id = q."_id"
        where (
          a.drug_id = d.id
        )
      ) q
    ) as advisories,
    (
      select
        coalesce(jsonb_agg(jsonb_build_object(
          'categoryName', q."categoryName",
          'description', q.description,
          'authorityName', q."authorityName",
          'authorityUrl', q."authorityUrl",
          'authorityDescription', q."authorityDescription"
        )),'[]'::jsonb) json
      from (
        select
          q."categoryName" "categoryName",
          q.description as description,
          q1."authorityName" "authorityName",
          q1."authorityUrl" "authorityUrl",
          q1."authorityDescription" "authorityDescription"
        from
          drug_functional_category dfc
          left join (
            select
              fc.id "_id",
              fc.name "categoryName",
              fc.description as description
            from
              functional_category fc
          ) q on dfc.functional_category_id = q."_id"
          left join (
            select
              a.id "_id",
              a.name "authorityName",
              a.url "authorityUrl",
              a.description "authorityDescription"
            from
              authority a
          ) q1 on dfc.authority_id = q1."_id"
        where (
          dfc.drug_id = d.id
        )
      ) q
    ) "functionalCategories",
    (
      select
        jsonb_build_object(
          'id', q.id,
          'shortName', q."shortName"
        ) json
      from (
        select
          a.id as id,
          a.short_name "shortName"
        from
          analyst a
        where (
          d.registered_by = a.id
        )
      ) q
    ) "registeredByAnalyst",
    (
      select
        jsonb_build_object(
          'displayName', q."displayName",
          'nctrIsisId', q."nctrIsisId",
          'cas', q.cas,
          'entered', q.entered,
          'enteredByAnalyst', q."enteredByAnalyst"
        ) json
      from (
        select
          c.display_name "displayName",
          c.nctr_isis_id "nctrIsisId",
          c.cas as cas,
          c.entered as entered,
          (
            select
              jsonb_build_object(
                'id', q.id,
                'shortName', q."shortName"
              ) json
            from (
              select
                a.id as id,
                a.short_name "shortName"
              from
                analyst a
              where (
                c.entered_by = a.id
              )
            ) q
          ) "enteredByAnalyst"
        from
          compound c
        where (
          d.compound_id = c.id
        )
      ) q
    ) as compound
  from
    drug d
  where (
    d.mesh_id IN (:drugMeshIdList)
    and
    (not d.id = 2)
  )
) q
