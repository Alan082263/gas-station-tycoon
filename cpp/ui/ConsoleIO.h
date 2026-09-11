#pragma once

#include <string>

// Reading answers off the terminal. Every prompt takes a fallback, so an
// empty line or a typo never stops the game.
namespace console
{
    std::string askLine(const std::string &prompt);

    int askInt(const std::string &prompt, int minimum, int maximum, int fallback);

    double askDouble(const std::string &prompt, double minimum, double maximum, double fallback);

    // Reads a litre count. Returns false if the input was not a number;
    // a negative answer means "as much as possible".
    bool askLitres(const std::string &prompt, double &value);
}
