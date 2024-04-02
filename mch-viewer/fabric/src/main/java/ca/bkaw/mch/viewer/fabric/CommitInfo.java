package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.commit.Commit;

public record CommitInfo(Commit commit, Sha1 hash) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitInfo that = (CommitInfo) o;

        return this.hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return this.hash.hashCode();
    }
}
