#pragma once

#include "GameConfig.h"
#include "Upgrade.h"

#include <string>
#include <vector>

// Everything the player owns and owes. The station's fuel and pumps live
// here too, so nothing has to be passed around by reference.
struct Business
{
    double cash = 0;
    double debt = 0;
    double tank = 0;
    int pumps = 0;
    std::vector<Upgrade> upgrades;

    static Business start(const Config &config);

    bool has(const std::string &label) const;

    // Totals across owned upgrades.
    double customerIncome() const;
    double demandBonus() const;
    double sensitivityReduction() const;

    double netWorth(double wholesale) const;

    // Why a purchase or a delivery was refused. Empty means it went through.
    enum class Refusal
    {
        None,
        AlreadyOwned,
        NotEnoughCash,
        NoDeliveryToday,
        BadAmount
    };

    // Buys upgrade `index` and applies its side effects. Does no printing:
    // the caller decides how to report the outcome.
    Refusal buyUpgrade(std::size_t index, Config &config);

    // The most fuel that fits and that the cash will cover.
    double affordableLitres(const Config &config, double wholesale) const;

    Refusal acceptDelivery(const Config &config, double wholesale, double litres, double &billed);

    void repay(double amount);
};
