package org.lazywizard.omnifac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.campaign.MessageUtils;

// TODO: Merge with the submarket plugin after next save-breaking SS update
public class OmniFac implements EveryFrameScript
{
    private final Map<String, ShipData> shipData = new HashMap<>();
    private final Map<String, WeaponData> wepData = new HashMap<>();
    private final SectorEntityToken station;
    private transient CargoAPI cargo;
    private long lastHeartbeat;
    private int numHeartbeats = 0;
    private boolean warnedRequirements = true;

    //<editor-fold desc="Constructor">
    public OmniFac(SectorEntityToken station)
    {
        this.station = station;

        // Synchronize factory heartbeat to the start of the next day
        final CampaignClockAPI clock = Global.getSector().getClock();
        lastHeartbeat = new GregorianCalendar(clock.getCycle(),
                clock.getMonth() - 1, clock.getDay()).getTimeInMillis();

        getFactoryMap().put(station, this);
    }

    // Backwards compatibility with 1.10 saves
    // TODO: Remove after next save-breaking Starsector update
    public Object readResolve()
    {
        MarketAPI market = station.getMarket();
        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE))
        {
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
            ((StoragePlugin) market.getSubmarket(Submarkets.SUBMARKET_STORAGE)
                    .getPlugin()).setPlayerPaidToUnlock(true);
        }

        return this;
    }
    //</editor-fold>

    //<editor-fold desc="Static methods">
    public static boolean isFactory(SectorEntityToken station)
    {
        return getFactoryMap().keySet().contains(station);
    }

    public static OmniFac getFactory(SectorEntityToken station)
    {
        return getFactoryMap().get(station);
    }

    private static Map<SectorEntityToken, OmniFac> getFactoryMap()
    {
        Map<SectorEntityToken, OmniFac> allFactories
                = (Map<SectorEntityToken, OmniFac>) Global.getSector()
                .getPersistentData().get(Constants.FACTORY_DATA_ID);

        if (allFactories == null)
        {
            allFactories = new HashMap<>();
            Global.getSector().getPersistentData()
                    .put(Constants.FACTORY_DATA_ID, allFactories);
        }

        return allFactories;
    }

    public static List<SectorEntityToken> getFactories()
    {
        return new ArrayList<>(getFactoryMap().keySet());
    }

    public static String parseHullName(FleetMemberAPI ship)
    {
        if (ship.isFighterWing())
        {
            return ship.getSpecId();
        }

        return ship.getHullId();
    }
    //</editor-fold>

    //<editor-fold desc="Stack/ship analysis">
    public boolean isUnknownShip(FleetMemberAPI ship)
    {
        return !shipData.containsKey(parseHullName(ship));
    }

    public boolean isUnknownWeapon(CargoStackAPI stack)
    {
        // We only deal with weapons, not resources
        return (stack.isWeaponStack() && !wepData.containsKey(stack.getData()));
    }

    public boolean isRestrictedShip(FleetMemberAPI ship)
    {
        return OmniFacSettings.getRestrictedShips().contains(parseHullName(ship));
    }

    public boolean isRestrictedWeapon(CargoStackAPI stack)
    {
        return OmniFacSettings.getRestrictedWeapons().contains(stack.getData());
    }
    //</editor-fold>

    //<editor-fold desc="Heartbeat">
    public CargoAPI getCargo()
    {
        if (cargo == null)
        {
            cargo = station.getMarket().getSubmarket(
                    Constants.SUBMARKET_ID).getCargo();
        }

        return cargo;
    }

    private void heartbeat()
    {
        boolean metRequirements = true;
        CargoAPI cargo = getCargo();

        if (cargo.getTotalCrew() < OmniFacSettings.getRequiredCrew())
        {
            if (!warnedRequirements)
            {
                Global.getSector().getCampaignUI().addMessage("The " + station.getName()
                        + " needs " + (OmniFacSettings.getRequiredCrew() - cargo.getTotalCrew())
                        + " more crew to function.");
            }

            metRequirements = false;
        }

        if (cargo.getFuel() < OmniFacSettings.getRequiredFuelPerDay())
        {
            if (!warnedRequirements)
            {
                Global.getSector().getCampaignUI().addMessage("The " + station.getName()
                        + " is out of fuel. It requires " + OmniFacSettings.getRequiredFuelPerDay()
                        + " per day to function.");
            }

            metRequirements = false;
        }

        if (cargo.getSupplies() < OmniFacSettings.getRequiredSuppliesPerDay())
        {
            if (!warnedRequirements)
            {
                Global.getSector().getCampaignUI().addMessage("The " + station.getName()
                        + " is out of supplies. It requires " + OmniFacSettings.getRequiredSuppliesPerDay()
                        + " per day to function.");
            }

            metRequirements = false;
        }

        if (!metRequirements)
        {
            warnedRequirements = true;
            return;
        }

        warnedRequirements = false;
        cargo.removeSupplies(OmniFacSettings.getRequiredSuppliesPerDay());
        cargo.removeFuel(OmniFacSettings.getRequiredFuelPerDay());
        numHeartbeats++;

        List<String> addedShips = new ArrayList<>();
        List<String> addedWeps = new ArrayList<>();
        List<String> analyzedShips = new ArrayList<>();
        List<String> analyzedWeps = new ArrayList<>();
        List<String> hitLimit = new ArrayList<>();

        for (BaseData tmp : shipData.values())
        {
            if (!tmp.isAnalyzed())
            {
                if (numHeartbeats - tmp.getDaysToAnalyze() >= tmp.getLastUpdate())
                {
                    tmp.setAnalyzed(true);

                    if (OmniFacSettings.shouldShowAnalysisComplete())
                    {
                        analyzedShips.add(tmp.getName() + " ("
                                + tmp.getDaysToCreate() + "d)");
                    }
                }
            }
            else
            {
                if (numHeartbeats - tmp.getDaysToCreate() >= tmp.getLastUpdate())
                {
                    try
                    {
                        if (tmp.create())
                        {
                            if (OmniFacSettings.shouldShowAddedCargo())
                            {
                                addedShips.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (OmniFacSettings.shouldShowLimitReached() && !tmp.hasWarnedLimit())
                        {
                            hitLimit.add(tmp.getName());
                            tmp.setWarnedLimit(true);
                        }
                    }
                    catch (RuntimeException ex)
                    {
                        Global.getSector().getCampaignUI().addMessage(
                                "Failed to create ship '" + tmp.getName() + "' ("
                                + tmp.getId() + ")! Was a required mod disabled?");

                        if (OmniFacSettings.shouldRemoveBrokenGoods())
                        {
                            Global.getSector().getCampaignUI().addMessage(
                                    "Removed ship '" + tmp.getName() + "' from "
                                    + station.getName() + "'s memory banks.");
                            shipData.remove(tmp.getId());
                        }
                    }
                }
            }
        }

        for (BaseData tmp : wepData.values())
        {
            if (!tmp.isAnalyzed())
            {
                if (numHeartbeats - tmp.getDaysToAnalyze() >= tmp.getLastUpdate())
                {
                    tmp.setAnalyzed(true);

                    if (OmniFacSettings.shouldShowAnalysisComplete())
                    {
                        analyzedWeps.add(tmp.getName() + " ("
                                + tmp.getDaysToCreate() + "d)");
                    }
                }
            }
            else
            {
                if (numHeartbeats - tmp.getDaysToCreate() >= tmp.getLastUpdate())
                {
                    try
                    {
                        if (tmp.create())
                        {
                            if (OmniFacSettings.shouldShowAddedCargo())
                            {
                                addedWeps.add(tmp.getName() + " (" + tmp.getTotal()
                                        + "/" + tmp.getLimit() + ")");
                            }
                        }
                        else if (OmniFacSettings.shouldShowLimitReached() && !tmp.hasWarnedLimit())
                        {
                            hitLimit.add(tmp.getName());
                            tmp.setWarnedLimit(true);
                        }
                    }
                    catch (RuntimeException ex)
                    {
                        Global.getSector().getCampaignUI().addMessage(
                                "Failed to create weapon '" + tmp.getName() + "' ("
                                + tmp.getId() + ")! Was a required mod disabled?");

                        if (OmniFacSettings.shouldRemoveBrokenGoods())
                        {
                            Global.getSector().getCampaignUI().addMessage(
                                    "Removed weapon '" + tmp.getName() + "' from "
                                    + station.getName() + "'s memory banks.");
                            wepData.remove(tmp.getId());
                        }
                    }
                }
            }
        }

        if (OmniFacSettings.shouldShowAddedCargo())
        {
            if (!addedShips.isEmpty())
            {
                Collections.sort(addedShips);
                MessageUtils.showMessage("The " + station.getName()
                        + " has produced the following ships:",
                        CollectionUtils.implode(addedShips) + ".", true);
            }
            if (!addedWeps.isEmpty())
            {
                Collections.sort(addedWeps);
                MessageUtils.showMessage("The " + station.getName()
                        + " has produced the following weapons:",
                        CollectionUtils.implode(addedWeps) + ".", true);
            }
        }

        if (OmniFacSettings.shouldShowLimitReached() && !hitLimit.isEmpty())
        {
            Collections.sort(hitLimit);
            MessageUtils.showMessage("The " + station.getName()
                    + " has reached its limit for the following goods:",
                    CollectionUtils.implode(hitLimit) + ".", true);
        }

        if (OmniFacSettings.shouldShowAnalysisComplete())
        {
            if (!analyzedShips.isEmpty())
            {
                Collections.sort(analyzedShips);
                MessageUtils.showMessage("The " + station.getName()
                        + " has started production for the following ships:",
                        CollectionUtils.implode(analyzedShips) + ".", true);
            }
            if (!analyzedWeps.isEmpty())
            {
                Collections.sort(analyzedWeps);
                MessageUtils.showMessage("The " + station.getName()
                        + " has started production for the following weapons:",
                        CollectionUtils.implode(analyzedWeps) + ".", true);
            }
        }
    }

    public boolean checkCargo()
    {
        boolean newItem = false;
        CargoAPI cargo = getCargo();
        List<String> newShips = new ArrayList<>();
        List<String> blockedShips = new ArrayList<>();
        List<String> newWeps = new ArrayList<>();
        List<String> blockedWeps = new ArrayList<>();

        for (FleetMemberAPI ship : cargo.getMothballedShips().getMembersListCopy())
        {
            if (isRestrictedShip(ship))
            {
                blockedShips.add(ship.getHullSpec().getHullName());
            }
            else if (isUnknownShip(ship))
            {
                newItem = true;
                String id = parseHullName(ship);
                ShipData tmp = new ShipData(ship, this);

                if (OmniFacSettings.getShipAnalysisTimeMod() == 0f)
                {
                    tmp.setAnalyzed(true);
                    newShips.add(tmp.getName() + " ("
                            + tmp.getDaysToCreate() + "d)");
                }
                else
                {
                    newShips.add(tmp.getName() + " ("
                            + tmp.getDaysToAnalyze() + "d)");
                }

                shipData.put(id, tmp);

                // Add all weapons on this ship to the station's cargo
                if (!ship.isFighterWing())
                {
                    for (String slot : ship.getVariant().getNonBuiltInWeaponSlots())
                    {
                        cargo.addWeapons(ship.getVariant().getWeaponId(slot), 1);
                    }
                }

                cargo.getMothballedShips().removeFleetMember(ship);
            }
        }

        for (CargoStackAPI stack : cargo.getStacksCopy())
        {
            if (isRestrictedWeapon(stack))
            {
                blockedWeps.add(stack.getDisplayName());
            }
            else if (isUnknownWeapon(stack))
            {
                newItem = true;
                WeaponData tmp = new WeaponData(stack, this);

                if (OmniFacSettings.getWeaponAnalysisTimeMod() == 0f)
                {
                    tmp.setAnalyzed(true);
                    newWeps.add(tmp.getName() + " ("
                            + tmp.getDaysToCreate() + "d)");
                }
                else
                {
                    newWeps.add(tmp.getName() + " ("
                            + tmp.getDaysToAnalyze() + "d)");
                }

                wepData.put((String) stack.getData(), tmp);
                cargo.removeWeapons((String) stack.getData(), 1);
            }
        }

        if (!newShips.isEmpty())
        {
            Collections.sort(newShips);
            if (OmniFacSettings.getShipAnalysisTimeMod() == 0f)
            {
                MessageUtils.showMessage("New ship blueprints added to the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newShips) + ".", true);
            }
            else
            {
                MessageUtils.showMessage("The following ships are being"
                        + " disassembled and analyzed by the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newShips) + ".", true);
            }
        }

        if (!newWeps.isEmpty())
        {
            Collections.sort(newWeps);
            if (OmniFacSettings.getWeaponAnalysisTimeMod() == 0f)
            {
                MessageUtils.showMessage("New weapon blueprints added to the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newWeps) + ".", true);
            }
            else
            {
                MessageUtils.showMessage("The following weapons are being"
                        + " disassembled and analyzed by the "
                        + station.getName() + ":",
                        CollectionUtils.implode(newWeps) + ".", true);
            }
        }

        if (!blockedShips.isEmpty())
        {
            Collections.sort(blockedShips);
            MessageUtils.showMessage("The " + station.getName()
                    + " is unable to replicate the following ships:",
                    CollectionUtils.implode(blockedShips) + ".", true);
        }

        if (!blockedWeps.isEmpty())
        {
            Collections.sort(blockedWeps);
            MessageUtils.showMessage("The " + station.getName()
                    + " is unable to replicate the following weapons:",
                    CollectionUtils.implode(blockedWeps) + ".", true);
        }

        return newItem;
    }

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return false;
    }

    @Override
    public void advance(float amount)
    {
        CampaignClockAPI clock = Global.getSector().getClock();

        if (clock.getElapsedDaysSince(lastHeartbeat) >= 1f)
        {
            lastHeartbeat = clock.getTimestamp();
            heartbeat();

            if (checkCargo())
            {
                warnedRequirements = false;
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Internal data types">
    private static interface BaseData
    {
        public int getDaysToAnalyze();

        public int getDaysToCreate();

        public int getLastUpdate();

        public String getName();

        public String getId();

        public int getTotal();

        public int getLimit();

        public boolean hasWarnedLimit();

        public void setWarnedLimit(boolean hasWarned);

        public boolean isAnalyzed();

        public void setAnalyzed(boolean isAnalyzed);

        public boolean create();
    }

    private static class ShipData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        FleetMemberType type;
        int fp, size, lastUpdate;
        boolean warnedLimit = false, isAnalyzed = false;

        ShipData(FleetMemberAPI ship, OmniFac factory)
        {
            fac = factory;
            id = parseHullName(ship);
            displayName = ship.getHullSpec().getHullName();
            type = ship.getType();
            fp = ship.getFleetPointCost();
            size = ship.getHullSpec().getHullSize().ordinal();
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) Math.max(1f,
                    getDaysToCreate() * OmniFacSettings.getShipAnalysisTimeMod());
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(((fp * size) / 2f) * OmniFacSettings.getShipProductionTimeMod(),
                    size * 3f);
        }

        @Override
        public int getLastUpdate()
        {
            return lastUpdate;
        }

        @Override
        public String getName()
        {
            return displayName;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public int getTotal()
        {
            int total = 0;

            for (FleetMemberAPI tmp : fac.getCargo().getMothballedShips().getMembersListCopy())
            {
                if (id.equals(parseHullName(tmp)))
                {
                    total++;
                }
            }

            return total;
        }

        @Override
        public int getLimit()
        {
            switch (size)
            {
                case 1:
                    return OmniFacSettings.getMaxHullsPerFighter();
                case 2:
                    return OmniFacSettings.getMaxHullsPerFrigate();
                case 3:
                    return OmniFacSettings.getMaxHullsPerDestroyer();
                case 4:
                    return OmniFacSettings.getMaxHullsPerCruiser();
                case 5:
                    return OmniFacSettings.getMaxHullsPerCapital();
                default:
                    return 0;
            }
        }

        @Override
        public boolean hasWarnedLimit()
        {
            return warnedLimit;
        }

        @Override
        public void setWarnedLimit(boolean hasWarned)
        {
            warnedLimit = hasWarned;
        }

        @Override
        public boolean isAnalyzed()
        {
            return isAnalyzed;
        }

        @Override
        public void setAnalyzed(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public boolean create()
        {
            lastUpdate = fac.numHeartbeats;

            if (getTotal() >= getLimit())
            {
                return false;
            }

            warnedLimit = false;
            fac.getCargo().addMothballedShip(type, id
                    + (type.equals(FleetMemberType.FIGHTER_WING) ? "" : "_Hull"), null);
            return true;
        }
    }

    private static class WeaponData implements BaseData
    {
        OmniFac fac;
        String id, displayName;
        float size;
        int lastUpdate, stackSize;
        boolean warnedLimit = false, isAnalyzed = false;

        WeaponData(CargoStackAPI stack, OmniFac factory)
        {
            fac = factory;
            id = (String) stack.getData();
            displayName = stack.getDisplayName();
            size = stack.getCargoSpacePerUnit();
            stackSize = (int) stack.getMaxSize();
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public int getDaysToAnalyze()
        {
            return (int) Math.max(1f,
                    getDaysToCreate() * OmniFacSettings.getWeaponAnalysisTimeMod());
        }

        @Override
        public int getDaysToCreate()
        {
            return (int) Math.max(size * OmniFacSettings.getWeaponProductionTimeMod(), 1f);
        }

        @Override
        public int getLastUpdate()
        {
            return lastUpdate;
        }

        @Override
        public String getName()
        {
            return displayName;
        }

        @Override
        public String getId()
        {
            return id;
        }

        @Override
        public int getTotal()
        {
            return fac.getCargo().getNumWeapons(id);
        }

        @Override
        public int getLimit()
        {
            return (int) (stackSize * OmniFacSettings.getMaxStacksPerWeapon());
        }

        @Override
        public boolean hasWarnedLimit()
        {
            return warnedLimit;
        }

        @Override
        public void setWarnedLimit(boolean hasWarned)
        {
            warnedLimit = hasWarned;
        }

        @Override
        public boolean isAnalyzed()
        {
            return isAnalyzed;
        }

        @Override
        public void setAnalyzed(boolean isAnalyzed)
        {
            this.isAnalyzed = isAnalyzed;
            lastUpdate = fac.numHeartbeats;
        }

        @Override
        public boolean create()
        {
            lastUpdate = fac.numHeartbeats;

            if (getTotal() >= getLimit())
            {
                return false;
            }

            warnedLimit = false;
            fac.getCargo().addWeapons(id, 1);
            return true;
        }
    }
    //</editor-fold>
}
