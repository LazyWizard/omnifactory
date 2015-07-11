package org.lazywizard.omnifactory.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.omnifactory.Blueprint;
import org.lazywizard.omnifactory.Blueprint.BlueprintType;
import org.lazywizard.omnifactory.BlueprintMaster;

public class OmniBalance implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        List<Blueprint> datas = new ArrayList<>(
                BlueprintMaster.getAllBlueprints(BlueprintType.SHIP).values());
        datas.addAll(BlueprintMaster.getAllBlueprints(BlueprintType.WING).values());
        datas.addAll(BlueprintMaster.getAllBlueprints(BlueprintType.WEAPON).values());
        Collections.sort(datas, new BlueprintSorter());
        String line = String.format("|%60s|\n", "").replace(' ', '-');
        StringBuilder output = new StringBuilder("\n" + line);
        output.append(String.format(
                "| %-26s | %-6s | %4s | %5s | %5s |\n",
                "id", "type", "scan", "build", "limit")).append(line);
        for (Blueprint data : datas)
        {
            output.append(String.format("| %-26.26s | %-6.6s | %4d | %5d | %5d |\n",
                    data.getId(), data.getType(), data.getDaysToAnalyze(),
                    data.getDaysToCreate(), data.getLimit()));
        }
        output.append(line);

        Console.showMessage("Posted a blueprint balance report to starsector.log.");
        Global.getLogger(OmniBalance.class).log(Level.INFO, output.toString());
        return CommandResult.SUCCESS;
    }

    private static class BlueprintSorter implements Comparator<Blueprint>
    {
        @Override
        public int compare(Blueprint o1, Blueprint o2)
        {
            if (o1.getType() == o2.getType())
            {
                if (o1.getDaysToCreate() == o2.getDaysToCreate())
                {
                    return o1.getId().compareTo(o2.getId());
                }

                return Integer.compare(o1.getDaysToCreate(), o2.getDaysToCreate());
            }

            return o1.getType().compareTo(o2.getType());
        }
    }
}
