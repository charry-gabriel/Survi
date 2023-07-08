package fr.miuby.survi.blessing;

public class Blessing {
    private final BlessingEffect[] blessingEffects;

    public Blessing(BlessingEffect... blessingEffects) {
        this.blessingEffects = blessingEffects;
    }

    public BlessingEffect[] getBlessingEffects() {
        return blessingEffects;
    }
}
