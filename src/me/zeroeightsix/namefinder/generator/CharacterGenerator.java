package me.zeroeightsix.namefinder.generator;

import me.zeroeightsix.namefinder.Namefinder;

/**
 * Created by 086 on 3/09/2017.
 */
public class CharacterGenerator implements NameGenerator {

    boolean invalid = false;

    @Override
    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    char[] characterSet;

    public CharacterGenerator(char[] characterSet) {
        if (characterSet == null || characterSet.length==0)
            setInvalid(true);
        this.characterSet = characterSet;
    }

    public String map(int length, long index) {
        StringBuilder builder = new StringBuilder();
        int rem;
        for (int i = 0; i < length; i++) { // Local index
            rem = (int) (index % characterSet.length);
            char c = characterSet[rem];
            builder.append(c);
            index /= characterSet.length;
        }
        return builder.reverse().toString();
    }

    @Override
    public long possibilities() {
        long pos = 0;
        for (int i = Namefinder.INSTANCE.minFactory.getValue(); i <= Namefinder.INSTANCE.maxFactory.getValue(); i++)
            pos += Math.pow(characterSet.length, i);
        return pos;
    }

    @Override
    public long getPossibilities(int length) {
        return (long) Math.pow(characterSet.length, length);
    }
}
