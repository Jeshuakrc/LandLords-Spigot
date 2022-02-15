package mc.thejsuser.landlords.totemElements;

public interface TotemRelative {

    int[] getPosition();
    default int[] getAbsolutePosition(int x, int y, int z) {
        int[]   pos = getPosition(),
                add = {x,y,z},
                r = new int[3];
        for (int i = 0; i < 3; i++) {
            r[i] = pos[i] + add[i];
        }
        return r;
    }
    TotemStructure getStructure();
}
