package me.zeroeightsix.namefinder.generator;

/**
 * Created by 086 on 3/09/2017.
 */
public interface NameGenerator {
    public boolean isInvalid();
    public long possibilities();
    public String map(int length, long index);
    public long getPossibilities(int length);
}
