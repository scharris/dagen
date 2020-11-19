/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.26.723 on 2020-11-07 13:41:34.

export interface ChildCollectionSpec {
    collectionName: string;
    tableJson: TableJsonSpec;
    foreignKeyFields?: Nullable<string[]>;
    customJoinCondition?: Nullable<CustomJoinCondition>;
    filter?: Nullable<string>;
    unwrap?: Nullable<boolean>;
    orderBy?: Nullable<string>;
}

export interface CustomJoinCondition {
    equatedFields: FieldPair[];
}

export interface FieldPair {
    childField: string;
    parentPrimaryKeyField: string;
}

export interface InlineParentSpec extends ParentSpec {
}

export interface ParentSpec {
    customJoinCondition?: Nullable<CustomJoinCondition>;
    tableJson: TableJsonSpec;
    viaForeignKeyFields?: Nullable<string[]>;
}

export interface QueryGroupSpec {
    defaultSchema?: Nullable<string>;
    outputFieldNameDefault: OutputFieldNameDefault;
    generateUnqualifiedNamesForSchemas: string[];
    querySpecs: QuerySpec[];
}

export interface QuerySpec {
    queryName: string;
    tableJson: TableJsonSpec;
    resultRepresentations?: Nullable<ResultRepr[]>;
    generateResultTypes?: Nullable<boolean>;
    generateSource?: Nullable<boolean>;
    outputFieldNameDefault?: Nullable<OutputFieldNameDefault>;
    orderBy?: Nullable<string>;
    forUpdate?: Nullable<boolean>;
    typesFileHeader?: Nullable<string>;
}

export interface RecordCondition {
    sql: string;
    paramNames?: Nullable<string[]>;
    withTableAliasAs?: Nullable<string>;
}

export interface ReferencedParentSpec extends ParentSpec {
    referenceName: string;
}

export interface TableFieldExpr {
    field?: Nullable<string>;
    expression?: Nullable<string>;
    withTableAliasAs?: Nullable<string>;
    jsonProperty?: Nullable<string>;
    generatedFieldType?: Nullable<string>;
}

export interface TableJsonSpec {
    table: string;
    fieldExpressions?: Nullable<TableFieldExpr[]>;
    inlineParentTables?: Nullable<InlineParentSpec[]>;
    referencedParentTables?: Nullable<ReferencedParentSpec[]>;
    childTableCollections?: Nullable<ChildCollectionSpec[]>;
    recordCondition?: Nullable<RecordCondition>;
}

export type OutputFieldNameDefault = "AS_IN_DB" | "CAMELCASE";

export type ResultRepr = "MULTI_COLUMN_ROWS" | "JSON_OBJECT_ROWS" | "JSON_ARRAY_ROW";

export type Nullable<T> = T | null | undefined;
