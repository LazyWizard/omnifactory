package org.lazywizard.omnifac;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;

public class OmniFacSubmarket extends StoragePlugin
{
    private transient OmniFac fac;

    @Override
    public void init(SubmarketAPI submarket)
    {
        super.init(submarket);
        super.setPlayerPaidToUnlock(true);
    }

    public OmniFac getFactory()
    {
        if (fac == null)
        {
            fac = OmniFac.getFactory(this.market.getPrimaryEntity());
        }

        return fac;
    }

    @Override
    public String getName()
    {
        return Constants.STATION_NAME;
    }

    @Override
    public String getBuyVerb()
    {
        return (getTariff() > 0f ? "Buy" : "Take");
    }

    @Override
    public String getSellVerb()
    {
        return (getTariff() < 1f ? "Sell" : "Leave");
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action)
    {
        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action)
    {
        // Can't sell restricted or known weapons to the Omnifactory
        if (action == TransferAction.PLAYER_SELL)
        {
            return (getFactory().isRestrictedWeapon(stack)
                    || !getFactory().isUnknownWeapon(stack));
        }

        return false;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action)
    {
        // Can't sell restricted or known ships to the Omnifactory
        if (action == TransferAction.PLAYER_SELL)
        {
            return (getFactory().isRestrictedShip(member)
                    || !getFactory().isUnknownShip(member));
        }

        return false;
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action)
    {
        if (!stack.isWeaponStack() || getFactory().isRestrictedWeapon(stack))
        {
            return "Unable to replicate";
        }

        return "Blueprint already known";
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action)
    {
        return (getFactory().isRestrictedShip(member)
                ? "Unable to replicate"
                : "Blueprint already known");
    }

    @Override
    public float getTariff()
    {
        return OmniFacSettings.getOmnifactoryTariff();
    }

    @Override
    public boolean isFreeTransfer()
    {
        return false;
    }
}
