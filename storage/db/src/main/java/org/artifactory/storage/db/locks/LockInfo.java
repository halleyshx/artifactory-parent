package org.artifactory.storage.db.locks;

/**
 * @author gidis
 */
public class LockInfo {
    private String category;
    private String key;
    private String owner;
    private long threadId;
    private long startedTime;
    private String threadName;

    public LockInfo(String category, String key, String owner, long threadId, String threadName, long startedTime) {
        this.category = category;
        this.key = key;
        this.owner = owner;
        this.threadId = threadId;
        this.threadName = threadName;
        this.startedTime = startedTime;
    }

    public String getCategory() {
        return category;
    }

    public String getKey() {
        return key;
    }

    public String getOwner() {
        return owner;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(long startedTime) {
        this.startedTime = startedTime;
    }

    public String getThreadName() {
        return threadName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LockInfo lockInfo = (LockInfo) o;

        if (threadId != lockInfo.threadId) {
            return false;
        }
        if (startedTime != lockInfo.startedTime) {
            return false;
        }
        if (category != null ? !category.equals(lockInfo.category) : lockInfo.category != null) {
            return false;
        }
        if (key != null ? !key.equals(lockInfo.key) : lockInfo.key != null) {
            return false;
        }
        if (owner != null ? !owner.equals(lockInfo.owner) : lockInfo.owner != null) {
            return false;
        }
        return threadName != null ? threadName.equals(lockInfo.threadName) : lockInfo.threadName == null;
    }

    @Override
    public int hashCode() {
        int result = category != null ? category.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (int) (threadId ^ (threadId >>> 32));
        result = 31 * result + (int) (startedTime ^ (startedTime >>> 32));
        result = 31 * result + (threadName != null ? threadName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LockInfo{" +
                "category='" + category + '\'' +
                ", key='" + key + '\'' +
                ", owner='" + owner + '\'' +
                ", threadId=" + threadId +
                ", startedTime=" + startedTime +
                ", threadName='" + threadName + '\'' +
                '}';
    }
}
