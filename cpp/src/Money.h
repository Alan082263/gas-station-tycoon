#pragma once

#include <string>

// Formatting for the money the game prints constantly.
//
// Cash is shown to 2 decimal places; fuel prices to 3, because a station
// really does price fuel at $1.559 a litre. Java's Money class draws the
// same distinction (cash / perLitre) -- keep using the right one.
namespace money
{
    // "$1,234.50", or "-$1,234.50" for negatives.
    std::string cash(double amount);

    // "$1.559" -- a tenth of a cent, the way fuel is priced.
    std::string perLitre(double amount);

    // "3,000 L"
    std::string litres(double amount);
}
