package org.lazywizard.omnifactory;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

public class Blueprint
{
    private final BlueprintType type;
    private final String id, displayName;
    private final int daysToAnalyze, daysToCreate, limit;

    public static enum BlueprintType
    {
        SHIP,
        WING,
        WEAPON;
    }

    Blueprint(FleetMemberAPI tmp)
    {
        id = (tmp.isFighterWing() ? tmp.getSpecId() : tmp.getHullId());
        displayName = tmp.getHullSpec().getHullName();
        type = (tmp.isFighterWing() ? BlueprintType.WING : BlueprintType.SHIP);

        final int fp = tmp.getFleetPointCost();
        final int size = tmp.getHullSpec().getHullSize().ordinal();
        daysToCreate = (int) Math.max(size * 3f, ((fp * size) / 2f)
                * OmnifactorySettings.getShipProductionTimeMod());
        daysToAnalyze = (int) Math.max(1f, daysToCreate
                * OmnifactorySettings.getShipAnalysisTimeMod());
        limit = OmnifactorySettings.getMaxHullsPerShip(tmp.getHullSpec().getHullSize());
    }

    Blueprint(CargoStackAPI tmp)
    {
        WeaponSpecAPI spec = tmp.getWeaponSpecIfWeapon();
        id = spec.getWeaponId();
        displayName = tmp.getDisplayName();
        type = BlueprintType.WEAPON;

        int baseDaysToCreate, baseLimit;
        switch (spec.getSize())
        {
            case SMALL:
                baseDaysToCreate = 5;
                baseLimit = 20;
                break;
            case MEDIUM:
                baseDaysToCreate = 10;
                baseLimit = 10;
                break;
            case LARGE:
            default:
                baseDaysToCreate = 20;
                baseLimit = 5;
                break;
        }

        daysToCreate = (int) Math.max(1f, baseDaysToCreate * OmnifactorySettings.getWeaponProductionTimeMod());
        daysToAnalyze = (int) Math.max(1f, daysToCreate
                * OmnifactorySettings.getWeaponAnalysisTimeMod());
        limit = (int) Math.max(1f, baseLimit * OmnifactorySettings.getMaxStacksPerWeapon());
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return displayName;
    }

    public BlueprintType getType()
    {
        return type;
    }

    public int getDaysToAnalyze()
    {
        return daysToAnalyze;
    }

    public int getDaysToCreate()
    {
        return daysToCreate;
    }

    public int getLimit()
    {
        return limit;
    }

    @Override
    public String toString()
    {
        return displayName
                + "\nType: " + type
                + "\nId: " + id
                + "\nDays to analyze: " + daysToAnalyze
                + "\nDays to create: " + daysToCreate
                + "\nLimit: " + limit;
    }
}
