package fr.miuby.survi.display;

import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import com.mojang.authlib.properties.Property;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Construit les faux joueurs de la colonne d'info (droite).
 * Identique à l'original, seule la signature de {@link #entry} change :
 * elle prend désormais une {@link Property} (avec signature Mojang) au lieu d'un {@code String} base64.
 */
class TabInfoColumn {

    static final int HEIGHT      = 20;
    static final int MAX_FAKE    = 40;
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
        entries.add(entry(slot++, section("Villageois"),   TabSkins.blue()));

        int maxV = Math.min(villagers.size(), MAX_VILLAGERS);
        for (int i = 0; i < maxV; i++) {
            entries.add(entry(slot++, villagerDisplay(villagers.get(i)), TabSkins.gray()));
        }

        // ── Section Métiers ──────────────────────────────────────────────────
        entries.add(entry(slot++, Component.empty(),    TabSkins.gray()));
        entries.add(entry(slot++, section("Métiers"),   TabSkins.blue()));

        for (EJob job : sortedJobs(ap)) {
            Property skin = TabSkins.JOB_PROPS.getOrDefault(job, TabSkins.gray());
            entries.add(entry(slot++, jobDisplay(ap, job), skin));
        }

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
    private static ClientboundPlayerInfoUpdatePacket.Entry entry(
            int slot, Component display, Property skinProp) {
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

    // ── Affichage ────────────────────────────────────────────────────────────

    private static Component section(String name) {
        return Component.text("─── " + name + " ───", NamedTextColor.DARK_AQUA);
    }

    private static Component villagerDisplay(VillagerLevel vl) {
        return vl.getDisplayName().append(Component.text(": niv." + vl.getLevel(), NamedTextColor.WHITE));
    }

    private static Component jobDisplay(AlphaPlayer ap, EJob job) {
        int level = ap.getJobLevel(job);
        int rep   = ap.getJobReputation(job);
        return job.toComponent()
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(Component.text("niv." + level, NamedTextColor.WHITE))
                .append(Component.text(" (" + rep + " rép.)", NamedTextColor.WHITE));
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