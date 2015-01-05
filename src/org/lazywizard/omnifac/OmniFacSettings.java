package org.lazywizard.omnifac;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class OmniFacSettings
{
    private final boolean showAddedCargo;
    private final boolean showAnalysisComplete;
    private final boolean showLimitReached;
    private final boolean removeBrokenGoods;
    private final float shipAnalysisTimeMod;
    private final float weaponAnalysisTimeMod;
    private final float shipProductionTimeMod;
    private final float weaponProductionTimeMod;
    private final int requiredCrew;
    private final float requiredSuppliesPerDay;
    private final float requiredFuelPerDay;
    private final int maxHullsPerFighter;
    private final int maxHullsPerFrigate;
    private final int maxHullsPerDestroyer;
    private final int maxHullsPerCruiser;
    private final int maxHullsPerCapital;
    private final float maxStacksPerWeapon;

    OmniFacSettings(String settingsFile) throws JSONException, IOException
    {
        JSONObject settings = Global.getSettings().loadJSON(settingsFile);
        showAddedCargo = settings.getBoolean("showAddedCargo");
        showAnalysisComplete = settings.getBoolean("showAnalysisComplete");
        showLimitReached = settings.getBoolean("showLimitReached");
        removeBrokenGoods = settings.getBoolean("removeBrokenGoods");
        shipAnalysisTimeMod = (float) settings.getDouble("shipAnalysisTimeMod");
        weaponAnalysisTimeMod = (float) settings.getDouble("weaponAnalysisTimeMod");
        shipProductionTimeMod = (float) settings.getDouble("shipProductionTimeMod");
        weaponProductionTimeMod = (float) settings.getDouble("weaponProductionTimeMod");
        requiredCrew = settings.getInt("requiredCrewToFunction");
        requiredSuppliesPerDay = (float) settings.getDouble("requiredSuppliesPerDay");
        requiredFuelPerDay = (float) settings.getDouble("requiredFuelPerDay");
        maxHullsPerFighter = settings.getInt("maxHullsPerFighter");
        maxHullsPerFrigate = settings.getInt("maxHullsPerFrigate");
        maxHullsPerDestroyer = settings.getInt("maxHullsPerDestroyer");
        maxHullsPerCruiser = settings.getInt("maxHullsPerCruiser");
        maxHullsPerCapital = settings.getInt("maxHullsPerCapital");
        maxStacksPerWeapon = (float) settings.getDouble("maxStacksPerWeapon");
    }

    public boolean shouldShowAddedCargo()
    {
        return showAddedCargo;
    }

    public boolean shouldShowAnalysisComplete()
    {
        return showAnalysisComplete;
    }

    public boolean shouldShowLimitReached()
    {
        return showLimitReached;
    }

    public boolean shouldRemoveBrokenGoods()
    {
        return removeBrokenGoods;
    }

    public float getShipAnalysisTimeMod()
    {
        return shipAnalysisTimeMod;
    }

    public float getWeaponAnalysisTimeMod()
    {
        return weaponAnalysisTimeMod;
    }

    public float getShipProductionTimeMod()
    {
        return shipProductionTimeMod;
    }

    public float getWeaponProductionTimeMod()
    {
        return weaponProductionTimeMod;
    }

    public int getRequiredCrew()
    {
        return requiredCrew;
    }

    public float getRequiredSuppliesPerDay()
    {
        return requiredSuppliesPerDay;
    }

    public float getRequiredFuelPerDay()
    {
        return requiredFuelPerDay;
    }

    public int getMaxHullsPerFighter()
    {
        return maxHullsPerFighter;
    }

    public int getMaxHullsPerFrigate()
    {
        return maxHullsPerFrigate;
    }

    public int getMaxHullsPerDestroyer()
    {
        return maxHullsPerDestroyer;
    }

    public int getMaxHullsPerCruiser()
    {
        return maxHullsPerCruiser;
    }

    public int getMaxHullsPerCapital()
    {
        return maxHullsPerCapital;
    }

    public float getMaxStacksPerWeapon()
    {
        return maxStacksPerWeapon;
    }
}
