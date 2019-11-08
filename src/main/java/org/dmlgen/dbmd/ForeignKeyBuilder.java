package org.dmlgen.dbmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.dmlgen.util.StringFuns.matches;


class ForeignKeyBuilder
{

    private RelId srcRel;
    private RelId tgtRel;
    private List<ForeignKey.Component> comps;

    public ForeignKeyBuilder(RelId srcRel, RelId tgtRel)
    {
        this.srcRel = srcRel;
        this.tgtRel = tgtRel;
        this.comps = new ArrayList<>();
    }

    boolean neitherRelMatches(Optional<Pattern> relIdsPattern)
    {
        return !(matches(relIdsPattern, srcRel.getIdString()) || matches(relIdsPattern, tgtRel.getIdString()));
    }

    ForeignKey build() { return new ForeignKey(srcRel, tgtRel, comps); }

    void addComponent(ForeignKey.Component comp) { comps.add(comp); }
}
