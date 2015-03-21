package org.lazywizard.omnifac;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.lazywizard.console.Console;

public class GoodData
{
    private static final Map<String, GoodData> shipData = new HashMap<>();
    private static final Map<String, GoodData> wingData = new HashMap<>();
    private static final Map<String, GoodData> weaponData = new HashMap<>();
    private final GoodType type;
    private final String id, displayName;
    private final int daysToAnalyze, daysToCreate, limit;

    public static enum GoodType
    {
        SHIP,
        WING,
        WEAPON;
    }

    public static void reload() throws IOException, JSONException
    {
        long startTime = System.nanoTime();
        // Load ship data
        shipData.clear();
        for (String id : Global.getSector().getAllEmptyVariantIds())
        {
            final FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, id);
            shipData.put(tmp.getHullId(), new GoodData(tmp));
        }

        // Load wing data
        wingData.clear();
        for (String id : Global.getSector().getAllFighterWingIds())
        {
            final FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                    FleetMemberType.FIGHTER_WING, id);
            wingData.put(tmp.getSpecId(), new GoodData(tmp));
        }

        // Load weapon data
        weaponData.clear();
        for (String id : Global.getSector().getAllWeaponIds())
        {
            final WeaponSpecAPI tmp = Global.getSettings().getWeaponSpec(id);
            weaponData.put(id, new GoodData(tmp));
        }

        long endTime = System.nanoTime();
        Global.getLogger(GoodData.class).log(Level.INFO,
                "Loaded information for " + shipData.size() + " ships, "
                + wingData.size() + " wings, and " + weaponData.size()
                + " weapons in " + DecimalFormat.getNumberInstance().format(
                        (endTime - startTime) / 1000000000.0d) + " seconds");

        // Debug
        List<GoodData> datas = new ArrayList<>(shipData.values());
        datas.addAll(wingData.values());
        datas.addAll(weaponData.values());
        for (GoodData data : datas)
        {
            Console.showMessage("\n" + data, Level.ALL);
        }
    }

    public static Map<String, GoodData> getShipData()
    {
        return Collections.unmodifiableMap(shipData);
    }

    public static Map<String, GoodData> getWingData()
    {
        return Collections.unmodifiableMap(wingData);
    }

    public static Map<String, GoodData> getWeaponData()
    {
        return Collections.unmodifiableMap(weaponData);
    }

    private GoodData(FleetMemberAPI tmp)
    {
        id = (tmp.isFighterWing() ? tmp.getSpecId() : tmp.getHullId());
        displayName = tmp.getHullSpec().getHullName();
        type = (tmp.isFighterWing() ? GoodType.WING : GoodType.SHIP);

        final int fp = tmp.getFleetPointCost();
        final int size = tmp.getHullSpec().getHullSize().ordinal();
        daysToCreate = (int) Math.max(((fp * size) / 2f)
                * OmniFacSettings.getShipProductionTimeMod(), size * 3f);
        daysToAnalyze = (int) Math.max(1f, daysToCreate
                * OmniFacSettings.getShipAnalysisTimeMod());

        switch (size)
        {
            case 1:
                limit = OmniFacSettings.getMaxHullsPerFighter();
                break;
            case 2:
                limit = OmniFacSettings.getMaxHullsPerFrigate();
                break;
            case 3:
                limit = OmniFacSettings.getMaxHullsPerDestroyer();
                break;
            case 4:
                limit = OmniFacSettings.getMaxHullsPerCruiser();
                break;
            case 5:
                limit = OmniFacSettings.getMaxHullsPerCapital();
                break;
            default:
                limit = 0;
        }
    }

    private GoodData(WeaponSpecAPI tmp)
    {
        id = tmp.getWeaponId();
        displayName = "";
        type = GoodType.WEAPON;

        int size;
        switch (tmp.getSize())
        {
            case SMALL:
                size = 4;
                break;
            case MEDIUM:
                size = 8;
                break;
            case LARGE:
                size = 16;
                break;
            default:
                size = 16;
        }

        daysToCreate = (int) Math.max(size * OmniFacSettings.getWeaponProductionTimeMod(), 1f);
        daysToAnalyze = (int) Math.max(1f, daysToCreate
                * OmniFacSettings.getWeaponAnalysisTimeMod());
        limit = (int) ((80 / size) * OmniFacSettings.getMaxStacksPerWeapon());
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return displayName;
    }

    public GoodType getType()
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
