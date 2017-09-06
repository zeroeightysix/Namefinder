package me.zeroeightsix.namefinder.generator;

import me.zeroeightsix.namefinder.control.CharacterEditor;

import java.util.ArrayList;

/**
 * Created by 086 on 6/09/2017.
 */
public class PresetGenerator extends CharacterGenerator {

    public PresetGenerator() {
        super(produce());
    }

    private static char[] produce() {
        ArrayList<Character> charset = new ArrayList<>();
        if (CharacterEditor.alphabeticCharacters.isSelected())
            for (char c : "abcdefghijklmnopqrstuvwxyz".toCharArray())
                charset.add(c);
        if (CharacterEditor.digits.isSelected())
            for (char c : "0123456789".toCharArray())
                charset.add(c);
        if (CharacterEditor.underscores.isSelected())
            charset.add('_');

        if (charset.isEmpty())
            return null;

        char[] tmp = new char[charset.size()];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i]=charset.get(i);
        }

        return tmp;
    }
}
