#pragma once

#include <string>
#include <vector>

struct Upgrade
{
    std::string label;
    std::string description;
    double cost = 0;
    double customerIncome = 0;
    double demandBonus = 0;
    double sensitivityReduction = 0;
    bool owned = false;
};

// The catalogue every new game starts with.
std::vector<Upgrade> defaultUpgrades();
