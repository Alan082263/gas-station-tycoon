#pragma once

#include "GameConfig.h"

#include <random>

// The wholesale price of fuel: shocked by events, and otherwise drifting
// back towards its long-run mean.
namespace market
{
    // Applies an event's jump, kept inside the floor/ceiling band.
    double afterEvent(const Config &config, double wholesale, double shift);

    // One night's drift: mean reversion plus a normal shock.
    double overnight(const Config &config, double wholesale, std::mt19937 &rng);

    // The share of passing traffic that stops, given how far the pump price
    // sits above or below the reference price. A logistic curve: the higher
    // `sensitivity`, the more sharply drivers react.
    double customerShare(double pumpPrice, double referencePrice, double sensitivity);
}
