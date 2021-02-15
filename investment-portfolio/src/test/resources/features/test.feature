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
Feature: Simple rebalance actions
  I want to use this template for my feature file

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
    And fractional account with:
      | instrumentId | quantity |
      | instrument1  |      0.0 |
      | instrument2  |      0.0 |

  Scenario: Market Prices are requested
    Given fund fund1 with portfolio portfolio1
    And that fund fund1 has 100.0 available to invest
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
    And that fund fund1 has 100.0 available to invest
    When the rebalancer runs
    Then there are no allocations
    And rebalancer is not done
    And market prices are requested for:
      | instrumentId |
      | instrument2  |

  Scenario: amount to invest is too small to trigger trade
    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  | 0.9 | 1.1 |
      | instrument1  | 0.8 | 1.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has 0.99 available to invest
    When the rebalancer runs
    Then there are no allocations
    And rebalancer is done

  Scenario: Request of Quotes. at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP quotes are required to buy 25,37.5 +20%+roundup = 30,45

    Given market prices:
      | instrumentId | bid | ask |
      | instrument1  | 0.9 | 1.0 |
      | instrument2  | 0.8 | 2.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has 100.00 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market quotes are requested for:
      | instrumentId | quantity |
      | instrument1  |       30 |
      | instrument2  |       45 |
    And rebalancer is not done

  Scenario: Request of Trades. at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP quotes are required to buy 25,37.5 +20%+roundup = 30,45

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  |     | 1.0 |
      | quote2a | instrument2  |     | 2.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has 100.00 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market trades are requested for:
      | quoteId | instrumentId | price | quantity |
      | quote1a | instrument1  |   1.0 |       25 |
      | quote2a | instrument2  |   2.0 |       38 |
    And rebalancer is not done

  Scenario: Only Buy Allocation:. at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings match exactly and they get allocated

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  |     | 1.0 |
      | quote2a | instrument2  |     | 2.0 |
    And control account holdings:
      | instrumentId | quantity | price |
      | instrument1  |       25 |   1.0 |
      | instrument2  |     37.5 |   2.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has 100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |             25 |   1.0 |
      | fund1 | instrument2  |           37.5 |   2.0 |
    And rebalancer is done

  Scenario: Only Buy Allocation:. at 40%-60% split with asks 1,2 100GBP will be invested \
    as 25GBP,75GBP Control account holdings get allocated, and left over moved to fractional account

    Given market prices:
      | instrumentId | bid  | ask  |
      | instrument1  | 0.89 | 1.01 |
      | instrument2  | 0.79 | 1.99 |
    And quotes:
      | quoteId | instrumentId | bid | ask |
      | quote1a | instrument1  |     | 1.0 |
      | quote2a | instrument2  |     | 2.0 |
    And control account holdings:
      | instrumentId | quantity | price |
      | instrument1  |       25 |   1.0 |
      | instrument2  |       38 |   2.0 |
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has 100.00 available to invest
    When the rebalancer runs
    Then there are no actions
    And there are allocations:
      | to    | instrumentId | quantity delta | price |
      | fund1 | instrument1  |             25 |   1.0 |
      | fund1 | instrument2  |           37.5 |   2.0 |
      |       | instrument2  |            0.5 |   2.0 |
    And rebalancer is done
