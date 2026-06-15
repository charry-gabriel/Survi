package fr.miuby.survi.display;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import com.mojang.authlib.properties.Property;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Construit les faux joueurs de la colonne d'info (droite).
 * Identique à l'original, seule la signature de {@link #entry} change :
 * elle prend désormais une {@link Property} (avec signature Mojang) au lieu d'un {@code String} base64.
 */
class TabInfoColumn {

    static final int HEIGHT        = 20;
    static final int MAX_FAKE      = 40;
    static final int MAX_VILLAGERS = HEIGHT - 16;

    static final List<String> PROFILE_NAMES;
    static final List<UUID>   FAKE_UUIDS;

    static {
        PROFILE_NAMES = new ArrayList<>(MAX_FAKE);
        FAKE_UUIDS    = new ArrayList<>(MAX_FAKE);
        for (int i = 0; i < MAX_FAKE; i++) {
            PROFILE_NAMES.add("s" + String.format("%02d", i));
            FAKE_UUIDS.add(new UUID(0xCAFEBABE00000000L, i));
        }
    }

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    /**
     * Valeurs vanilla Minecraft pour chaque attribut affiché dans les stats.
     * On ne peut pas se fier à {@code AttributeInstance.getDefaultValue()} : Paper 26.1
     * y renvoie une valeur interne qui diffère du vanilla (ex. MOVEMENT_SPEED → ~0.35
     * au lieu de 0.1), ce qui fausserait tous les pourcentages.
     */
    private static final Map<Attribute, Double> VANILLA_DEFAULTS = Map.of(
            Attribute.MAX_HEALTH,     20.0,
            Attribute.ATTACK_DAMAGE,   2.0,
            Attribute.ARMOR,           0.0,
            Attribute.MOVEMENT_SPEED,  0.1
    );

    private TabInfoColumn() {}

    static List<ClientboundPlayerInfoUpdatePacket.Entry> build(AlphaPlayer ap, int playerCount) {
        int padding    = Math.max(0, 20 - playerCount);
        int totalSlots = padding + HEIGHT;
        List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>(totalSlots);
        List<VillagerLevel> villagers = getVillagerList();
        int slot = 0;

        // ── Padding colonne gauche ───────────────────────────────────────────
        for (int i = 0; i < padding; i++) {
            entries.add(entry(slot++, Component.empty(), TabSkins.gray()));
        }

        // ── Section Villageois ───────────────────────────────────────────────
        entries.add(entry(slot++, section("Villageois"), TabSkins.blue()));

        int maxV = Math.min(villagers.size(), MAX_VILLAGERS);
        for (int i = 0; i < maxV; i++) {
            entries.add(entry(slot++, villagerDisplay(villagers.get(i)), TabSkins.gray()));
        }

        // ── Section Métiers ──────────────────────────────────────────────────
        entries.add(entry(slot++, Component.empty(),  TabSkins.gray()));
        entries.add(entry(slot++, section("Métiers"), TabSkins.blue()));

        for (EJob job : sortedJobs(ap)) {
            Property skin = TabSkins.JOB_PROPS.getOrDefault(job, TabSkins.gray());
            entries.add(entry(slot++, jobDisplay(ap, job), skin));
        }

        // ── Section Stats ────────────────────────────────────────────────────
        entries.add(entry(slot++, Component.empty(), TabSkins.gray()));
        entries.add(entry(slot++, section("Stats"),  TabSkins.blue()));
        entries.add(entry(slot++, formatCombinedDamageStat(ap),                                   TabSkins.gray()));
        entries.add(entry(slot++, formatBlessingModifier("Résistance", ap.getResistanceModifier()), TabSkins.gray()));
        entries.add(entry(slot++, formatStat(ap, Attribute.MAX_HEALTH,     "Vie"),                 TabSkins.gray()));
        entries.add(entry(slot++, formatStat(ap, Attribute.MOVEMENT_SPEED, "Vitesse"),             TabSkins.gray()));

        // ── Padding bas de contenu ───────────────────────────────────────────
        while (slot < totalSlots) {
            entries.add(entry(slot++, Component.empty(), TabSkins.gray()));
        }

        return entries;
    }

    // ── Constructeur d'entrée NMS ────────────────────────────────────────────

    /**
     * @param skinProp Property signée (issue de {@link TabSkins#gray()}, etc.).
     *                 Si {@code null} (skins pas encore chargés au démarrage),
     *                 {@link TabSkins#createProfile} retourne un profil sans texture.
     */
    private static ClientboundPlayerInfoUpdatePacket.Entry entry(int slot, Component display, Property skinProp) {
        UUID uuid = FAKE_UUIDS.get(slot);
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid,
                TabSkins.createProfile(uuid, PROFILE_NAMES.get(slot), skinProp),
                true,
                0,
                GameType.SURVIVAL,
                PaperAdventure.asVanilla(display),
                false,
                0,
                null
        );
    }

    // ── Affichage — structure ────────────────────────────────────────────────

    private static Component section(String name) {
        return Component.text("─── " + name + " ───", NamedTextColor.DARK_AQUA);
    }

    private static Component villagerDisplay(VillagerLevel vl) {
        return vl.getDisplayName().append(Component.text(": niv." + vl.getLevel(), NamedTextColor.WHITE));
    }

    private static Component jobDisplay(AlphaPlayer ap, EJob job) {
        int level = ap.getJobLevel(job);
        return job.toComponent()
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(Component.text("niv." + level, NamedTextColor.WHITE));
    }

    // ── Affichage — stats ────────────────────────────────────────────────────

    /**
     * Affiche la stat d'un attribut en pourcentage de la valeur vanilla Minecraft,
     * en ne comptant que les modificateurs apportés par notre plugin (rôles, métiers…).
     * Les modificateurs d'équipement (épée, armure portée…) sont ignorés.
     *
     * <ul>
     *   <li>Attribut à base non nulle (Vie, Force, Vitesse…) → pourcentage (100 % = vanilla).</li>
     *   <li>Attribut à base nulle avec ADD_NUMBER (Chance…) → valeur absolue (+1.0).</li>
     *   <li>Attribut à base nulle avec ADD_SCALAR uniquement (Armure…) → multiplicateur de rôle (+20%).</li>
     * </ul>
     */
    private static Component formatStat(AlphaPlayer ap, Attribute attributeType, String statName) {
        Player player = ap.getPlayer();
        if (player == null) return Component.empty();

        AttributeInstance attr = player.getAttribute(attributeType);
        if (attr == null) return Component.empty();

        String ns = GameManager.getInstance().getPlugin().getName().toLowerCase();

        // Reconstruit la valeur effective en ne comptant que les modificateurs de notre plugin.
        // Formule Minecraft : step2 = base + Σ ADD_NUMBER ; roleValue = step2 × (1 + Σ ADD_SCALAR) × Π (1 + MULTIPLY_SCALAR_1)
        double base = attr.getBaseValue();
        double addNum = 0, addScalar = 0, multiplyTotal = 1.0;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (!mod.getKey().getNamespace().equalsIgnoreCase(ns)) continue;
            switch (mod.getOperation()) {
                case ADD_NUMBER        -> addNum        += mod.getAmount();
                case ADD_SCALAR        -> addScalar     += mod.getAmount();
                case MULTIPLY_SCALAR_1 -> multiplyTotal *= 1.0 + mod.getAmount();
            }
        }
        double step2     = base + addNum;
        double roleValue = step2 * (1.0 + addScalar) * multiplyTotal;

        double vanilla = VANILLA_DEFAULTS.getOrDefault(attributeType, attr.getDefaultValue());

        // ── Attribut à base nulle (Armure, Chance…) ──────────────────────────────
        if (vanilla < 0.001) {
            if (Math.abs(roleValue) > 0.001) {
                NamedTextColor c = roleValue > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                String sign = roleValue > 0 ? "+" : "";
                return Component.text(statName + ": ", NamedTextColor.WHITE)
                        .append(Component.text(sign + DF.format(roleValue), c));
            }
            if (Math.abs(addScalar) < 0.001) {
                return Component.text(statName + ": ", NamedTextColor.WHITE)
                        .append(Component.text("—", NamedTextColor.DARK_GRAY));
            }
            long scalarPct = Math.round(addScalar * 100.0);
            NamedTextColor c = addScalar > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
            String sign = addScalar > 0 ? "+" : "";
            return Component.text(statName + ": ", NamedTextColor.WHITE)
                    .append(Component.text(sign + scalarPct + "%", c));
        }

        // ── Attribut à base non nulle : pourcentage de la valeur vanilla ─────────
        double pct = roleValue / vanilla * 100.0;
        long pctRounded = Math.round(pct);
        NamedTextColor color = Math.abs(pct - 100.0) < 0.5 ? NamedTextColor.GRAY
                : pct > 100.0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(pctRounded + "%", color));
    }

    /**
     * Affiche les dégâts effectifs du joueur : combinaison de l'attribut ATTACK_DAMAGE
     * (bonus de rôle/métier) et du {@code damageModifier} (bénédictions/malédictions).
     * 100 % = dégâts vanilla sans aucun modificateur de part et d'autre.
     * Vert au-dessus de 100 %, rouge en-dessous, gris à 100 %.
     */
    private static Component formatCombinedDamageStat(AlphaPlayer ap) {
        Player player = ap.getPlayer();
        if (player == null) return Component.empty();

        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr == null) return Component.empty();

        String ns = GameManager.getInstance().getPlugin().getName().toLowerCase();

        double base = attr.getBaseValue();
        double addNum = 0, addScalar = 0, multiplyTotal = 1.0;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (!mod.getKey().getNamespace().equalsIgnoreCase(ns)) continue;
            switch (mod.getOperation()) {
                case ADD_NUMBER        -> addNum        += mod.getAmount();
                case ADD_SCALAR        -> addScalar     += mod.getAmount();
                case MULTIPLY_SCALAR_1 -> multiplyTotal *= 1.0 + mod.getAmount();
            }
        }
        double step2     = base + addNum;
        double roleValue = step2 * (1.0 + addScalar) * multiplyTotal;

        double vanilla    = VANILLA_DEFAULTS.getOrDefault(Attribute.ATTACK_DAMAGE, attr.getDefaultValue());
        double pct        = (roleValue / vanilla) * ap.getDamageModifier() * 100.0;
        long   pctRounded = Math.round(pct);

        NamedTextColor color = Math.abs(pct - 100.0) < 0.5 ? NamedTextColor.GRAY
                : pct > 100.0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text("Dégâts: ", NamedTextColor.WHITE)
                .append(Component.text(pctRounded + "%", color));
    }

    /**
     * Affiche un modifier de blessing en pourcentage absolu (1.0 = 100 %).
     * Vert au-dessus de 100 %, rouge en-dessous, gris exactement à 100 %.
     */
    private static Component formatBlessingModifier(String statName, float current) {
        long displayPct = Math.round((double) current * 100.0);
        NamedTextColor color = Math.abs(current - 1.0f) < 0.005f ? NamedTextColor.GRAY
                : current > 1.0f ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(displayPct + "%", color));
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private static List<VillagerLevel> getVillagerList() {
        List<VillagerLevel> list = new ArrayList<>();
        for (var v : VillagerRegistry.getAll()) {
            if (v instanceof VillagerLevel vl) list.add(vl);
        }
        return list;
    }

    private static EJob[] sortedJobs(AlphaPlayer ap) {
        return Arrays.stream(EJob.values())
                .sorted((a, b) -> {
                    int d = Integer.compare(ap.getJobLevel(b), ap.getJobLevel(a));
                    return d != 0 ? d : Integer.compare(ap.getJobReputation(b), ap.getJobReputation(a));
                })
                .toArray(EJob[]::new);
    }
}