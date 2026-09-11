#include "GameConfig.h"

#include <algorithm>
#include <random>

void applyHard(Config &config)
{
    config.cash = 5000;
    config.debt = 20000;
    config.target = 12000;
    config.traffic = 215;
    config.sensitivity = 16;
    config.volatility = .05;
    config.eventChance = .40;
    config.fixedCosts = 260;
}

void applyEasy(Config &config)
{
    config.cash = 12000;
    config.debt = 10000;
    config.target = 34000;
    config.traffic = 285;
    config.sensitivity = 11;
    config.volatility = .025;
    config.eventChance = .22;
    config.fixedCosts = 180;
}

unsigned parseArguments(const std::vector<std::string> &arguments, Config &config)
{
    unsigned seed = std::random_device{}();

    for (const std::string &argument : arguments)
    {
        if (argument == "--hard")
            applyHard(config);
        else if (argument == "--easy")
            applyEasy(config);
        else if (argument.rfind("--seed=", 0) == 0)
            seed = static_cast<unsigned>(std::stoul(argument.substr(7)));
        else if (argument.rfind("--days=", 0) == 0)
            config.days = std::max(1, std::stoi(argument.substr(7)));
    }

    return seed;
}
