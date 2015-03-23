package org.lazywizard.omnifactory;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;

public class Omnifactory
{
    private static final String SHIP_PDATA_ID = "omnifactory_known_ships";
    private static final String WING_PDATA_ID = "omnifactory_known_wings";
    private static final String WEAPON_PDATA_ID = "omnifactory_known_weapons";

    private static Map<String, BlueprintStatus> getKnownBlueprints(BlueprintType type)
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

    public static boolean isKnownBlueprint(BlueprintType type, String id)
    {
        return getKnownBlueprints(type).containsKey(id);
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
    }
}
