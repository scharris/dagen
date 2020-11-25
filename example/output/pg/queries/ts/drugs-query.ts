// ---------------------------------------------------------------------------
// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
// ---------------------------------------------------------------------------
// Can add more imports or other items via via "--types-file-header" option here.


export const sqlResource = "drugs query(json object rows).sql";


export interface Drug
{
   name: string;
   meshId: string | null;
   cid: number | null;
   registered: string | null;
   marketEntryDate: string | null;
   therapeuticIndications: string | null;
   cidPlus1000: number | null;
   references: DrugReference[];
   brands: Brand[];
   advisories: Advisory[];
   functionalCategories: DrugFunctionalCategory[];
   registeredByAnalyst: Analyst;
   compound: Compound;
}

export interface Analyst
{
   id: number;
   shortName: string;
}

export interface Compound
{
   displayName: string | null;
   nctrIsisId: string | null;
   cas: string | null;
   entered: string | null;
   enteredByAnalyst: Analyst;
}

export interface DrugReference
{
   publication: string;
}

export interface Brand
{
   brandName: string;
   manufacturer: string | null;
}

export interface Advisory
{
   advisoryText: string;
   advisoryType: string;
   authorityName: string;
   authorityUrl: string | null;
   authorityDescription: string | null;
   exprYieldingTwo: number;
}

export interface DrugFunctionalCategory
{
   categoryName: string;
   description: string | null;
   authorityName: string;
   authorityUrl: string | null;
   authorityDescription: string | null;
}
