/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.26.723 on 2020-10-31 20:33:43.

export interface FieldParamCondition {
    field: string;
    op: Operator;
    paramName?: Nullable<string>;
}

export interface RecordCondition {
    sql: string;
    paramNames: string[];
    withTableAliasAs?: Nullable<string>;
}

export interface ModGroupSpec {
    defaultSchema?: Nullable<string>;
    generateUnqualifiedNamesForSchemas: string[];
    modificationStatementSpecs: ModSpec[];
}

export interface ModSpec {
    statementName: string;
    command: ModType;
    table: string;
    tableAlias?: Nullable<string>;
    parametersType: ParametersType;
    generateSourceCode: boolean;
    targetFields: TargetField[];
    fieldParamConditions: FieldParamCondition[];
    recordCondition?: Nullable<RecordCondition>;
}

export interface TargetField {
    field: string;
    value: string;
    paramNames: string[];
}

export interface ChildCollectionSpec {
    collectionName: string;
    tableJson: TableJsonSpec;
    foreignKeyFields?: Nullable<string[]>;
    customJoinCondition?: Nullable<CustomJoinCondition>;
    filter?: Nullable<string>;
    unwrap: boolean;
}

export interface CustomJoinCondition {
    equatedFields: FieldPair[];
}

export interface FieldPair {
    childField: string;
    parentPrimaryKeyField: string;
}

export interface InlineParentSpec extends ParentSpec {
    tableJson: TableJsonSpec;
    viaForeignKeyFields?: Nullable<string[]>;
    customJoinCondition?: Nullable<CustomJoinCondition>;
}

export interface ParentSpec {
    childForeignKeyFieldsSet?: Nullable<string[]>;
    parentTableJsonSpec: TableJsonSpec;
}

export interface QueryGroupSpec {
    defaultSchema?: Nullable<string>;
    outputFieldNameDefault: OutputFieldNameDefault;
    generateUnqualifiedNamesForSchemas: string[];
    querySpecs: QuerySpec[];
}

export interface QuerySpec {
    queryName: string;
    resultsRepresentations: ResultsRepr[];
    generateResultTypes: boolean;
    generateSource: boolean;
    outputFieldNameDefault?: Nullable<OutputFieldNameDefault>;
    tableJson: TableJsonSpec;
    forUpdate: boolean;
    typesFileHeader?: Nullable<string>;
}

export interface ReferencedParentSpec extends ParentSpec {
    referenceName: string;
    tableJson: TableJsonSpec;
    viaForeignKeyFields?: Nullable<string[]>;
    customJoinCondition?: Nullable<CustomJoinCondition>;
}

export interface TableFieldExpr {
    field: string;
    expression: string;
    withTableAliasAs?: Nullable<string>;
    jsonProperty?: Nullable<string>;
    generatedFieldType?: Nullable<string>;
}

export interface TableJsonSpec {
    table: string;
    fieldExpressions: TableFieldExpr[];
    inlineParentTables: InlineParentSpec[];
    referencedParentTables: ReferencedParentSpec[];
    childTableCollections: ChildCollectionSpec[];
    fieldParamConditions: FieldParamCondition[];
    recordCondition?: Nullable<RecordCondition>;
}

export type Operator = "EQ" | "LT" | "LE" | "GT" | "GE" | "IN" | "EQ_IF_PARAM_NONNULL" | "JSON_CONTAINS";

export type ModType = "INSERT" | "UPDATE" | "DELETE";

export type ParametersType = "NUMBERED" | "NAMED";

export type OutputFieldNameDefault = "AS_IN_DB" | "CAMELCASE";

export type ResultsRepr = "MULTI_COLUMN_ROWS" | "JSON_OBJECT_ROWS" | "JSON_ARRAY_ROW";

export type Nullable<T> = T | null | undefined;
