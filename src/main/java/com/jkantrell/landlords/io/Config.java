package com.jkantrell.landlords.io;

import com.jkantrell.yamlizer.yaml.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                            yamlizer_.deserialize(map.get("delta"),int[].class)
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
        yamlizer_.addSerializationRule(Level.class,
            (e,t) -> Level.parse(e.get(YamlElementType.STRING))
        );
    }

    //ENUMS
    public enum EndCrystalOnAnyBlock {
        never,
        always,
        on_totem
    }
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
    public record ParticleData(Particle particle, int count, int[] delta) {}
    public record TitleData(int fadeIn, int stay, int fadeOut) {}

    //FIELDS
    public String configPath = "plugins/Landlords";

    @ConfigField(path = "logging_level")
    public Level loggingLevel = Level.INFO;

    @ConfigField(path = "default_language")
    public String defaultLanguageCode = "en";

    @ConfigField(path = "end_crystal_on_any_block")
    public EndCrystalOnAnyBlock endCrystalOnAnyBlock = EndCrystalOnAnyBlock.on_totem;

    @ConfigField(path = "totems.default_group_level")
    public int totemDefaultGroupLevel = 1;

    @ConfigField(path = "totems.upgrade_item")
    public TotemInteractionData totemUpgradeItem = new TotemInteractionData(Material.DIAMOND,1,true);

    @ConfigField(path = "totems.downgrade_item")
    public TotemInteractionData totemDowngradeItem = new TotemInteractionData(Material.DIAMOND,1,true);

    @ConfigField(path = "totems.interact_cooldown")
    public int totemInteractCoolDown = 400;

    @ConfigField(path = "totems.drop_back_rate")
    public double totemDropBackRate = 0.5;

    @ConfigField(path = "totems.place_particle_effect")
    public Config.ParticleData totemPlaceParticleEffect = new ParticleData(Particle.REVERSE_PORTAL,400,new int[] {0,0,0});

    @ConfigField(path = "totems.place_particle_effect.position")
    public int[] totemPlaceParticlePos = {0,-3,0};

    @ConfigField(path = "totems.destroy_arrow_effects")
    public List<PotionType> totemDestroyArrowEffects = List.of(PotionType.POISON);

    @ConfigField(path = "deeds.exchange_item")
    public Material deedsExchangeItem = Material.WRITABLE_BOOK;

    @ConfigField(path = "deeds.players_per_page")
    public int deedsPlayersPerPage = 13;

    @ConfigField (path = "deeds.players_for_new_page")
    public int deedsPlayersForNewPage = 10;

    @ConfigField (path = "regions.minimum_name_length")
    public int regionsMinimumNameLength = 8;

    @ConfigField (path = "regions.maximum_name_length")
    public int regionsMaximumNameLength = 32;

    @ConfigField(path = "regions.border_refresh_rate")
    public int regionsBorderRefreshRate = 10;

    @ConfigField(path = "regions.border_persistence_bell")
    public int regionsBorderPersistenceBell = 280;

    @ConfigField(path = "regions.border_persistence_placed")
    public int regionsBorderPersistencePlaced = 420;

    @ConfigField(path = "regions.enforced_buttons")
    public List<Material> regionsEnforcedButtons = List.of(Material.STONE_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON);

    @ConfigField(path = "regions.lever_locker_blocks")
    public List<Material> regionsLeverLockerBlocks = List.of(Material.COPPER_BLOCK, Material.IRON_BLOCK);

    @ConfigField(path = "messages_reach.region_resize")
    public Config.GroupLevelReach msgReachRegionResize = GroupLevelReach.all;
}
