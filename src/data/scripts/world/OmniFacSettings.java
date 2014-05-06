package data.scripts.world;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;

public class OmniFacSettings
{
    public boolean SHOW_ADDED_CARGO = false;
    public boolean SHOW_ANALYSIS_COMPLETE = true;
    public boolean SHOW_LIMIT_REACHED = true;
    public boolean REMOVE_BROKEN_GOODS = false;
    public float SHIP_ANALYSIS_TIME_MOD = 0.5f;
    public float WEAPON_ANALYSIS_TIME_MOD = 0.5f;
    public float SHIP_PRODUCTION_TIME_MOD = 1.0f;
    public float WEAPON_PRODUCTION_TIME_MOD = 1.0f;
    public int REQUIRED_CREW = 0;
    public float REQUIRED_SUPPLIES_PER_DAY = 0f;
    public float REQUIRED_FUEL_PER_DAY = 0f;
    public int MAX_HULLS_PER_FIGHTER = 3;
    public int MAX_HULLS_PER_FRIGATE = 3;
    public int MAX_HULLS_PER_DESTROYER = 2;
    public int MAX_HULLS_PER_CRUISER = 2;
    public int MAX_HULLS_PER_CAPITAL = 1;
    public float MAX_STACKS_PER_WEAPON = 0.5f;

    OmniFacSettings(String settingsFile)
    {
        reloadSettingsFromJSON(settingsFile);
    }

    public void reloadSettingsFromJSON(String filePath)
    {
        try
        {
            JSONObject settings = Global.getSettings().loadJSON(filePath);
            SHOW_ADDED_CARGO = settings.getBoolean("showAddedCargo");
            SHOW_ANALYSIS_COMPLETE = settings.getBoolean("showAnalysisComplete");
            SHOW_LIMIT_REACHED = settings.getBoolean("showLimitReached");
            REMOVE_BROKEN_GOODS = settings.getBoolean("removeBrokenGoods");
            SHIP_ANALYSIS_TIME_MOD = (float) settings.getDouble("shipAnalysisTimeMod");
            WEAPON_ANALYSIS_TIME_MOD = (float) settings.getDouble("weaponAnalysisTimeMod");
            SHIP_PRODUCTION_TIME_MOD = (float) settings.getDouble("shipProductionTimeMod");
            WEAPON_PRODUCTION_TIME_MOD = (float) settings.getDouble("weaponProductionTimeMod");
            REQUIRED_CREW = settings.getInt("requiredCrewToFunction");
            REQUIRED_SUPPLIES_PER_DAY = (float) settings.getDouble("requiredSuppliesPerDay");
            REQUIRED_FUEL_PER_DAY = (float) settings.getDouble("requiredFuelPerDay");
            MAX_HULLS_PER_FIGHTER = settings.getInt("maxHullsPerFighter");
            MAX_HULLS_PER_FRIGATE = settings.getInt("maxHullsPerFrigate");
            MAX_HULLS_PER_DESTROYER = settings.getInt("maxHullsPerDestroyer");
            MAX_HULLS_PER_CRUISER = settings.getInt("maxHullsPerCruiser");
            MAX_HULLS_PER_CAPITAL = settings.getInt("maxHullsPerCapital");
            MAX_STACKS_PER_WEAPON = (float) settings.getDouble("maxStacksPerWeapon");
        }
        catch (IOException ex)
        {
            Global.getLogger(OmniFacSettings.class).log(Level.ERROR,
                    "Failed to load " + filePath, ex);
            return;
        }
        catch (JSONException ex)
        {
            Global.getLogger(OmniFacSettings.class).log(Level.ERROR,
                    "Failed to parse " + filePath, ex);
            return;
        }

        Global.getLogger(OmniFacSettings.class).log(Level.INFO,
                "Loaded settings successfully");
    }

    public void setShowAddedCargo(boolean showAddedCargo)
    {
        SHOW_ADDED_CARGO = showAddedCargo;
    }

    public void setShowLimitReached(boolean showLimitReached)
    {
        SHOW_LIMIT_REACHED = showLimitReached;
    }

    public void setShowAnalysisComplete(boolean showAnalysisComplete)
    {
        SHOW_ANALYSIS_COMPLETE = showAnalysisComplete;
    }

    public void setRemoveBrokenGoods(boolean removeBrokenGoods)
    {
        REMOVE_BROKEN_GOODS = removeBrokenGoods;
    }

    public void setShipAnalysisTimeModifier(float modifier)
    {
        SHIP_ANALYSIS_TIME_MOD = modifier;
    }

    public void setWeaponAnalysisTimeModifier(float modifier)
    {
        WEAPON_ANALYSIS_TIME_MOD = modifier;
    }

    public void setShipProductionTimeModifier(float modifier)
    {
        SHIP_PRODUCTION_TIME_MOD = modifier;
    }

    public void setWeaponProductionTimeModifier(float modifier)
    {
        WEAPON_PRODUCTION_TIME_MOD = modifier;
    }

    public void setRequiredCrew(int requiredCrew)
    {
        REQUIRED_CREW = requiredCrew;
    }

    public void setRequiredSuppliesPerDay(float suppliesPerDay)
    {
        REQUIRED_SUPPLIES_PER_DAY = suppliesPerDay;
    }

    public void setRequiredFuelPerDay(float fuelPerDay)
    {
        REQUIRED_FUEL_PER_DAY = fuelPerDay;
    }

    public void setMaxHullsPerFighter(int maxHulls)
    {
        MAX_HULLS_PER_FIGHTER = maxHulls;
    }

    public void setMaxHullsPerFrigate(int maxHulls)
    {
        MAX_HULLS_PER_FRIGATE = maxHulls;
    }

    public void setMaxHullsPerDestroyer(int maxHulls)
    {
        MAX_HULLS_PER_DESTROYER = maxHulls;
    }

    public void setMaxHullsPerCruiser(int maxHulls)
    {
        MAX_HULLS_PER_CRUISER = maxHulls;
    }

    public void setMaxHullsPerCapital(int maxHulls)
    {
        MAX_HULLS_PER_CAPITAL = maxHulls;
    }

    public void setMaxStacksPerWeapon(float maxStacks)
    {
        MAX_STACKS_PER_WEAPON = maxStacks;
    }
}
