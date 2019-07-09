package org.fengfei.lanproxy.common;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class LangUtilTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Rule public final Timeout globalTimeout = new Timeout(10000);

  /* testedClasses: LangUtil */
  // Test written by Diffblue Cover.
  @Test
  public void parseBooleanInputNotNullFalseOutputFalse() {

    // Arrange
    final Object value = "2";
    final boolean defaultValue = false;

    // Act
    final boolean actual = LangUtil.parseBoolean(value, defaultValue);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseBooleanInputNotNullFalseOutputTrue() {

    // Arrange
    final Boolean value = new Boolean(true);
    final boolean defaultValue = false;

    // Act
    final boolean actual = LangUtil.parseBoolean(value, defaultValue);

    // Assert result
    Assert.assertTrue(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseBooleanInputNotNullOutputFalse() {

    // Arrange
    final Boolean value = new Boolean(false);

    // Act
    final Boolean actual = LangUtil.parseBoolean(value);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void parseBooleanInputNotNullOutputFalse2() {

    // Arrange
    final Object value = "\'";

    // Act
    final Boolean actual = LangUtil.parseBoolean(value);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseBooleanInputNullFalseOutputFalse() {

    // Arrange
    final Object value = null;
    final boolean defaultValue = false;

    // Act
    final boolean actual = LangUtil.parseBoolean(value, defaultValue);

    // Assert result
    Assert.assertFalse(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseBooleanInputNullOutputNull() {

    // Arrange
    final Object value = null;

    // Act
    final Boolean actual = LangUtil.parseBoolean(value);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseDoubleInputNotNullNotNullOutputPositive() {

    // Arrange
    final Double value = new Double(2.0);
    final Double defaultValue = new Double(2.0);

    // Act
    final Double actual = LangUtil.parseDouble(value, defaultValue);

    // Assert result
    Assert.assertEquals(2.0, actual, 0.0);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseDoubleInputNotNullOutputPositive() {

    // Arrange
    final Double value = new Double(2.0);

    // Act
    final Double actual = LangUtil.parseDouble(value);

    // Assert result
    Assert.assertEquals(2.0, actual, 0.0);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseDoubleInputNullNullOutputNull() {

    // Arrange
    final Object value = null;
    final Double defaultValue = null;

    // Act
    final Double actual = LangUtil.parseDouble(value, defaultValue);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseDoubleInputNullOutputNull() {

    // Arrange
    final Object value = null;

    // Act
    final Double actual = LangUtil.parseDouble(value);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseIntInputNotNullOutputPositive() {

    // Arrange
    final Object value = "3";

    // Act
    final Integer actual = LangUtil.parseInt(value);

    // Assert result
    Assert.assertEquals(new Integer(3), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseIntInputNotNullZeroOutputPositive() {

    // Arrange
    final Object value = "2";
    final Integer defaultValue = 0;

    // Act
    final Integer actual = LangUtil.parseInt(value, defaultValue);

    // Assert result
    Assert.assertEquals(new Integer(2), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseIntInputNullNullOutputNull() {

    // Arrange
    final Object value = null;
    final Integer defaultValue = null;

    // Act
    final Integer actual = LangUtil.parseInt(value, defaultValue);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseIntInputNullOutputNull() {

    // Arrange
    final Object value = null;

    // Act
    final Integer actual = LangUtil.parseInt(value);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseIntInputPositiveOutputPositive() {

    // Arrange
    final Object value = 1;

    // Act
    final Integer actual = LangUtil.parseInt(value);

    // Assert result
    Assert.assertEquals(new Integer(1), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseIntInputZeroZeroOutputZero() {

    // Arrange
    final Object value = 0;
    final Integer defaultValue = 0;

    // Act
    final Integer actual = LangUtil.parseInt(value, defaultValue);

    // Assert result
    Assert.assertEquals(new Integer(0), actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void parseLongInputNotNullNegativeOutputPositive() {

    // Arrange
    final Object value = "1";
    final Long defaultValue = -100L;

    // Act
    final Long actual = LangUtil.parseLong(value, defaultValue);

    // Assert result
    Assert.assertEquals(new Long(1L), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseLongInputNotNullNotNullOutputPositive() {

    // Arrange
    final Long value = new Long(476_164L);
    final Long defaultValue = new Long(475_718L);

    // Act
    final Long actual = LangUtil.parseLong(value, defaultValue);

    // Assert result
    Assert.assertEquals(new Long(476_164L), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseLongInputNotNullOutputPositive() {

    // Arrange
    final Long value = new Long(776L);

    // Act
    final Long actual = LangUtil.parseLong(value);

    // Assert result
    Assert.assertEquals(new Long(776L), actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void parseLongInputNotNullOutputPositive2() {

    // Arrange
    final Object value = "3";

    // Act
    final Long actual = LangUtil.parseLong(value);

    // Assert result
    Assert.assertEquals(new Long(3L), actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseLongInputNullNullOutputNull() {

    // Arrange
    final Object value = null;
    final Long defaultValue = null;

    // Act
    final Long actual = LangUtil.parseLong(value, defaultValue);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void parseLongInputNullOutputNull() {

    // Arrange
    final Object value = null;

    // Act
    final Long actual = LangUtil.parseLong(value);

    // Assert result
    Assert.assertNull(actual);
  }
}
