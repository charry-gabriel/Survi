package fr.miuby.survi.player;

import fr.miuby.lib.player.MLPlayer;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MLWorld;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.attribute.Attribute;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import fr.miuby.survi.quest.PlayerQuestData;
import java.util.*;

public class AlphaPlayer extends MLPlayer implements Serializable {
    @Getter
    private final AlphaScoreboard scoreboard;
    @Getter
    private final AlphaLife alphaLife;

    @Getter
    private final List<Role> subRoles = new ArrayList<>();

    @Getter
    private final Map<String, Integer> reputationByTrader = new HashMap<>();
    @Getter
    @Setter
    private PlayerQuestData activeQuest;

    @Setter
    private MLWorld world;
    @Getter
    private int mort = 0;
    @Getter
    private int success = 0;

    @Setter
    @Getter
    private boolean isTakingNoDamage;
    @Setter
    @Getter
    private Role role;

    @Getter
    private final Map<Attribute, Double> baseAttributes = new HashMap<>();

    //region Modifier
    @Getter
    @Setter
    private float resistanceModifier = 0.2f;
    @Setter
    @Getter
    private float damageModifier = 0.2f;
    @Getter
    private final float endResistanceModifier = 0.7f;
    @Getter
    private final float endDamageModifier = 0.7f;
    //endregion

    public AlphaPlayer(UUID uuid) {
        super(uuid);
        this.scoreboard = new AlphaScoreboard();
        this.alphaLife = new AlphaLife(this);
    }

    public static AlphaPlayer get(UUID uuid) {
        return GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
    }

    @Override
    public void onJoinServer() {
        this.player.setScoreboard(this.scoreboard.getScoreboard());

        this.world = WorldRegistry.get(getPlayer().getWorld().getUID());
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(this);

        GameManager.getInstance().getVillagerFactory().applyAllCurrentBlessing(this);
        GameManager.getInstance().getAlphaPlayerFactory().setPlayersToTeam(this.scoreboard);

        this.getAlphaLife().actualizeDeath();
        this.getAlphaLife().actualizeSuccess();

        this.player.discoverRecipes(GameManager.getInstance().getCustomRecipeFactory().getNewRecipes().keySet());

        // Load reputation and quest data
        this.reputationByTrader.putAll(GameManager.getInstance().getDatabase().quests().getReputation(this.getUuid()));
        this.activeQuest = GameManager.getInstance().getDatabase().quests().getPlayerQuest(this.getUuid());
    }

    public void gainOneSuccess(boolean challenge) {
        if(challenge) {
            this.success++;
            this.addSuccess(this.success);
        }
    }

    public void addMort(int mort) {
        this.mort += mort;
        this.alphaLife.setDeath(this.mort);
        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateMort(this);
    }

    public void addSuccess(int success) {
        this.success = success;
        this.getAlphaLife().regenHealth(() -> this.getAlphaLife().setSuccess(success));
        GameManager.getInstance().getAlphaPlayerFactory().getPersistenceService().updateSuccess(this);
    }

    public void teleport(MLWorld monde) {
        if (getPlayer() != null)
            getPlayer().teleport(monde.getSpawnPoint());
    }

    //region Getters Setters
    public EWorld getWorld() {
        return (EWorld) this.world.getType();
    }

    public void resetPlayer(){
        this.player = null;
    }

    public void setMort(int mort) {
        this.mort = mort;
        this.alphaLife.setDeath(mort);
    }

    public void setSuccess(int success) {
        this.success = success;
        this.alphaLife.setSuccess(success);
    }

    public void addSubRole(Role role) {
        Role removeRole = null;
        for (Role subRole : this.subRoles) {
            if (subRole.roleId().equals(role.roleId()))
                removeRole = subRole;
        }
        this.subRoles.remove(removeRole);
        this.subRoles.add(role);
    }

    public void removeSubRole(Role role) {
        this.subRoles.remove(role);
    }

    public int getReputation(String traderId) {
        return reputationByTrader.getOrDefault(traderId, 0);
    }

    public void addReputation(String traderId, int amount) {
        int newRep = getReputation(traderId) + amount;
        reputationByTrader.put(traderId, newRep);
        GameManager.getInstance().getDatabase().quests().updateReputation(this.getUuid(), traderId, newRep);
    }
    //endregion
}