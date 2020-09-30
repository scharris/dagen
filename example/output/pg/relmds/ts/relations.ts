// ---------------------------------------------------------------------------
// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
// ---------------------------------------------------------------------------
export const drugs = {
   "advisory": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "drug_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": null},
      "advisory_type_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": null},
      "text": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": false, "pkPart": null},
   },
   "advisory_type": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "name": {"type": "varchar", "len": 50, "prec":null, "scale": null, "null": false, "pkPart": null},
      "authority_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": null},
   },
   "analyst": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "short_name": {"type": "varchar", "len": 50, "prec":null, "scale": null, "null": false, "pkPart": null},
   },
   "authority": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "name": {"type": "varchar", "len": 200, "prec":null, "scale": null, "null": false, "pkPart": null},
      "url": {"type": "varchar", "len": 500, "prec":null, "scale": null, "null": true, "pkPart": null},
      "description": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": true, "pkPart": null},
      "weight": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": true, "pkPart": null},
   },
   "brand": {
      "drug_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "brand_name": {"type": "varchar", "len": 200, "prec":null, "scale": null, "null": false, "pkPart": 2},
      "language_code": {"type": "varchar", "len": 10, "prec":null, "scale": null, "null": true, "pkPart": null},
      "manufacturer_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": true, "pkPart": null},
   },
   "compound": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "display_name": {"type": "varchar", "len": 50, "prec":null, "scale": null, "null": true, "pkPart": null},
      "nctr_isis_id": {"type": "varchar", "len": 100, "prec":null, "scale": null, "null": true, "pkPart": null},
      "smiles": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": true, "pkPart": null},
      "canonical_smiles": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": true, "pkPart": null},
      "cas": {"type": "varchar", "len": 50, "prec":null, "scale": null, "null": true, "pkPart": null},
      "mol_formula": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": true, "pkPart": null},
      "mol_weight": {"type": "numeric", "len": null, "prec":131089, "scale": 0, "null": true, "pkPart": null},
      "entered": {"type": "timestamptz", "len": null, "prec":null, "scale": null, "null": true, "pkPart": null},
      "entered_by": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": null},
   },
   "drug": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "name": {"type": "varchar", "len": 500, "prec":null, "scale": null, "null": false, "pkPart": null},
      "compound_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": null},
      "mesh_id": {"type": "varchar", "len": 7, "prec":null, "scale": null, "null": true, "pkPart": null},
      "drugbank_id": {"type": "varchar", "len": 7, "prec":null, "scale": null, "null": true, "pkPart": null},
      "cid": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": true, "pkPart": null},
      "therapeutic_indications": {"type": "varchar", "len": 4000, "prec":null, "scale": null, "null": true, "pkPart": null},
      "registered": {"type": "timestamptz", "len": null, "prec":null, "scale": null, "null": true, "pkPart": null},
      "registered_by": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": null},
      "market_entry_date": {"type": "date", "len": null, "prec":null, "scale": null, "null": true, "pkPart": null},
   },
   "drug_functional_category": {
      "drug_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "functional_category_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 2},
      "authority_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 3},
      "seq": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": true, "pkPart": null},
   },
   "drug_reference": {
      "drug_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "reference_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 2},
      "priority": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": true, "pkPart": null},
   },
   "functional_category": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "name": {"type": "varchar", "len": 500, "prec":null, "scale": null, "null": false, "pkPart": null},
      "description": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": true, "pkPart": null},
      "parent_functional_category_id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": true, "pkPart": null},
   },
   "manufacturer": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "name": {"type": "varchar", "len": 200, "prec":null, "scale": null, "null": false, "pkPart": null},
   },
   "reference": {
      "id": {"type": "int4", "len": null, "prec":10, "scale": 0, "null": false, "pkPart": 1},
      "publication": {"type": "varchar", "len": 2000, "prec":null, "scale": null, "null": false, "pkPart": null},
   },
};
