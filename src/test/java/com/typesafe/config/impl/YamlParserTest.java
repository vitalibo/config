package com.typesafe.config.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigMemorySize;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigParseOptions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Period;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class YamlParserTest {

    private static final ConfigOrigin origin = SimpleConfigOrigin.newSimple("default");

    @Test
    public void testParseResourcesYamlSyntax() {
        ConfigObject actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults());

        Assert.assertNotNull(actual);
        String description = actual.origin().description();
        Assert.assertTrue(description.matches("test\\.yaml @ file:/.+/test\\.yaml: 0"));
    }

    @Test
    public void testParseResourcesYamlSyntaxInts() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertEquals(actual.getInt("ints.fortyTwo"), 42);
        Assert.assertEquals(actual.getInt("ints.fortyTwoAgain"), 42);
    }

    @Test
    public void testParseResourcesYamlSyntaxFloats() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertEquals(actual.getDouble("floats.fortyTwoPointOne"), 42.1);
        Assert.assertEquals(actual.getDouble("floats.fortyTwoPointOneAgain"), 42.1);
        Assert.assertEquals(actual.getDouble("floats.pointThirtyThree"), 0.33);
        Assert.assertEquals(actual.getDouble("floats.pointThirtyThreeAgain"), 0.33);
    }

    @Test
    public void testParseResourcesYamlSyntaxStrings() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertEquals(actual.getString("strings.abcd"), "abcd");
        Assert.assertEquals(actual.getString("strings.abcdAgain"), "abcd");
        Assert.assertEquals(actual.getString("strings.a"), "a");
        Assert.assertEquals(actual.getString("strings.b"), "b");
        Assert.assertEquals(actual.getString("strings.c"), "c");
        Assert.assertEquals(actual.getString("strings.d"), "d");
        Assert.assertEquals(actual.getString("strings.concatenated"), " null bar 42 baz true 3.14 hi,");
        Assert.assertEquals(actual.getString("strings.double"), "3.14");
        Assert.assertEquals(actual.getDouble("strings.double"), 3.14);
        Assert.assertEquals(actual.getString("strings.doubleStartingWithDot"), ".33");
        Assert.assertEquals(actual.getDouble("strings.doubleStartingWithDot"), 0.33);
        Assert.assertEquals(actual.getString("strings.number"), "57");
        Assert.assertEquals(actual.getInt("strings.number"), 57);
        Assert.assertEquals(actual.getString("strings.null"), "null");
        Assert.assertEquals(actual.getString("strings.true"), "true");
        Assert.assertTrue(actual.getBoolean("strings.true"));
        Assert.assertEquals(actual.getString("strings.yes"), "yes");
        Assert.assertTrue(actual.getBoolean("strings.yes"));
        Assert.assertEquals(actual.getString("strings.false"), "false");
        Assert.assertFalse(actual.getBoolean("strings.false"));
        Assert.assertEquals(actual.getString("strings.no"), "no");
        Assert.assertFalse(actual.getBoolean("strings.no"));
    }

    @Test
    public void testParseResourcesYamlSyntaxArrays() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertTrue(actual.getList("arrays.empty").isEmpty());
        Assert.assertEquals(actual.getIntList("arrays.ofInt"), Arrays.asList(1, 2, 3));
        Assert.assertEquals(actual.getStringList("arrays.ofString"), Arrays.asList("a", "b", "c"));
        Assert.assertEquals(actual.getDoubleList("arrays.ofDouble"), Arrays.asList(3.14, 4.14, 5.14));
        //TODO: how to check arrays.ofNull ?
        Assert.assertEquals(actual.getBooleanList("arrays.ofBoolean"), Arrays.asList(true, false));
        Assert.assertEquals(actual.getList("arrays.ofArray").size(), 3);
        Assert.assertEquals(actual.getList("arrays.ofArray").get(0).unwrapped(), Arrays.asList("a", "b", "c"));
        Assert.assertEquals(actual.getList("arrays.ofObject").size(), 3);
        Assert.assertEquals(actual.getList("arrays.ofObject").get(0).unwrapped(), new HashMap<String, Integer>() {{
            put("fortyTwo", 42);
            put("fortyTwoAgain", 42);
        }});
        Assert.assertEquals(actual.getStringList("arrays.firstElementNotASubst"), Arrays.asList("a", "b"));
    }

    @Test
    public void testParseResourcesYamlSyntaxNulls() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertTrue(actual.getIsNull("nulls.null"));
        Assert.assertTrue(actual.getIsNull("nulls.nullAgain"));
    }

    @Test
    public void testParseResourcesYamlSyntaxDurations() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertEquals(actual.getDuration("durations.second"), Duration.ofSeconds(1));
        Assert.assertEquals(actual.getDurationList("durations.secondsList"), Arrays.asList(
            Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofSeconds(4)));
        Assert.assertEquals(actual.getDuration("durations.secondAsNumber"), Duration.ofSeconds(1));
        Assert.assertEquals(actual.getDuration("durations.halfSecond"), Duration.ofMillis(500));
        Assert.assertEquals(actual.getDuration("durations.millis"), Duration.ofMillis(1));
        Assert.assertEquals(actual.getDuration("durations.micros"), Duration.ofMillis(2));
        Assert.assertEquals(actual.getDuration("durations.largeNanos"), Duration.ofNanos(4878955355435272204L));
        Assert.assertEquals(actual.getDuration("durations.plusLargeNanos"), Duration.ofNanos(4878955355435272204L));
        Assert.assertEquals(actual.getDuration("durations.minusLargeNanos"), Duration.ofNanos(-4878955355435272204L));
    }

    @Test
    public void testParseResourcesYamlSyntaxPeriods() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertEquals(actual.getPeriod("periods.day"), Period.ofDays(1));
        Assert.assertEquals(actual.getPeriod("periods.dayAsNumber"), Period.ofDays(2));
        Assert.assertEquals(actual.getPeriod("periods.week"), Period.ofWeeks(3));
        Assert.assertEquals(actual.getPeriod("periods.month"), Period.ofMonths(5));
        Assert.assertEquals(actual.getPeriod("periods.year"), Period.ofYears(8));
    }

    @Test
    public void testParseResourcesYamlSyntaxMemSizes() {
        Config actual = YamlParser.parseResourcesYamlSyntax("test.yaml", ConfigParseOptions.defaults())
            .toConfig()
            .resolve();

        Assert.assertEquals(actual.getMemorySize("memsizes.meg").toBytes(), 1L * 1024 * 1024);
        Assert.assertEquals(actual.getMemorySizeList("memsizes.megsList").stream()
            .map(ConfigMemorySize::toBytes).collect(Collectors.toList()), Arrays.asList(
            1L * 1024 * 1024, 1024L * 1024, 1048576L));
        Assert.assertEquals(actual.getMemorySize("memsizes.megAsNumber").toBytes(), 1048576L);
        Assert.assertEquals(actual.getMemorySize("memsizes.halfMeg").toBytes(), 512L * 1024);
        Assert.assertEquals(actual.getMemorySize("memsizes.yottabyte").toBytesBigInteger(),
            new BigInteger("1000000000000000000000000"));
        Assert.assertEquals(actual.getMemorySizeList("memsizes.yottabyteList").stream()
            .map(ConfigMemorySize::toBytesBigInteger).collect(Collectors.toList()), Arrays.asList(
            new BigInteger("1000000000000000000000000"), new BigInteger("500000000000000000000000")));
    }

    @DataProvider
    public Object[][] samples() {
        return new Object[][]{
            {"foo", string("foo")},
            {"${foo}", reference("foo")},
            {"$${foo}", string("${foo}")},
            {"${foo.bar}", reference("foo", "bar")},
            {"${foo}aaa", concatenation(reference("foo"), string("aaa"))},
            {"a${foo}aa${bar}aaa${baz}", concatenation(
                string("a"), reference("foo"), string("aa"),
                reference("bar"), string("aaa"), reference("baz"))},
            {"aaa${foo.bar}", concatenation(string("aaa"), reference("foo", "bar"))}
        };
    }

    @Test(dataProvider = "samples")
    public void testFromString(String str, AbstractConfigValue expected) {
        AbstractConfigValue actual = YamlParser.fromString(origin, str);

        Assert.assertEquals(actual, expected);
    }

    private static ConfigConcatenation concatenation(AbstractConfigValue... pieces) {
        return new ConfigConcatenation(origin, Arrays.asList(pieces));
    }

    private static ConfigReference reference(String... s) {
        return new ConfigReference(origin, new SubstitutionExpression(
            new Path(s), false));
    }

    private static ConfigString string(String s) {
        return new ConfigString.Quoted(origin, s);
    }

}
