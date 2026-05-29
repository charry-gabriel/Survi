package fr.miuby.survi.blessing;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;

public class ReputationEffect extends BlessingEffect {
    private final EJob job;
    private final int reputation;

    public ReputationEffect(EJob job, int reputation) {
        this.job = job;
        this.reputation = reputation;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.addJobReputation(job, reputation);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        player.addJobReputation(job, -reputation);
    }
}