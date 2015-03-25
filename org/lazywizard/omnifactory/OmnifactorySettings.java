package org.lazywizard.omnifactory;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OmnifactorySettings
{
    private static final String MOD_ID = "lw_omnifac";
    private static final String SETTINGS_FILE = "data/config/omnifactory/omnifac_settings.json";
    private static final String RESTRICTED_WEAPONS_CSV = "data/config/omnifactory/restricted_weapons.csv";
    private static final String RESTRICTED_SHIPS_CSV = "data/config/omnifactory/restricted_ships.csv";
    private static Set<String> restrictedWeapons;
    private static Set<String> restrictedShips;
    private static boolean showAddedCargo;
    private static boolean showAnalysisComplete;
    private static boolean allowRestrictedGoods;
    private static float shipAnalysisTimeMod;
    private static float weaponAnalysisTimeMod;
    private static float shipProductionTimeMod;
    private static float weaponProductionTimeMod;
    private static Map<HullSize, Integer> maxHullsPerShip;
    private static float maxStacksPerWeapon;

    public static void reloadSettings() throws JSONException, IOException
    {
        // Base Omnifactory settings
        JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);
        showAddedCargo = settings.getBoolean("showAddedCargo");
        showAnalysisComplete = settings.getBoolean("showAnalysisComplete");
        allowRestrictedGoods = settings.getBoolean("ignoreGoodRestrictions");
        shipAnalysisTimeMod = (float) settings.getDouble("shipAnalysisTimeMod");
        weaponAnalysisTimeMod = (float) settings.getDouble("weaponAnalysisTimeMod");
        shipProductionTimeMod = (float) settings.getDouble("shipProductionTimeMod");
        weaponProductionTimeMod = (float) settings.getDouble("weaponProductionTimeMod");
        maxStacksPerWeapon = (float) settings.getDouble("maxStacksPerWeapon");
        maxHullsPerShip = new EnumMap<>(HullSize.class);
        maxHullsPerShip.put(HullSize.DEFAULT, 0);
        maxHullsPerShip.put(HullSize.FIGHTER, settings.getInt("maxHullsPerFighter"));
        maxHullsPerShip.put(HullSize.FRIGATE, settings.getInt("maxHullsPerFrigate"));
        maxHullsPerShip.put(HullSize.DESTROYER, settings.getInt("maxHullsPerDestroyer"));
        maxHullsPerShip.put(HullSize.CRUISER, settings.getInt("maxHullsPerCruiser"));
        maxHullsPerShip.put(HullSize.CAPITAL_SHIP, settings.getInt("maxHullsPerCapital"));
        // Restricted goods
        JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod("weapon id",
                RESTRICTED_WEAPONS_CSV, MOD_ID);
        restrictedWeapons = new HashSet<>();
        for (int x = 0; x < csv.length(); x++)
        {
            JSONObject row = csv.getJSONObject(x);
            restrictedWeapons.add(row.getString("weapon id"));
        }

        // Restricted ships
        csv = Global.getSettings().getMergedSpreadsheetDataForMod("hull id",
                RESTRICTED_SHIPS_CSV, MOD_ID);
        restrictedShips = new HashSet<>();
        for (int x = 0; x < csv.length(); x++)
        {
            JSONObject row = csv.getJSONObject(x);
            restrictedShips.add(row.getString("hull id"));
        }
    }

    public static Set<String> getRestrictedWeapons()
    {
        if (allowRestrictedGoods)
        {
            return Collections.<String>emptySet();
        }

        return restrictedWeapons;
    }

    public static Set<String> getRestrictedShips()
    {
        if (allowRestrictedGoods)
        {
            return Collections.<String>emptySet();
        }

        return restrictedShips;
    }

    public static boolean shouldShowAddedCargo()
    {
        return showAddedCargo;
    }

    public static boolean shouldShowAnalysisComplete()
    {
        return showAnalysisComplete;
    }

    public static float getShipAnalysisTimeMod()
    {
        return shipAnalysisTimeMod;
    }

    public static float getWeaponAnalysisTimeMod()
    {
        return weaponAnalysisTimeMod;
    }

    public static float getShipProductionTimeMod()
    {
        return shipProductionTimeMod;
    }

    public static float getWeaponProductionTimeMod()
    {
        return weaponProductionTimeMod;
    }

    public static int getMaxHullsPerShip(HullSize size)
    {
        return maxHullsPerShip.get(size);
    }

    public static float getMaxStacksPerWeapon()
    {
        return maxStacksPerWeapon;
    }

    private OmnifactorySettings()
    {
    }
}
