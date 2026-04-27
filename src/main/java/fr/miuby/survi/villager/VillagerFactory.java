package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.item.SimpleItemStack;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.QuestDifficulty;
import fr.miuby.survi.villager.blessing.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.item.locked_item.LockedArmorType;
import fr.miuby.survi.item.locked_item.LockedToolType;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import fr.miuby.survi.job.EJob;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VillagerFactory {

    public VillagerFactory() {
        spawnSurvivant();
        spawnNain();
        spawnMaddox();
        spawnThomas();
        spawnFrancois();

        spawnGolDRoger();
        spawnHermanos();
        spawnSpeedBoots();
        spawnIndiana();
        spawnScaleChestplate();
        spawnBarman();
        spawnReceptionniste();
        spawnConcierge();
    }

    //region VillagerLevel
    private void spawnSurvivant(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new MaxHealthEffect(-20)),
                new Blessing(new MaxHealthEffect(-18),new UnlockToolEffect(LockedToolType.WOOD)),
                new Blessing(new MaxHealthEffect(-16),new UnlockArmorEffect(LockedArmorType.LEATHER), new UnlockToolEffect(LockedToolType.STONE)),
                new Blessing(new MaxHealthEffect(-14),new UnlockArmorEffect(LockedArmorType.CHAINMAIL), new UnlockToolEffect(LockedToolType.IRON)),
                new Blessing(new MaxHealthEffect(-12),new UnlockArmorEffect(LockedArmorType.IRON)),
                new Blessing(new MaxHealthEffect(-10),new UnlockArmorEffect(LockedArmorType.GOLD), new UnlockToolEffect(LockedToolType.GOLD)),
                new Blessing(new MaxHealthEffect(-8),new UnlockArmorEffect(LockedArmorType.DIAMOND), new UnlockToolEffect(LockedToolType.DIAMOND), new UnlockArmorEffect(LockedArmorType.NETHERITE)),
                new Blessing(new MaxHealthEffect(-6)),
                new Blessing(new MaxHealthEffect(-4)),
                new Blessing(new MaxHealthEffect(-2)),
                new Blessing(new MaxHealthEffect(0)),
                new Blessing(new MaxHealthEffect(2)),
        };

        this.addNewVillagerLevel("survivant", blessings);
    }

    private void spawnNain(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new ResistanceEffect(0.4f)),
                new Blessing(new ResistanceEffect(0.6f)),
                new Blessing(new ResistanceEffect(1f)),
                new Blessing(new ResistanceEffect(1.2f)),
                new Blessing(new ResistanceEffect(1.4f)),
        };

        this.addNewVillagerLevel("nain", blessings);
    }

    private void spawnMaddox(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new DamageEffect(0.4f)),
                new Blessing(new DamageEffect(0.6f)),
                new Blessing(new DamageEffect(0.8f)),
                new Blessing(new DamageEffect(1f)),
                new Blessing(new DamageEffect(1.5f)),
        };

        this.addNewVillagerLevel("maddox", blessings);
    }

    private void spawnThomas(){
        String villagerId = "thomas";
        Blessing[] blessings = new Blessing[]{
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau I réussi !")),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau II disponible pour Thomas Pesquet !")),
                new Blessing(new LockWorldEffect(EWorld.NETHER), new MessageEffect("Niveau II réussi !")),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau III disponible pour Thomas Pesquet !")),
                new Blessing(new LimitWorldEffect(EWorld.WILDERNESS, new Rect(10000,-10000, Integer.MAX_VALUE, Integer.MIN_VALUE,10000,-10000))),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau VI disponible pour Thomas Pesquet !")),
                new Blessing(new LockWorldEffect(EWorld.END), new MessageEffect("Niveau VI réussi !")),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau V disponible pour Thomas Pesquet !")),
                new Blessing(new LimitWorldEffect(EWorld.WILDERNESS, new Rect(Integer.MAX_VALUE,Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE))),
        };

        this.addNewVillagerLevel(villagerId, blessings);
    }

    private void spawnFrancois(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new DispelEffect(1)),
                new Blessing(new DispelEffect(2)),
                new Blessing(new DispelEffect(3)),
                new Blessing(new DispelEffect(4)),
                new Blessing(new DispelEffect(5)),
                new Blessing(new DispelEffect(6)),
                new Blessing(new DispelEffect(7)),
                new Blessing(new DispelEffect(8)),
                new Blessing(new DispelEffect(9)),
                new Blessing(new DispelEffect(100)),
        };

        this.addNewVillagerLevel("francois", blessings);
    }
    //endregion VillagerLevel

    //region Trader
    private void spawnGolDRoger(){
        MerchantRecipe fishingRod = new MerchantRecipe(ECustomItem.FISHING_D_ROD.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        fishingRod.addIngredient(new ItemStack(Material.EMERALD_BLOCK, 9));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                fishingRod,
        };

        TextComponent[] messages = new TextComponent[] {
                Component.text("Voici une canne à pêche !"),
        };

        TextComponent openMessage = Component.text("Veux-tu une belle canne à pêche ?");

        AVillager.spawn(() -> {
            Trader t = new Trader("GoldRoger", Component.text("Gol D. Roger"), Villager.Type.SAVANNA, Villager.Profession.WEAPONSMITH, recipes, messages, openMessage);
            t.setJob(EJob.PECHEUR);
            return t;
        });
        VillagerPostLoadActions.add("GoldRoger", villager -> villager.getVillager().getEquipment().setItemInMainHand(new ItemStack(Material.FISHING_ROD)));
    }

    private void spawnHermanos(){
        MerchantRecipe mending = new MerchantRecipe(ECustomItem.MENDING.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        mending.addIngredient(new ItemStack(Material.CHICKEN_SPAWN_EGG, 1));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                mending,
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Voici un livre rare, utilise le intelligemment."),
        };

        TextComponent openMessage = Component.text("J'adore le poulet. Je ne vends aucune drogue.");

        AVillager.spawn(() -> {
            Trader t = new Trader("Hermanos", Component.text("Los Pollos Hermanos"), Villager.Type.SWAMP, Villager.Profession.LIBRARIAN, recipes, messages, openMessage);
            t.setJob(EJob.ENCHANTEUR);
            return t;
        });
    }

    private void spawnSpeedBoots(){
        MerchantRecipe airForce = new MerchantRecipe(ECustomItem.AIR_FORCE.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        airForce.addIngredient(new ItemStack(Material.CHICKEN_SPAWN_EGG, 1));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                airForce,
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici une paire d'Air Force 1."),
        };

        TextComponent openMessage = Component.text("Les Air Force 1, les chaussures qui courent vite.");

        AVillager.spawn(() -> {
            Trader t = new Trader("Nike_49", Component.text("Nike_49"), Villager.Type.PLAINS, Villager.Profession.LEATHERWORKER, recipes, messages, openMessage);
            t.setJob(EJob.AVENTURIER);
            return t;
        });
    }

    private void spawnIndiana(){
        MerchantRecipe mineur = new MerchantRecipe(ECustomItem.MINEUR.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        mineur.addIngredient(new ItemStack(Material.CHICKEN_SPAWN_EGG, 1));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                mineur,
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici un Casque de Mineur"),
        };

        TextComponent openMessage = Component.text("Le Casque de Mineur, le casque qui mine vite.");

        AVillager.spawn(() -> {
            Trader t = new Trader("Indiana", Component.text("Indiana"), Villager.Type.SNOW, Villager.Profession.CARTOGRAPHER, recipes, messages, openMessage);
            t.setJob(EJob.MINEUR);
            return t;
        });
    }

    private void spawnScaleChestplate(){
        MerchantRecipe endiale = new MerchantRecipe(ECustomItem.ENDIALE.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        endiale.addIngredient(new ItemStack(Material.CHICKEN_SPAWN_EGG, 1));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                endiale,
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici une Combinaison pour visiter l'End"),
        };

        TextComponent openMessage = Component.text("La Combinaison Endiale, la combinaison à la bonne taille !");

        AVillager.spawn(() -> {
            Trader t = new Trader("Sophie", Component.text("Sophie Adenot"), Villager.Type.SWAMP, Villager.Profession.FISHERMAN, recipes, messages, openMessage);
            t.setJob(EJob.COMBATANT);
            return t;
        });
    }

    private void spawnBarman(){
        MerchantRecipe sexOnTheBeach = new MerchantRecipe(ECustomItem.SEX_ON_THE_BEACH.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        sexOnTheBeach.addIngredient(new ItemStack(Material.GOLD_INGOT, 10));

        MerchantRecipe pornStarMartini = new MerchantRecipe(ECustomItem.PORN_STAR_MARTINI.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        pornStarMartini.addIngredient(new ItemStack(Material.DIAMOND, 4));

        MerchantRecipe shooterOrgasm = new MerchantRecipe(ECustomItem.SHOOTER_ORGASM.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        shooterOrgasm.addIngredient(new ItemStack(Material.GOLD_BLOCK, 8));

        MerchantRecipe SpicySweetDreamsTicket = new MerchantRecipe(ECustomItem.SPICY_SWEET_DREAMS_TICKET.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        SpicySweetDreamsTicket.addIngredient(new ItemStack(Material.EMERALD, 1));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                sexOnTheBeach,
                pornStarMartini,
                shooterOrgasm,
                SpicySweetDreamsTicket,
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
        };

        TextComponent openMessage = Component.text("Bonjour, bienvenue au bar du Merveilleux Royal Bling-Bling Sexy-Baka Palace-Hôtel. A la carte, nous proposons différents cocktails élaborés avec amour, tendresse et voluptuosité: le Sex On the Beach, le Porn Star Martini, et notre fameux Shooter Orgasm. D’autre part, je peux aussi proposer un Spicy Sweet Dreams Ticket si vous le désirez !");

        AVillager.spawn(() -> {
            Trader trader = new Trader("Barman", Component.text("Fruity Délice"), Villager.Type.SAVANNA, Villager.Profession.FLETCHER, recipes, messages, openMessage);
            trader.setQuestDifficulty(QuestDifficulty.COMMON);
            trader.setJob(EJob.ALCHIMISTE);

            // Items débloqués par réputation
            MerchantRecipe rareCocktail = new MerchantRecipe(new ItemStack(Material.DRAGON_BREATH), 0, 99, false, 0, 0,9,0,true);
            rareCocktail.addIngredient(new ItemStack(Material.DIAMOND, 5));
            trader.addReputationRecipe(rareCocktail, 50); // Débloqué à 50 de réputation

            return trader;
        });
        VillagerPostLoadActions.add("Barman", villager -> villager.getVillager().getEquipment().setItemInMainHand(new ItemStack(Material.POTION)));
    }

    private void spawnReceptionniste(){
        MerchantRecipe cle00 = new MerchantRecipe(ECustomItem.CLE00.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle00.addIngredient(new ItemStack(Material.NETHERITE_INGOT, 12));

        MerchantRecipe cle01 = new MerchantRecipe(ECustomItem.CLE01.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle01.addIngredient(new ItemStack(Material.GOLD_BLOCK, 3));

        MerchantRecipe cle02 = new MerchantRecipe(ECustomItem.CLE02.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle02.addIngredient(new ItemStack(Material.GOLD_INGOT, 9));

        MerchantRecipe cle11 = new MerchantRecipe(ECustomItem.CLE11.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle11.addIngredient(new ItemStack(Material.DIAMOND_BLOCK, 5));

        MerchantRecipe cle12 = new MerchantRecipe(ECustomItem.CLE12.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle12.addIngredient(new ItemStack(Material.DIAMOND, 27));

        MerchantRecipe cle13 = new MerchantRecipe(ECustomItem.CLE13.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle13.addIngredient(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));

        MerchantRecipe cle14 = new MerchantRecipe(ECustomItem.CLE14.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle14.addIngredient(new ItemStack(Material.SPONGE, 1));

        MerchantRecipe cle15 = new MerchantRecipe(ECustomItem.CLE15.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle15.addIngredient(new ItemStack(Material.DRAGON_BREATH, 1));

        MerchantRecipe cle16 = new MerchantRecipe(ECustomItem.CLE16.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle16.addIngredient(new ItemStack(Material.EMERALD, 10));

        MerchantRecipe cle31 = new MerchantRecipe(ECustomItem.CLE31.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle31.addIngredient(new ItemStack(Material.HEART_OF_THE_SEA, 5));

        MerchantRecipe cle32 = new MerchantRecipe(ECustomItem.CLE32.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        cle32.addIngredient(new ItemStack(Material.MUSIC_DISC_PIGSTEP, 1));

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                cle00,
                cle01,
                cle02,
                cle11,
                cle12,
                cle13,
                cle14,
                cle15,
                cle16,
                cle31,
                cle32,
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
        };

        TextComponent openMessage = Component.text("La réception, c’est ma vocation. Bienvenue au Merveilleux Royal Bling-Bling Sexy-Baka Palace-Hôtel. Je vous accueille à bras ouverts dans notre somptueux manoir du 19ième, où le service 5 étoiles fourni par nos employés sauront ravir toutes vos exigences. Le discrétion c’est notre crédo… profitez de votre séjour et explorez à votre rythme chaque recoin de notre manoir qui abrite bien plus de secrets que vous ne pouvez l’imaginer. \n" +
                "Si vous souhaitez siroter une délicieux cocktail aphrodisiaque, notre barman Fruity Delice saura vous contenter. En cas de demande particulière, veuillez-vous référez à Jean Touchatouille… mais on ne sait jamais où il traine celui-ci, il sait se faire discret. Jacques Black, notre croupier, est au premier étage si une envie de jouer vous vient.De toute manière, ici tous les jeux sont gagnants! ");

        AVillager.spawn(() -> {
            Trader t = new Trader("Receptionniste", Component.text("Alainse Lapince"), Villager.Type.JUNGLE, Villager.Profession.CARTOGRAPHER, recipes, messages, openMessage);
            t.setJob(EJob.MARCHAND);
            return t;
        });
    }

    private void spawnConcierge() {
        MerchantRecipe silenceArmorTrim = new MerchantRecipe(new ItemStack(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE), 0, 99, false, 0, 0,9,0,true);
        silenceArmorTrim.addIngredient(ECustomItem.CLE00.getItemStack());

        MerchantRecipe piglinHead = new MerchantRecipe(new ItemStack(Material.PIGLIN_HEAD), 0, 99, false, 0, 0,9,0,true);
        piglinHead.addIngredient(ECustomItem.CLE01.getItemStack());

        MerchantRecipe skeletonSkull = new MerchantRecipe(new ItemStack(Material.SKELETON_SKULL), 0, 99, false, 0, 0,9,0,true);
        skeletonSkull.addIngredient(ECustomItem.CLE02.getItemStack());

        MerchantRecipe parrotSpawn = new MerchantRecipe(new ItemStack(Material.PARROT_SPAWN_EGG), 0, 99, false, 0, 0,9,0,true);
        parrotSpawn.addIngredient(ECustomItem.CLE11.getItemStack());

        MerchantRecipe catSpawn = new MerchantRecipe(new ItemStack(Material.CAT_SPAWN_EGG), 0, 99, false, 0, 0,9,0,true);
        catSpawn.addIngredient(ECustomItem.CLE12.getItemStack());

        MerchantRecipe axolotlBucket = new MerchantRecipe(new ItemStack(Material.AXOLOTL_BUCKET), 0, 99, false, 0, 0,9,0,true);
        axolotlBucket.addIngredient(ECustomItem.CLE13.getItemStack());

        MerchantRecipe prismarineShard = new MerchantRecipe(new ItemStack(Material.PRISMARINE_SHARD, 64), 0, 99, false, 0, 0,9,0,true);
        prismarineShard.addIngredient(ECustomItem.CLE14.getItemStack());

        MerchantRecipe healingArrow = new MerchantRecipe(ECustomItem.HEALING_ARROW.getItemStack(64), 0, 99, false, 0, 0,9,0,true);
        healingArrow.addIngredient(ECustomItem.CLE16.getItemStack());

        MerchantRecipe unbreaking = new MerchantRecipe(ECustomItem.UNBREAKING3.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        unbreaking.addIngredient(ECustomItem.CLE15.getItemStack());

        MerchantRecipe netheriteIngot = new MerchantRecipe(new ItemStack(Material.NETHERITE_INGOT), 0, 99, false, 0, 0,9,0,true);
        netheriteIngot.addIngredient(ECustomItem.CLE31.getItemStack());

        MerchantRecipe terminator = new MerchantRecipe(ECustomItem.TERMINATOR.getItemStack(), 0, 99, false, 0, 0,9,0,true);
        terminator.addIngredient(ECustomItem.CLE32.getItemStack());

        MerchantRecipe[] recipes = new MerchantRecipe[] {
                silenceArmorTrim,
                piglinHead,
                skeletonSkull,
                parrotSpawn,
                catSpawn,
                axolotlBucket,
                prismarineShard,
                healingArrow,
                unbreaking,
                netheriteIngot,
                terminator
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),

        };

        TextComponent openMessage = Component.text("Quoi… Que… Que… vous m’avez repéré… ce n’est pas ce que vous croyez, je… je faisais le ménage! En tout cas j’ai pu être spectateur d’une performance honorable de votre part. Le shooter Orgasme de notre barman Fruity Delice a réussi son effet! Eum Eum… \n" + "\n" +
                "Ecoutez, je pense qu’on peut s’arranger, je ne voudrais pas que de fausses rumeurs se répandent… que diriez-vous d’un arrangement? On est presque ami maintenant. Confirmez-moi que vous êtes bien client de l’hôtel, fournissez-moi vos clefs, et je vous fournirai une récompense en échange de votre aimable discrétion et de votre départ précipité.");

        AVillager.spawn(() -> {
            Trader t = new Trader("Concierge", Component.text("Jean Touchatouille"), Villager.Type.TAIGA, Villager.Profession.ARMORER, recipes, messages, openMessage);
            t.setJob(EJob.FORGERON);
            return t;
        });
    }
    //endregion Trader

    private void addNewVillagerLevel(String id, Blessing[] blessings) {
        VillagerConfig config = VillagerLoader.load(id);

        Villager.Type type = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(config.type.toLowerCase()));
        Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(config.profession.toLowerCase()));
        TextComponent[] names = config.levels.stream().map(level -> Component.text(level.name)).toArray(TextComponent[]::new);
        TextComponent[] messages = config.levels.stream().map(level -> Component.text(level.message)).toArray(TextComponent[]::new);
        TextComponent[] recap = config.levels.stream().map(level -> Component.text(level.recap)).toArray(TextComponent[]::new);
        Tribute[] tributes = config.levels.stream().map(level -> new Tribute(level.tribute.stream().map(SimpleItemStack::toItemStack).toArray(ItemStack[]::new))).toArray(Tribute[]::new);

        AVillager.spawn(() -> new VillagerLevel(config.name, type, profession, blessings, messages, tributes, names, recap));
    }

    /**
     * Retourne tous les Traders enregistrés dans le VillagerRegistry.
     * Utilisé notamment par AlphaPlayer pour calculer les niveaux de métier.
     */
    public List<Trader> getTraders() {
        return VillagerRegistry.getAll().stream()
                .filter(v -> v instanceof Trader)
                .map(v -> (Trader) v)
                .collect(Collectors.toList());
    }

    public void applyAllCurrentBlessing(AlphaPlayer player) {
        TextComponent.Builder builder = Component.text();

        for (MLVillager villager : VillagerRegistry.getAll()) {
            if (!(villager instanceof VillagerLevel villagerLevel))
                continue;

            villagerLevel.applyAllCurrentBlessing(villagerLevel, player);

            Component recap = villagerLevel.getRecapMessage();
            if (recap != null && !PlainTextComponentSerializer.plainText().serialize(recap).isBlank()) {
                builder.append(recap).append(Component.newline());
            }
        }

        Component globalText = builder.build();
        if (PlainTextComponentSerializer.plainText().serialize(globalText).isBlank())
            return;

        player.getPlayer().sendMessage(Component.text()
                .append(Component.text("-------------------- Récapitulatif --------------------\n", NamedTextColor.AQUA))
                .append(globalText)
                .append(Component.text("----------------------------------------------------", NamedTextColor.AQUA))
                .build());
    }
}