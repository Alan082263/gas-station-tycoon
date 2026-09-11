#pragma once

#include "Business.h"
#include "DayOutlook.h"
#include "DayResult.h"
#include "GameConfig.h"

// Everything the console version prints or prompts for. Swapping this out
// for FTXUI later should not need a single change under src/.
namespace ui
{
    void printTitle(const Config &config, unsigned seed);

    void printDashboard(const Config &config, const Business &business,
                        const DayOutlook &outlook, int day);

    // [1] Set price ... [0] Quit -- returns the chosen number.
    int askAction();

    void orderFuel(Config &config, Business &business, const DayOutlook &outlook);

    void chooseUpgrade(Config &config, Business &business);

    void repayLoan(Business &business);

    void printDayResult(const Config &config, const Business &business, const DayResult &result);

    void printBankrupt(int day);

    void printFinale(const Config &config, const Business &business);
}
