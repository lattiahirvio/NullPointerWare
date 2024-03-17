/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.other.timing;

public final class TimerMS {

    public long time = -1L;

    public boolean hasTimePassed(final long MS) {
        return System.currentTimeMillis() >= time + MS;
    }

    public long hasTimeLeft(final long MS) {
        return (MS + time) - System.currentTimeMillis();
    }
    public long getTimeElapsed() {
        return System.currentTimeMillis() - time;
    }

    public void setTimeElapsed(long time) {
        this.time = System.currentTimeMillis() - time;
    }
    public void reset() {
        time = System.currentTimeMillis();
    }
}
