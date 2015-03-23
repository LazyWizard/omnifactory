package org.lazywizard.omnifactory;

import java.util.Objects;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.omnifactory.Omnifactory.BlueprintStatus;

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
                baseLimit = 40;
                break;
            case MEDIUM:
                baseDaysToCreate = 10;
                baseLimit = 20;
                break;
            case LARGE:
            default:
                baseDaysToCreate = 20;
                baseLimit = 10;
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

    public String getDisplayName()
    {
        return displayName;
    }

    public BlueprintType getType()
    {
        return type;
    }

    public BlueprintStatus getStatus()
    {
        return Omnifactory.getBlueprintStatus(type, id);
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

    public int getTotalInCargo(CargoAPI cargo)
    {
        switch (type)
        {
            case SHIP:
            case WING:
                int total = 0;
                for (FleetMemberAPI tmp : cargo.getMothballedShips().getMembersListCopy())
                {
                    String otherId = (type == BlueprintType.WING ? tmp.getSpecId() : tmp.getHullId());
                    if (id.equals(otherId))
                    {
                        total++;
                    }
                }

                return total;
            case WEAPON:
                return cargo.getNumWeapons(id);
            default:
                throw new RuntimeException("No such known type: " + type.name());
        }
    }

    public void create(CargoAPI cargo)
    {
        switch (type)
        {
            case SHIP:
                cargo.getMothballedShips().addFleetMember(
                        Global.getFactory().createFleetMember(
                                FleetMemberType.SHIP, id + "_Hull"));
                break;
            case WING:
                cargo.getMothballedShips().addFleetMember(
                        Global.getFactory().createFleetMember(
                                FleetMemberType.FIGHTER_WING, id));
                break;
            case WEAPON:
                cargo.addWeapons(id, 1);
                break;
            default:
                throw new RuntimeException("No such known type: " + type.name());
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.type);
        hash = 73 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        final Blueprint other = (Blueprint) obj;
        if (this.type != other.type)
        {
            return false;
        }

        return Objects.equals(this.id, other.id);
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
