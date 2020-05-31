package io.github.steliospaps.experimental.investment.invest.bdd;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

import io.github.steliospaps.experimental.investment.invest.InvestmentPortfolioApplication;

/**
 * Class to use spring application context while running cucumber
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = InvestmentPortfolioApplication.class, loader = SpringBootContextLoader.class)
public class CucumberSpringContextConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(CucumberSpringContextConfiguration.class);

  /**
   * Need this method so the cucumber will recognize this class as glue and load spring context configuration
   */
  @Before
  public void setUp() {
    LOG.info("-------------- Spring Context Initialized For Executing Cucumber Tests --------------");
  }
}