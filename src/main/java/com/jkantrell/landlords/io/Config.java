package com.jkantrell.landlords.io;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.yamlizer.yaml.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.potion.PotionType;
import java.util.List;
import java.util.logging.Level;

public class Config extends AbstractYamlConfig {

    //CONSTRUCTOR
    public Config(String filePath) {
        super(filePath);
        Yamlizer yamlizer_ = this.yamlizer;

        yamlizer_.addSerializationRule(Config.ParticleData.class,
                (e,t) -> {
                    YamlMap map = e.get(YamlElementType.MAP);
                    return new Config.ParticleData(
                            Particle.valueOf(map.get("type").get(YamlElementType.STRING)),
                            map.get("count").get(YamlElementType.INT),
                            yamlizer_.deserialize(map.get("delta"),Double[].class)
                    );
                }
        );
        yamlizer_.addSerializationRule(Config.TotemInteractionData.class,
            (e,t) -> {
                YamlMap map = e.get(YamlElementType.MAP);
                return new TotemInteractionData(
                        Material.valueOf(map.get("item").get(YamlElementType.STRING)),
                        map.get("count").get(YamlElementType.INT).intValue(),
                        map.get("consume").get(YamlElementType.BOOL).booleanValue()
                );
            }
        );
        yamlizer_.addSerializationRule(Config.GroupLevelReach.class,
            (e,t) -> {
                if (e.is(YamlElementType.INT)) {
                    Config.GroupLevelReach r = GroupLevelReach.lvl;
                    r.setLevel(e.get(YamlElementType.INT));
                    return r;
                } else {
                    return Config.GroupLevelReach.valueOf(e.get(YamlElementType.STRING));
                }
            }
        );
        yamlizer_.addSerializationRule(Config.TitleData.class,
            (e,t) -> {
                YamlMap map = e.get(YamlElementType.MAP);
                return new Config.TitleData(
                        map.get("fadeIn").get(YamlElementType.INT),
                        map.get("stay").get(YamlElementType.INT),
                        map.get("fadeOut").get(YamlElementType.INT)
                );
            }
        );
        yamlizer_.addSerializationRule(Level.class,
            (e,t) -> {
                Level level = Level.parse(e.get(YamlElementType.STRING));
                Landlords.getMainInstance().getLogger().setLevel(level);
                return level;
            }
        );
    }

    //ENUMS
    public enum GroupLevelReach {
        noOne, all, responsible, members, lvl;
        private int level_ = -1;

        public void setLevel(int level) {
            this.level_ = level;
        }
        public int getLevel() {
            return this.level_;
        }
    }

    //RECORDS
    public record TotemInteractionData(Material item, int count, boolean consume) {}
    public record TitleData(int fadeIn, int stay, int fadeOut) {}
    public record ParticleData(Particle particle, int count, Double[] delta) {
        public void spawn(Location location, double speed) {
            if (location.getWorld() == null) {
                throw new IllegalArgumentException("The location must contain a World");
            }
            this.spawn(location.getWorld(),location,speed);
        }
        public void spawn(World world, Location location, double speed) {
            Double[] delta = this.delta();
            world.spawnParticle(this.particle(),location,this.count,delta[0].doubleValue(),delta[1].doubleValue(),delta[2].doubleValue(),speed);
        }
    }

    //FIELDS
    public String configPath = "plugins/Landlords";

    @ConfigField(path = "logging_level")
    public Level loggingLevel = Level.INFO;

    @ConfigField(path = "default_language")
    public String defaultLanguageCode = "en";

    @ConfigField(path = "totems.default_group_level")
    public int totemDefaultGroupLevel = 1;

    @ConfigField(path = "totems.upgrade_item")
    public TotemInteractionData totemUpgradeItem = new TotemInteractionData(Material.DIAMOND,1,true);

    @ConfigField(path = "totems.directional_item")
    public Material totemDirectionalItem = Material.BLAZE_ROD;

    @ConfigField(path = "totems.interact_cooldown")
    public int totemInteractCoolDown = 400;

    @ConfigField(path = "totems.drop_back_rate")
    public double totemDropBackRate = 0.5;

    @ConfigField(path = "totems.particles.enable")
    public Config.ParticleData totemEnableParticleData = new ParticleData(Particle.REVERSE_PORTAL,400,new Double[] {0.0,0.0,0.0});

    @ConfigField(path = "totems.particles.hurt")
    public Config.ParticleData totemHurtParticleData = new ParticleData(Particle.DRAGON_BREATH,120,new Double[] {.3,.3,.3});

    @ConfigField(path = "totems.particles.feed")
    public Config.ParticleData totemFeedParticleData = new ParticleData(Particle.PORTAL,250,new Double[] {.3,.3,.3});

    @ConfigField(path = "totems.particles.disable")
    public Config.ParticleData totemDisableParticleData = new ParticleData(Particle.CRIT,300,new Double[] {.6,.6,.6});

    @ConfigField(path = "totems.destroy_arrow_effects")
    public List<PotionType> totemDestroyArrowEffects = List.of(PotionType.POISON);

    @ConfigField(path = "deeds.exchange_item")
    public Material deedsExchangeItem = Material.WRITABLE_BOOK;

    @ConfigField(path = "deeds.players_per_page")
    public int deedsPlayersPerPage = 13;

    @ConfigField (path = "deeds.players_for_new_page")
    public int deedsPlayersForNewPage = 10;

    @ConfigField (path = "regions.names_length_limit.min")
    public int regionsMinimumNameLength = 8;

    @ConfigField (path = "regions.names_length_limit.max")
    public int regionsMaximumNameLength = 32;

    @ConfigField(path = "regions.border_display.refresh_rate")
    public int regionsBorderRefreshRate = 10;

    @ConfigField(path = "regions.border_persistence_bell")
    public int regionsBorderPersistenceBell = 280;

    @ConfigField(path = "regions.border_persistence_placed")
    public int regionsBorderPersistencePlaced = 420;

    @ConfigField(path = "regions.enforced_buttons")
    public List<Material> regionsEnforcedButtons = List.of(Material.STONE_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON);

    @ConfigField(path = "regions.lever_locker_blocks")
    public List<Material> regionsLeverLockerBlocks = List.of(Material.COPPER_BLOCK, Material.IRON_BLOCK);

    @ConfigField(path = "regions.name_title_display")
    public Config.TitleData regionsNameTitleData = new TitleData(10,30,8);

    @ConfigField(path = "regions.name_title_display.enabled")
    public boolean regionsNameTitleEnabled = true;

    @ConfigField(path = "messages_reach.region_resize")
    public Config.GroupLevelReach msgReachRegionResize = GroupLevelReach.all;
}
