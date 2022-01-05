package com.github.vitalibo.config;

import com.typesafe.config.Config;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigFactoryTest {

    @Test
    public void testLoadYaml() {
        Config actual = ConfigFactory.load("test.yaml");

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getInt("ints.fortyTwo"), 42);
        Assert.assertTrue(actual.hasPath("user.home"));
    }

    @Test
    public void testLoadHocon() {
        Config actual = ConfigFactory.load("test.conf");

        Assert.assertNotNull(actual);
        Assert.assertFalse(actual.hasPath("ints.fortyTwo"));
        Assert.assertTrue(actual.hasPath("user.home"));
        Assert.assertEquals(actual.getInt("foo.bar"), 123);
    }

    @Test
    public void testParseResourcesYaml() {
        Config actual = ConfigFactory.parseResources("test.yaml");

        Assert.assertNotNull(actual);
        Assert.assertEquals(actual.getInt("ints.fortyTwo"), 42);
        Assert.assertFalse(actual.hasPath("user.home"));
    }

    @Test
    public void testParseResourcesHocon() {
        Config actual = ConfigFactory.parseResources("test.conf");

        Assert.assertNotNull(actual);
        Assert.assertFalse(actual.hasPath("ints.fortyTwo"));
        Assert.assertFalse(actual.hasPath("user.home"));
        Assert.assertEquals(actual.getInt("foo.bar"), 123);
    }

}
