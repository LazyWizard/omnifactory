package org.lazywizard.omnifac;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

public class OmniFacSubmarket extends StoragePlugin
{
    @Override
    public void init(SubmarketAPI submarket)
    {
        super.init(submarket);
        super.setPlayerPaidToUnlock(true);
    }

    @Override
    public String getName()
    {
        return Constants.STATION_NAME;
    }

    @Override
    public String getBuyVerb()
    {
        return "Buy";
    }

    @Override
    public String getSellVerb()
    {
        return "Leave";
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action)
    {
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action)
    {
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action)
    {
        return false;
    }

    @Override
    public float getTariff()
    {
        return OmniFacSettings.getTariff();
    }

    @Override
    public boolean isFreeTransfer()
    {
        return false;
    }
}
