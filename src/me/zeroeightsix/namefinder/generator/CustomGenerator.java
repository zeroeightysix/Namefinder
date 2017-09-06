package me.zeroeightsix.namefinder.generator;

import me.zeroeightsix.namefinder.control.CharacterEditor;

/**
 * Created by 086 on 6/09/2017.
 */
public class CustomGenerator extends CharacterGenerator {
    public CustomGenerator() {
        super(produce());
    }

    private static char[] produce() {
        String s = CharacterEditor.characters.getText();
        if (s.isEmpty()) return null;
        return s.toCharArray();
    }
}
