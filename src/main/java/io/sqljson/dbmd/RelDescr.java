package io.sqljson.dbmd;

import java.util.Optional;
import static java.util.Objects.requireNonNull;


public class RelDescr {

    private RelId relId;

    private RelMetadata.RelType relType;

    private Optional<String> relComment;

    public RelDescr(RelId relId, RelMetadata.RelType relType, Optional<String> relComment)
    {
        this.relId = requireNonNull(relId);
        this.relType = requireNonNull(relType);
        this.relComment = requireNonNull(relComment);
    }

    protected RelDescr() {}

    public RelId getRelationId() { return relId; }

    public RelMetadata.RelType getRelationType() { return relType; }

    public Optional<String> getRelationComment() { return relComment; }

}