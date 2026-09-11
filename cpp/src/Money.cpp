#include "Money.h"

#include <cmath>
#include <cstdio>

namespace
{
    // "1234.50" -> "1,234.50"
    std::string group(const std::string &plain)
    {
        std::size_t dot = plain.find('.');
        std::string whole = plain.substr(0, dot);
        std::string rest = (dot == std::string::npos) ? "" : plain.substr(dot);

        std::string out;
        int seen = 0;
        for (std::size_t i = whole.size(); i-- > 0;)
        {
            out.insert(out.begin(), whole[i]);
            if (++seen % 3 == 0 && i > 0)
                out.insert(out.begin(), ',');
        }
        return out + rest;
    }

    std::string toFixed(double value, int places)
    {
        char buffer[64];
        std::snprintf(buffer, sizeof buffer, "%.*f", places, value);
        return buffer;
    }
}

namespace money
{
    std::string cash(double amount)
    {
        bool negative = amount < 0;
        return (negative ? "-$" : "$") + group(toFixed(std::fabs(amount), 2));
    }

    std::string perLitre(double amount)
    {
        bool negative = amount < 0;
        return (negative ? "-$" : "$") + group(toFixed(std::fabs(amount), 3));
    }

    std::string litres(double amount)
    {
        return group(toFixed(amount, 0)) + " L";
    }
}
