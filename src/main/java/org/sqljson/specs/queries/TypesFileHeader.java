package org.sqljson.specs.queries;

import org.sqljson.TypesLanguage;


public class TypesFileHeader
{
    private final TypesLanguage language;
    private final String text;

    private TypesFileHeader()
    {
        language = TypesLanguage.Java;
        text = "";
    }

    public TypesFileHeader(TypesLanguage language, String text)
    {
        this.language = language;
        this.text = text;
    }

    public TypesLanguage getLanguage() { return language; }

    public String getText() { return text; }
}
