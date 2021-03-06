#Author: your.email@your.domain.com
#Keywords Summary :
#Feature: List of scenarios.
#Scenario: Business rule through list of steps with arguments.
#Given: Some precondition step
#When: Some key actions
#Then: To observe outcomes or validation
#And,But: To enumerate more Given,When,Then steps
#Scenario Outline: List of steps for data-driven as an Examples and <placeholder>
#Examples: Container for s table
#Background: List of steps run before each of the scenarios
#""" (Doc Strings)
#| (Data Tables)
#@ (Tags/Labels):To group Scenarios
#<> (placeholder)
#""
## (Comments)
#Sample Feature Definition Template
Feature: Single account sell only

  Background: 
    Given an empty rebalance state
    And config:
      | maximumTolerableCash          |  0.99 |
      | maximumTolerableVarianceRatio | 0.005 |
      | overSizeQuoteRatio            |   0.2 |
    And a portfolio portfolio1 with targets:
      | instrumentId | ratio |
      | instrument1  |   0.4 |
      | instrument2  |   0.6 |
    And fund fund1 with portfolio portfolio1
    And fund fund1 has assets:
      | instrumentId | quantity |
      | instrument1  |       40 |
      | instrument2  |       60 |

  Scenario: Market Prices are requested
    Given fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.0 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market prices are requested for:
      | instrumentId |
      | instrument1  |
      | instrument2  |

  Scenario: Market Prices that are there are not requested
    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  | 0.9 | 1.1 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.0 available to invest
    When the rebalancer runs
    Then there are no allocations
    And rebalancer is not done
    And market prices are requested for:
      | instrumentId |
      | instrument2  |

  Scenario: even small amounts cause trade
    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  | 0.9 | 1.1 |
      | instrument1  | 0.8 | 1.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -0.1 available to invest
    When the rebalancer runs
    Then there are no allocations
    And rebalancer is not done

  #TODO: for very small amounts we should only trade one instrument
  Scenario: Request of Quotes. at 40%-60% split with asks 1,2 100GBP will be divested \
    as 25GBP,75GBP quotes are required to buy 25,37.5 +20%+roundup = 30,45

    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  | 1.0 | 1.1 |
      | instrument2  | 2.0 | 2.1 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market quotes are requested for:
      | instrumentId | quantity |
      | instrument1  |      -30 |
      | instrument2  |      -45 |
    And rebalancer is not done

  Scenario: Request of Trades. at 40%-60% split with asks 1,2 100GBP will be divested
    as 25GBP,75GBP quotes are required to sell 25,37.5(37 to the market 0.5 to the fractional account

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 1.0 | 1.1 |
      | quote2a | instrument2  | 2.0 | 2.1 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market trades are requested for:
      | quoteId | instrumentId | price | quantity |
      | quote1a | instrument1  |   1.0 |      -25 |
      | quote2a | instrument2  |   2.0 |      -37 |
    And rebalancer is not done

  Scenario: Request of Trades with fractional account flush.
    At 40%-60% split with asks 1,2 100GBP will be divested
    as 25GBP,75GBP quotes are required to sell 25,37.5 (38 to the market topping up from the fractional account

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 1.0 | 1.1 |
      | quote2a | instrument2  | 2.0 | 2.1 |
    And fractional account with:
      | instrumentId | quantity |
      | instrument1  |      0.5 |
      | instrument2  |      0.5 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market trades are requested for:
      | quoteId | instrumentId | price | quantity |
      | quote1a | instrument1  |   1.0 |      -25 |
      | quote2a | instrument2  |   2.0 |      -38 |
    And rebalancer is not done

  Scenario: Request of Trades with fractional account having enough space.
    At 40%-60% split with asks 1,2 100GBP will be divested
    as 25GBP,75GBP quotes are required to sell 25,37.5 (37 to the market and 0.5 to frac account)

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 1.0 | 1.1 |
      | quote2a | instrument2  | 2.0 | 2.1 |
    And fractional account with:
      | instrumentId | quantity  |
      | instrument1  |       0.5 |
      | instrument2  | 0.4999999 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market trades are requested for:
      | quoteId | instrumentId | price | quantity |
      | quote1a | instrument1  |   1.0 |      -25 |
      | quote2a | instrument2  |   2.0 |      -37 |
    And rebalancer is not done

  Scenario: Only sell Allocation: excess sold to fractional. at 40%-60% split with asks 1,2 100GBP will be dicested
    as 25GBP,75GBP Control account holdings get allocated, and left over moved to fractional account

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask  |
      | quote1a | instrument1  | 1.0 | 1.01 |
      | quote2a | instrument2  | 2.0 | 2.01 |
    And control account holdings:
      | instrumentId | quantity | price |
      | instrument1  |      -25 |   1.0 |
      | instrument2  |      -37 |   2.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |            -25 |   1.0 |
      | fund1 | instrument2  |          -37.5 |   2.0 |
      |       | instrument2  |            0.5 |   2.0 |
    And rebalancer is done

  Scenario: Only Sell Allocation FractionalOnly:. at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings get allocated, straight from fractional account

    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  |  89 | 101 |
      | instrument2  |  79 | 199 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 100 | 101 |
      | quote2a | instrument2  | 200 | 201 |
    And fractional account with:
      | instrumentId | quantity |
      | instrument1  |      0.5 |
      | instrument2  |      0.5 |
    And control account holdings:
      | instrumentId | quantity | price |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |          -0.25 |   100 |
      | fund1 | instrument2  |         -0.375 |   200 |
      |       | instrument1  |           0.25 |   100 |
      |       | instrument2  |          0.375 |   200 |
    And rebalancer is done

  Scenario: Only Sell Allocation FractionalOnly (no holdings):. at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings get allocated, straight from fractional account

    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  |  89 | 101 |
      | instrument2  |  79 | 199 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 100 | 101 |
      | quote2a | instrument2  | 200 | 201 |
    And fractional account with:
      | instrumentId | quantity |
    And control account holdings:
      | instrumentId | quantity | price |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |          -0.25 |   100 |
      | fund1 | instrument2  |         -0.375 |   200 |
      |       | instrument1  |           0.25 |   100 |
      |       | instrument2  |          0.375 |   200 |
    And rebalancer is done

  Scenario: Only Sell Allocation Fractional has too much and we need to sell.
    At 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings get allocated, straight from fractional account

    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  |  89 | 101 |
      | instrument2  |  79 | 199 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 100 | 101 |
      | quote2a | instrument2  | 200 | 201 |
    And fractional account with:
      | instrumentId | quantity |
      | instrument2  |    0.625 |
    And control account holdings:
      | instrumentId | quantity | price |
      | instrument2  |       -1 |   200 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |          -0.25 |   100 |
      | fund1 | instrument2  |         -0.375 |   200 |
      |       | instrument1  |           0.25 |   100 |
      |       | instrument2  |         -0.625 |   200 |
    And rebalancer is done

  Scenario: Only Sell Allocation, TradeRequest: Fractional has too much and we need to sell.
    At 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings get allocated, straight from fractional account

    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  |  89 | 101 |
      | instrument2  |  79 | 199 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  | 100 | 101 |
      | quote2a | instrument2  | 200 | 201 |
    And fractional account with:
      | instrumentId | quantity |
      | instrument2  |    0.625 |
    And control account holdings:
      | instrumentId | quantity | price |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then market trades are requested for:
      | quoteId | instrumentId | price | quantity |
      | quote2a | instrument2  |   200 |       -1 |
    And there are no allocations
    And rebalancer is not done

  Scenario: Only Sell Allocation with fractional topup: at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings get allocated, and left over moved to fractional account

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask  |
      | quote1a | instrument1  | 1.0 | 1.01 |
      | quote2a | instrument2  | 2.0 | 2.01 |
    And control account holdings:
      | instrumentId | quantity | price |
      | instrument1  |      -25 |   1.0 |
      | instrument2  |      -37 |   2.0 |
    And fractional account with:
      | instrumentId | quantity  |
      | instrument1  |       0.5 |
      | instrument2  | 0.4999999 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has -100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |            -25 |   1.0 |
      | fund1 | instrument2  |          -37.5 |   2.0 |
      |       | instrument2  |            0.5 |   2.0 |
    And rebalancer is done
