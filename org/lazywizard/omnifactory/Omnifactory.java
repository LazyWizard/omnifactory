package org.lazywizard.omnifactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;

// Handles per-save blueprint data
public class Omnifactory
{
    private static final String FACTORY_PDATA_ID = "omnifactory_all_factories";
    private static final String SHIP_PDATA_ID = "omnifactory_known_ships";
    private static final String WING_PDATA_ID = "omnifactory_known_wings";
    private static final String WEAPON_PDATA_ID = "omnifactory_known_weapons";

    public static Set<SectorEntityToken> getAllOmnifactories()
    {
        final Map<String, Object> pData = Global.getSector().getPersistentData();
        if (!pData.containsKey(FACTORY_PDATA_ID))
        {
            pData.put(FACTORY_PDATA_ID, new HashSet<SectorEntityToken>());
        }

        return (Set<SectorEntityToken>) pData.get(FACTORY_PDATA_ID);
    }

    public static Map<String, BlueprintStatus> getKnownBlueprints(BlueprintType type)
    {
        final String dataId;
        switch (type)
        {
            case SHIP:
                dataId = SHIP_PDATA_ID;
                break;
            case WING:
                dataId = WING_PDATA_ID;
                break;
            case WEAPON:
                dataId = WEAPON_PDATA_ID;
                break;
            default:
                throw new RuntimeException("No such blueprint type: " + type.name());
        }

        final Map<String, Object> pData = Global.getSector().getPersistentData();
        if (!pData.containsKey(dataId))
        {
            pData.put(dataId, new LinkedHashMap<String, BlueprintStatus>());
        }

        return (Map<String, BlueprintStatus>) pData.get(dataId);
    }

    public static BlueprintStatus getBlueprintStatus(BlueprintType type, String id)
    {
        return getKnownBlueprints(type).get(id);
    }

    public static boolean addBlueprint(BlueprintType type, String id, boolean isAnalyzed)
    {
        // Not a valid id
        if (!BlueprintMaster.getAllBlueprints(type).containsKey(id))
        {
            throw new RuntimeException("No such blueprint of type \""
                    + type.name() + "\": " + id);
        }

        // Blueprint already known
        final Map<String, BlueprintStatus> knownBlueprints = getKnownBlueprints(type);
        if (knownBlueprints.containsKey(id))
        {
            return false;
        }

        // Register blueprint with the Omnifactory
        knownBlueprints.put(id, new BlueprintStatus(isAnalyzed));
        return true;
    }

    public static boolean removeBlueprint(BlueprintType type, String id)
    {
        return (getKnownBlueprints(type).remove(id) != null);
    }

    public static boolean isBlueprintKnown(BlueprintType type, String id)
    {
        return getKnownBlueprints(type).containsKey(id);
    }

    public static boolean isBlueprintKnown(FleetMemberAPI member)
    {
        return isBlueprintKnown(BlueprintMaster.getBlueprintType(member),
                BlueprintMaster.getBlueprintId(member));
    }

    public static boolean isBlueprintKnown(CargoStackAPI stack)
    {
        if (stack.isNull() || !stack.isWeaponStack())
        {
            return false;
        }

        return isBlueprintKnown(BlueprintType.WEAPON,
                stack.getWeaponSpecIfWeapon().getWeaponId());
    }

    private Omnifactory()
    {
    }

    public static class BlueprintStatus
    {
        private boolean isAnalyzed;
        private int totalCreated;

        private BlueprintStatus(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
            totalCreated = 0;
        }

        public boolean isAnalyzed()
        {
            return isAnalyzed;
        }

        public void setAnalyzed(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
        }

        public int getTotalCreated()
        {
            return totalCreated;
        }

        public void incTotalCreated()
        {
            totalCreated++;
        }
    }
}
