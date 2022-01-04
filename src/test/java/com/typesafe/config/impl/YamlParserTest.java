package com.typesafe.config.impl;

import com.typesafe.config.ConfigOrigin;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;

public class YamlParserTest {

    private static final ConfigOrigin origin = SimpleConfigOrigin.newSimple("default");

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
