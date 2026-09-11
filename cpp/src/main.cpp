#include "Business.h"
#include "DayOutlook.h"
#include "DayResult.h"
#include "DaySimulator.h"
#include "GameConfig.h"
#include "Market.h"

#include "ConsoleIO.h"
#include "ConsoleUI.h"

#include <iostream>
#include <random>
#include <string>
#include <vector>

int main(int argc, char **argv)
{
    Config config;
    unsigned seed = parseArguments(std::vector<std::string>(argv + 1, argv + argc), config);

    std::mt19937 rng(seed);
    Business business = Business::start(config);

    ui::printTitle(config, seed);

    for (int day = 1; day <= config.days; ++day)
    {
        DayOutlook outlook = dawn(config, day, rng);
        if (outlook.event.happened())
            config.wholesale = market::afterEvent(config, config.wholesale, outlook.event.wholesaleShift);

        ui::printDashboard(config, business, outlook, day);

        bool quit = false;
        while (true)
        {
            int choice = ui::askAction();
            if (choice == 0)
            {
                quit = true;
                break;
            }
            if (choice == 1)
                config.pumpPrice = console::askDouble("New price per litre: $", .10, 9.99, config.pumpPrice);
            if (choice == 2)
                ui::orderFuel(config, business, outlook);
            if (choice == 3)
                ui::chooseUpgrade(config, business);
            if (choice == 4)
                ui::repayLoan(business);
            if (choice == 5)
                break;
        }

        if (quit)
        {
            std::cout << "You hand back the keys.\n";
            return 0;
        }

        DayResult result = tradeDay(config, business, outlook, rng);
        ui::printDayResult(config, business, result);

        if (business.cash < 0)
        {
            ui::printBankrupt(day);
            return 0;
        }

        config.wholesale = market::overnight(config, config.wholesale, rng);
    }

    ui::printFinale(config, business);
    return 0;
}
