package io.github.steliospaps.experimental.investment.invest.bdd;

//import cucumber.api.CucumberOptions;
//import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * To run cucumber test.
 */
@RunWith(Cucumber.class)
@CucumberOptions(features = "classpath:features", //
		strict = true, //
		plugin = { "pretty", "json:target/cucumber-report.json" })
public class CucumberTest {

}
