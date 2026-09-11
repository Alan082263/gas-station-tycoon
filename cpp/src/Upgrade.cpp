#include "Upgrade.h"

std::vector<Upgrade> defaultUpgrades()
{
    return {
        {"Extra pump", "+1 pump, so 38 more fills a day", 4200, 0, 0, 0, false},
        {"Bigger tank", "+8,000 L of storage", 3200, 0, 0, 0, false},
        {"Forecourt canopy", "+8% demand and softer bad-weather penalties", 1500, 0, .08, 0, false},
        {"Convenience store", "$3.60 of shop sales per customer", 4800, 3.60, .04, 0, false},
        {"Car wash", "$1.80 per customer", 2600, 1.80, .02, 0, false},
        {"Loyalty app", "Regulars mind a high price less", 3000, 0, .03, 3, false}};
}
