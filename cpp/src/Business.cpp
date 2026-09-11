#include "Business.h"

#include <algorithm>

Business Business::start(const Config &config)
{
    Business business;
    business.cash = config.cash;
    business.debt = config.debt;
    business.tank = config.fuel;
    business.pumps = config.pumps;
    business.upgrades = defaultUpgrades();
    return business;
}

bool Business::has(const std::string &label) const
{
    for (const Upgrade &upgrade : upgrades)
        if (upgrade.label == label && upgrade.owned)
            return true;
    return false;
}

double Business::customerIncome() const
{
    double total = 0;
    for (const Upgrade &upgrade : upgrades)
        if (upgrade.owned)
            total += upgrade.customerIncome;
    return total;
}

double Business::demandBonus() const
{
    double total = 0;
    for (const Upgrade &upgrade : upgrades)
        if (upgrade.owned)
            total += upgrade.demandBonus;
    return total;
}

double Business::sensitivityReduction() const
{
    double total = 0;
    for (const Upgrade &upgrade : upgrades)
        if (upgrade.owned)
            total += upgrade.sensitivityReduction;
    return total;
}

double Business::netWorth(double wholesale) const
{
    return cash + tank * wholesale - debt;
}

Business::Refusal Business::buyUpgrade(std::size_t index, Config &config)
{
    if (index >= upgrades.size())
        return Refusal::BadAmount;

    Upgrade &upgrade = upgrades[index];
    if (upgrade.owned)
        return Refusal::AlreadyOwned;
    if (cash < upgrade.cost)
        return Refusal::NotEnoughCash;

    upgrade.owned = true;
    cash -= upgrade.cost;

    if (upgrade.label == "Extra pump")
        ++pumps;
    if (upgrade.label == "Bigger tank")
        config.tankCapacity += 8000;

    return Refusal::None;
}

double Business::affordableLitres(const Config &config, double wholesale) const
{
    double room = tank < config.tankCapacity ? config.tankCapacity - tank : 0.0;
    double payable = std::max(0.0, (cash - config.deliveryFee) / wholesale);
    return std::min(room, payable);
}

Business::Refusal Business::acceptDelivery(const Config &config, double wholesale,
                                           double litres, double &billed)
{
    billed = 0;
    if (litres <= 0 || litres > affordableLitres(config, wholesale))
        return Refusal::BadAmount;

    tank += litres;
    billed = litres * wholesale + config.deliveryFee;
    cash -= billed;
    return Refusal::None;
}

void Business::repay(double amount)
{
    amount = std::min(amount, std::min(cash, debt));
    if (amount <= 0)
        return;
    cash -= amount;
    debt -= amount;
}
