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
	
  Scenario: Market Prices are requested
    Given a portfolio portfolio1 with targets:
    	|instrumentId|ratio|
    	|instrument1|0.4|
    	|instrument2|0.6|
    And fund fund1 with portfolio portfolio1
    And that fund fund1 has 100.0 available to invest
    When the rebalancer runs
    Then there are no allocations
    And market prices are requested for:
    	|instrumentId|
    	|instrument1|
    	|instrument2|
  