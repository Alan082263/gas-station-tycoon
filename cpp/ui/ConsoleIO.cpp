#include "ConsoleIO.h"

#include <iostream>

namespace console
{
    std::string askLine(const std::string &prompt)
    {
        std::cout << prompt;
        std::string input;
        std::getline(std::cin, input);
        return input;
    }

    int askInt(const std::string &prompt, int minimum, int maximum, int fallback)
    {
        std::string input = askLine(prompt);
        if (input.empty())
            return fallback;
        try
        {
            int value = std::stoi(input);
            return value >= minimum && value <= maximum ? value : fallback;
        }
        catch (...)
        {
            return fallback;
        }
    }

    double askDouble(const std::string &prompt, double minimum, double maximum, double fallback)
    {
        std::string input = askLine(prompt);
        if (input.empty())
            return fallback;
        try
        {
            double value = std::stod(input);
            return value >= minimum && value <= maximum ? value : fallback;
        }
        catch (...)
        {
            return fallback;
        }
    }

    bool askLitres(const std::string &prompt, double &value)
    {
        std::string input = askLine(prompt);
        try
        {
            value = std::stod(input);
            return true;
        }
        catch (...)
        {
            return false;
        }
    }
}
