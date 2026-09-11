#include "Market.h"

#include <algorithm>
#include <cmath>

namespace market
{
    double afterEvent(const Config &config, double wholesale, double shift)
    {
        return std::clamp(wholesale + shift, config.floor, config.ceiling);
    }

    double overnight(const Config &config, double wholesale, std::mt19937 &rng)
    {
        std::normal_distribution<double> shock(0, config.volatility);
        double drift = .15 * (config.wholesaleMean - wholesale);
        return std::clamp(wholesale + drift + shock(rng), config.floor, config.ceiling);
    }

    double customerShare(double pumpPrice, double referencePrice, double sensitivity)
    {
        double relative = (pumpPrice - referencePrice) / referencePrice;
        return 1.0 / (1.0 + std::exp(sensitivity * relative));
    }
}
