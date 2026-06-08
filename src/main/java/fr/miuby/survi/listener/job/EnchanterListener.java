package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.Map;

/**
 * Gère tous les effets du métier {@link EJob#ENCHANTER} :
 * table d'enchantement, enclume (cap XP + bypass Too Expensive),
 * durabilité accélérée/réduite, réparation XP mending-like.
 *
 * <p>Tous les paramètres numériques sont lus depuis {@link JobsConfig} ({@code jobs.yml}).</p>
 */
public class EnchanterListener implements Listener {

    // ════════════════════════════════════════════════════════════════════════════
    //  Table d'enchantement
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getEnchanter().getUniqueId());
        if (alpha == null) return;
        int jobLevel = alpha.getJobLevel(EJob.ENCHANTER);
        int maxXpCost = jobLevel * 3;
        for (int i = 0; i < event.getOffers().length; i++) {
            EnchantmentOffer offer = event.getOffers()[i];
            if (offer == null) continue;
            if (jobLevel == 0 || offer.getCost() > maxXpCost || offer.getEnchantmentLevel() > jobLevel)
                event.getOffers()[i] = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getEnchanter().getUniqueId());
        if (alpha == null) return;
        int jobLevel = alpha.getJobLevel(EJob.ENCHANTER);
        if (jobLevel == 0) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(Component.text("✗ Vous ne pouvez pas encore enchanter. Progressez dans le métier ")
                    .color(NamedTextColor.RED).append(EJob.ENCHANTER.toComponent())
                    .append(Component.text(".", NamedTextColor.RED)));
            return;
        }
        boolean tooHigh = event.getEnchantsToAdd().entrySet().stream()
                .anyMatch(e -> e.getValue() > jobLevel);
        if (tooHigh) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(Component.text("✗ Cet enchantement dépasse votre niveau de métier ")
                    .color(NamedTextColor.RED).append(EJob.ENCHANTER.toComponent())
                    .append(Component.text(" (max niv." + jobLevel + " d'enchantement).", NamedTextColor.RED)));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Enclume
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int jobLevel = alpha.getJobLevel(EJob.ENCHANTER);
        JobsConfig.EnchanterCfg enc = JobsConfig.getInstance().getEnchanter();
        AnvilInventory anvil = event.getInventory();
        ItemStack first  = anvil.getItem(0);
        ItemStack second = anvil.getItem(1);

        // Résultat nul → niv.10 : reconstruction pour bypasser "Too Expensive"
        if (event.getResult() == null || event.getResult().getType() == Material.AIR) {
            if (enc.getAnvilMaxXpCost()[jobLevel] < 0 && first != null && !first.getType().isAir()) {
                ItemStack rebuilt = constructAnvilResult(first, second, anvil.getRenameText(), jobLevel);
                if (rebuilt != null) { event.setResult(rebuilt); anvil.setRepairCost(39); }
            }
            return;
        }

        if (first == null || first.getType() == Material.AIR) return;

        // Vérification du niveau d'enchantement
        Map<Enchantment, Integer> firstEnchants  = getEnchants(first);
        Map<Enchantment, Integer> resultEnchants = getEnchants(event.getResult());
        boolean changed = false, tooHigh = false;
        for (var entry : resultEnchants.entrySet()) {
            if (entry.getValue() > firstEnchants.getOrDefault(entry.getKey(), 0)) {
                changed = true;
                if (jobLevel == 0 || entry.getValue() > jobLevel) { tooHigh = true; break; }
            }
        }
        if (changed && tooHigh) { event.setResult(null); return; }

        // Vérification du cap XP (-1 = illimité)
        int maxCost = enc.getAnvilMaxXpCost()[jobLevel];
        if (maxCost >= 0 && anvil.getRepairCost() > maxCost) { event.setResult(null); return; }

        // Réinitialisation du RepairCost → plus jamais "Too Expensive"
        ItemStack finalResult = event.getResult().clone();
        if (finalResult.getItemMeta() instanceof Repairable r) {
            r.setRepairCost(0);
            finalResult.setItemMeta(r);
            event.setResult(finalResult);
        }
    }

    private static ItemStack constructAnvilResult(ItemStack base, ItemStack addition,
                                                  String rename, int jobLevel) {
        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return null;
        if (rename != null && !rename.isBlank())
            meta.displayName(Component.text(rename, NamedTextColor.WHITE));
        if (addition != null && !addition.getType().isAir()) {
            for (var entry : getEnchants(addition).entrySet()) {
                int addLvl = entry.getValue();
                if (addLvl > jobLevel) continue;
                int existLvl = meta.getEnchantLevel(entry.getKey());
                int newLvl = (existLvl == addLvl) ? existLvl + 1 : Math.max(existLvl, addLvl);
                if (entry.getKey().canEnchantItem(result)) meta.addEnchant(entry.getKey(), newLvl, true);
            }
            if (addition.getType() == base.getType() && meta instanceof Damageable d && d.getDamage() > 0)
                d.setDamage(Math.max(0, d.getDamage() - base.getType().getMaxDurability() / 2));
        }
        if (meta instanceof Repairable r) r.setRepairCost(0);
        result.setItemMeta(meta);
        return result;
    }

    private static Map<Enchantment, Integer> getEnchants(ItemStack item) {
        if (item == null) return Map.of();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Map.of();
        if (meta instanceof EnchantmentStorageMeta esm) return esm.getStoredEnchants();
        return meta.getEnchants();
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Durabilité
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        double mult = JobsConfig.getInstance().getEnchanter()
                .getDurabilityLossMultiplier()[alpha.getJobLevel(EJob.ENCHANTER)];
        if (mult <= 0) { event.setCancelled(true); return; }
        if (mult == 1.0) return;
        double total = event.getDamage() * mult;
        int damage = (int) total;
        if (JobUtils.RANDOM.nextDouble() < total - damage) damage++;
        if (damage <= 0) { event.setCancelled(true); return; }
        event.setDamage(damage);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Réparation XP — mending-like
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExpGain(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) return;
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.ENCHANTER);
        int repairPerXP = JobsConfig.getInstance().getEnchanter().getRepairPerXp()[level];
        if (repairPerXP <= 0) return;
        ItemStack target = findMostDamagedItem(event.getPlayer());
        if (target == null) return;
        if (!(target.getItemMeta() instanceof Damageable dmg) || dmg.getDamage() == 0) return;
        int curDmg = dmg.getDamage();
        int xpUsed = Math.min(event.getAmount(), (int) Math.ceil((double) curDmg / repairPerXP));
        dmg.setDamage(Math.max(0, curDmg - xpUsed * repairPerXP));
        target.setItemMeta(dmg);
        event.setAmount(Math.max(0, event.getAmount() - xpUsed));
    }

    private static ItemStack findMostDamagedItem(Player player) {
        ItemStack best = null; int maxDmg = 0;
        ItemStack[] candidates = {
                player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand(),
                player.getInventory().getHelmet(), player.getInventory().getChestplate(),
                player.getInventory().getLeggings(), player.getInventory().getBoots()
        };
        for (ItemStack item : candidates) {
            if (item == null || item.getType().isAir()) continue;
            if (!(item.getItemMeta() instanceof Damageable d)) continue;
            if (d.getDamage() > maxDmg) { maxDmg = d.getDamage(); best = item; }
        }
        return best;
    }
}
