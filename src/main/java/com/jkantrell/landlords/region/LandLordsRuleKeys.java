package com.jkantrell.landlords.region;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import com.jkantrell.regionslib.regions.rules.RuleEnumDataType;
import com.jkantrell.regionslib.regions.rules.RuleKey;

public class LandLordsRuleKeys {

    //DATATYPES
    public enum TntProtection { none, all, ignitor }

    //KEYS
    public final RuleKey CREEPER_PROTECTED_KEY;
    public final RuleKey TNT_PROTECTED;
    public final RuleKey FIRE_PROTECTED;
    public final RuleKey RAID_PROTECTED;
    public final RuleKey NO_MONSTER_SPAWN;
    public final RuleKey AUTOPLANT;
    
    //FIELDS
    private static Landlords mainInstance_ = null;

    public LandLordsRuleKeys(Landlords landlordsInstance) {
        mainInstance_ = landlordsInstance;
        this.CREEPER_PROTECTED_KEY = RuleKey.registerNew(mainInstance_,"creeperProtected", RuleDataType.BOOL);
        this.TNT_PROTECTED = RuleKey.registerNew(mainInstance_,"tntProtected", new RuleEnumDataType<>(TntProtection.class));
        this.FIRE_PROTECTED = RuleKey.registerNew(mainInstance_,"fireProtected",RuleDataType.BOOL);
        this.RAID_PROTECTED = RuleKey.registerNew(mainInstance_,"raidProtected",RuleDataType.BOOL);
        this.NO_MONSTER_SPAWN = RuleKey.registerNew(mainInstance_,"noMonsterSpawn",RuleDataType.BOOL);
        this.AUTOPLANT = RuleKey.registerNew(mainInstance_,"autoplant",RuleDataType.BOOL);
    }
}
