package org.lazywizard.omnifactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;

// Handles static blueprint data
public class BlueprintMaster
{
    private static final Map<String, Blueprint> shipBlueprints = new HashMap<>();
    private static final Map<String, Blueprint> wingBlueprints = new HashMap<>();
    private static final Map<String, Blueprint> weaponBlueprints = new HashMap<>();
    private static boolean HAS_LOADED = false;

    public static void reloadBlueprints() throws IOException, JSONException
    {
        HAS_LOADED = true;

        long startTime = System.nanoTime();
        // Load ship data
        shipBlueprints.clear();
        for (String id : Global.getSector().getAllEmptyVariantIds())
        {
            final FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, id);
            shipBlueprints.put(getBlueprintId(tmp), new Blueprint(tmp));
        }

        // Load wing data
        wingBlueprints.clear();
        for (String id : Global.getSector().getAllFighterWingIds())
        {
            final FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                    FleetMemberType.FIGHTER_WING, id);
            wingBlueprints.put(getBlueprintId(tmp), new Blueprint(tmp));
        }

        // Load weapon data
        // TODO: If getDisplayName() is added to WeaponSpec this could be done in one line
        weaponBlueprints.clear();
        final CargoAPI cargo = Global.getFactory().createCargo(true);
        cargo.clear();
        for (String id : Global.getSector().getAllWeaponIds())
        {
            cargo.addWeapons(id, 1);
        }
        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (stack.isNull())
            {
                continue;
            }

            weaponBlueprints.put(stack.getWeaponSpecIfWeapon().getWeaponId(), new Blueprint(stack));
        }

        long endTime = System.nanoTime();
        Global.getLogger(Blueprint.class).log(Level.INFO,
                "Loaded blueprints for " + shipBlueprints.size() + " ships, "
                + wingBlueprints.size() + " wings, and " + weaponBlueprints.size()
                + " weapons in " + DecimalFormat.getNumberInstance().format(
                        (endTime - startTime) / 1000000000.0d) + " seconds");
    }

    private static void checkLoaded()
    {
        if (!HAS_LOADED)
        {
            try
            {
                reloadBlueprints();
            }
            catch (IOException | JSONException ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    public static Blueprint getBlueprint(BlueprintType type, String id)
    {
        checkLoaded();
        switch (type)
        {
            case SHIP:
                return shipBlueprints.get(id);
            case WING:
                return wingBlueprints.get(id);
            case WEAPON:
                return weaponBlueprints.get(id);
            default:
                throw new RuntimeException("No such blueprint type: " + type.name());
        }
    }

    public static BlueprintType getBlueprintType(FleetMemberAPI member)
    {
        return (member.isFighterWing() ? BlueprintType.WING : BlueprintType.SHIP);
    }

    public static String getBlueprintId(FleetMemberAPI member)
    {
        return (member.isFighterWing() ? member.getSpecId() : member.getHullId());
    }

    public static Map<String, Blueprint> getAllBlueprints(BlueprintType type)
    {
        checkLoaded();
        switch (type)
        {
            case SHIP:
                return Collections.unmodifiableMap(shipBlueprints);
            case WING:
                return Collections.unmodifiableMap(wingBlueprints);
            case WEAPON:
                return Collections.unmodifiableMap(weaponBlueprints);
            default:
                throw new RuntimeException("No such blueprint type: " + type.name());
        }
    }
}
