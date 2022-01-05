package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigMemorySize;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class YamlParser {

    // default origin for values created with fromAnyRef and no origin specified
    final private static ConfigOrigin defaultValueOrigin = SimpleConfigOrigin
        .newSimple("hardcoded value");
    final private static ConfigBoolean defaultTrueValue = new ConfigBoolean(
        defaultValueOrigin, true);
    final private static ConfigBoolean defaultFalseValue = new ConfigBoolean(
        defaultValueOrigin, false);
    final private static ConfigNull defaultNullValue = new ConfigNull(
        defaultValueOrigin);
    final private static SimpleConfigList defaultEmptyList = new SimpleConfigList(
        defaultValueOrigin, Collections.emptyList());
    final private static SimpleConfigObject defaultEmptyObject = SimpleConfigObject
        .empty(defaultValueOrigin);

    private YamlParser() {
    }

    private static SimpleConfigList emptyList(ConfigOrigin origin) {
        if (origin == null || origin == defaultValueOrigin)
            return defaultEmptyList;
        else
            return new SimpleConfigList(origin,
                Collections.emptyList());
    }

    private static AbstractConfigObject emptyObject(ConfigOrigin origin) {
        // we want null origin to go to SimpleConfigObject.empty() to get the
        // origin "empty config" rather than "hardcoded value"
        if (origin == defaultValueOrigin)
            return defaultEmptyObject;
        else
            return SimpleConfigObject.empty(origin);
    }

    private static ConfigOrigin valueOrigin(String originDescription) {
        if (originDescription == null)
            return defaultValueOrigin;
        else
            return SimpleConfigOrigin.newSimple(originDescription);
    }

    public static ConfigValue fromAnyRef(Object object, String originDescription) {
        ConfigOrigin origin = valueOrigin(originDescription);
        return fromAnyRef(object, origin, FromMapMode.KEYS_ARE_KEYS);
    }

    public static ConfigObject parseResourcesYamlSyntax(String resourceBasename,
                                                        ConfigParseOptions baseOptions) {
        final Yaml yaml = new Yaml();
        final ClassLoader classLoader = baseOptions.getClassLoader();
        InputStream inputStream = Stream.of(resourceBasename, resourceBasename + ".yaml", resourceBasename + ".yml")
            .map(classLoader::getResourceAsStream)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new ConfigException.Missing(resourceBasename));

        return YamlParser.fromPathMap(yaml.load(inputStream), resourceBasename);
    }

    public static ConfigObject fromPathMap(
        Map<String, ? extends Object> pathMap, String originDescription) {
        ConfigOrigin origin = valueOrigin(originDescription);
        return (ConfigObject) fromAnyRef(pathMap, origin,
            FromMapMode.KEYS_ARE_PATHS);
    }

    static AbstractConfigValue fromAnyRef(Object object, ConfigOrigin origin,
                                          FromMapMode mapMode) {
        if (origin == null)
            throw new ConfigException.BugOrBroken(
                "origin not supposed to be null");

        if (object == null) {
            if (origin != defaultValueOrigin)
                return new ConfigNull(origin);
            else
                return defaultNullValue;
        } else if (object instanceof AbstractConfigValue) {
            return (AbstractConfigValue) object;
        } else if (object instanceof Boolean) {
            if (origin != defaultValueOrigin) {
                return new ConfigBoolean(origin, (Boolean) object);
            } else if ((Boolean) object) {
                return defaultTrueValue;
            } else {
                return defaultFalseValue;
            }
        } else if (object instanceof String) {
            return YamlParser.fromString(origin, (String) object);
        } else if (object instanceof Number) {
            // here we always keep the same type that was passed to us,
            // rather than figuring out if a Long would fit in an Int
            // or a Double has no fractional part. i.e. deliberately
            // not using ConfigNumber.newNumber() when we have a
            // Double, Integer, or Long.
            if (object instanceof Double) {
                return new ConfigDouble(origin, (Double) object, null);
            } else if (object instanceof Integer) {
                return new ConfigInt(origin, (Integer) object, null);
            } else if (object instanceof Long) {
                return new ConfigLong(origin, (Long) object, null);
            } else {
                return ConfigNumber.newNumber(origin,
                    ((Number) object).doubleValue(), null);
            }
        } else if (object instanceof Duration) {
            return new ConfigLong(origin, ((Duration) object).toMillis(), null);
        } else if (object instanceof Map) {
            if (((Map<?, ?>) object).isEmpty())
                return emptyObject(origin);

            if (mapMode == FromMapMode.KEYS_ARE_KEYS) {
                Map<String, AbstractConfigValue> values = new HashMap<String, AbstractConfigValue>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
                    Object key = entry.getKey();
                    if (!(key instanceof String))
                        throw new ConfigException.BugOrBroken(
                            "bug in method caller: not valid to create ConfigObject from map with non-String key: "
                                + key);
                    AbstractConfigValue value = fromAnyRef(entry.getValue(),
                        origin, mapMode);
                    values.put((String) key, value);
                }

                return new SimpleConfigObject(origin, values);
            } else {
                return YamlParser.fromPathMap(origin, (Map<?, ?>) object);
            }
        } else if (object instanceof Iterable) {
            Iterator<?> i = ((Iterable<?>) object).iterator();
            if (!i.hasNext())
                return emptyList(origin);

            List<AbstractConfigValue> values = new ArrayList<AbstractConfigValue>();
            while (i.hasNext()) {
                AbstractConfigValue v = fromAnyRef(i.next(), origin, mapMode);
                values.add(v);
            }

            return new SimpleConfigList(origin, values);
        } else if (object instanceof ConfigMemorySize) {
            return new ConfigLong(origin, ((ConfigMemorySize) object).toBytes(), null);
        } else {
            throw new ConfigException.BugOrBroken(
                "bug in method caller: not valid to create ConfigValue from: "
                    + object);
        }
    }

    static AbstractConfigObject fromPathMap(ConfigOrigin origin,
                                            Map<?, ?> pathExpressionMap) {
        Map<Path, Object> pathMap = new HashMap<Path, Object>();
        for (Map.Entry<?, ?> entry : pathExpressionMap.entrySet()) {
            Object keyObj = entry.getKey();
            if (!(keyObj instanceof String)) {
                throw new ConfigException.BugOrBroken(
                    "Map has a non-string as a key, expecting a path expression as a String");
            }
            Path path = Path.newPath((String) keyObj);
            pathMap.put(path, entry.getValue());
        }
        return fromPathMap(origin, pathMap, false /* from properties */);
    }

    private static AbstractConfigObject fromPathMap(ConfigOrigin origin,
                                                    Map<Path, Object> pathMap, boolean convertedFromProperties) {
        /*
         * First, build a list of paths that will have values, either string or
         * object values.
         */
        Set<Path> scopePaths = new HashSet<Path>();
        Set<Path> valuePaths = new HashSet<Path>();
        for (Path path : pathMap.keySet()) {
            // add value's path
            valuePaths.add(path);

            // all parent paths are objects
            Path next = path.parent();
            while (next != null) {
                scopePaths.add(next);
                next = next.parent();
            }
        }

        if (convertedFromProperties) {
            /*
             * If any string values are also objects containing other values,
             * drop those string values - objects "win".
             */
            valuePaths.removeAll(scopePaths);
        } else {
            /* If we didn't start out as properties, then this is an error. */
            for (Path path : valuePaths) {
                if (scopePaths.contains(path)) {
                    throw new ConfigException.BugOrBroken(
                        "In the map, path '"
                            + path.render()
                            + "' occurs as both the parent object of a value and as a value. "
                            + "Because Map has no defined ordering, this is a broken situation.");
                }
            }
        }

        /*
         * Create maps for the object-valued values.
         */
        Map<String, AbstractConfigValue> root = new HashMap<String, AbstractConfigValue>();
        Map<Path, Map<String, AbstractConfigValue>> scopes = new HashMap<Path, Map<String, AbstractConfigValue>>();

        for (Path path : scopePaths) {
            Map<String, AbstractConfigValue> scope = new HashMap<String, AbstractConfigValue>();
            scopes.put(path, scope);
        }

        /* Store string values in the associated scope maps */
        for (Path path : valuePaths) {
            Path parentPath = path.parent();
            Map<String, AbstractConfigValue> parent = parentPath != null ? scopes
                .get(parentPath) : root;

            String last = path.last();
            Object rawValue = pathMap.get(path);
            AbstractConfigValue value;
            if (convertedFromProperties) {
                if (rawValue instanceof String) {
                    value = new ConfigString.Quoted(origin, (String) rawValue);
                } else {
                    // silently ignore non-string values in Properties
                    value = null;
                }
            } else {
                value = YamlParser.fromAnyRef(pathMap.get(path), origin,
                    FromMapMode.KEYS_ARE_PATHS);
            }
            if (value != null)
                parent.put(last, value);
        }

        /*
         * Make a list of scope paths from longest to shortest, so children go
         * before parents.
         */
        List<Path> sortedScopePaths = new ArrayList<Path>();
        sortedScopePaths.addAll(scopePaths);
        // sort descending by length
        Collections.sort(sortedScopePaths, new Comparator<Path>() {
            @Override
            public int compare(Path a, Path b) {
                // Path.length() is O(n) so in theory this sucks
                // but in practice we can make Path precompute length
                // if it ever matters.
                return b.length() - a.length();
            }
        });

        /*
         * Create ConfigObject for each scope map, working from children to
         * parents to avoid modifying any already-created ConfigObject. This is
         * where we need the sorted list.
         */
        for (Path scopePath : sortedScopePaths) {
            Map<String, AbstractConfigValue> scope = scopes.get(scopePath);

            Path parentPath = scopePath.parent();
            Map<String, AbstractConfigValue> parent = parentPath != null ? scopes
                .get(parentPath) : root;

            AbstractConfigObject o = new SimpleConfigObject(origin, scope,
                ResolveStatus.RESOLVED, false /* ignoresFallbacks */);
            parent.put(scopePath.last(), o);
        }

        // return root config object
        return new SimpleConfigObject(origin, root, ResolveStatus.fromValues(root.values()),
            false /* ignoresFallbacks */);
    }

    static AbstractConfigValue fromString(ConfigOrigin origin, String string) {
        if (!string.contains("$")) {
            return new ConfigString.Quoted(origin, string);
        }

        final List<AbstractConfigValue> pieces = Arrays
            .stream(string.split("(?<!\\$)\\Q$\\E"))
            .flatMap(s -> fromStringPiece(origin, s))
            .collect(Collectors.toList());

        if (pieces.size() == 1) {
            return pieces.get(0);
        }

        return new ConfigConcatenation(origin, pieces);
    }

    private static Stream<AbstractConfigValue> fromStringPiece(ConfigOrigin origin, String string) {
        if (string.isEmpty()) {
            return Stream.empty();
        }

        if (string.startsWith("$") || !string.startsWith("{")) {
            return Stream.of(new ConfigString.Quoted(origin, string));
        }

        int index = string.indexOf("}");
        if (index == -1) {
            throw new ConfigException.Parse(
                origin, "String substitutions should ended with '}'.");
        }

        final ConfigReference reference = new ConfigReference(origin,
            new SubstitutionExpression(
                new Path(string.substring(1, index).split("\\.")),
                false));

        if (index == string.length() - 1) {
            return Stream.of(reference);
        }

        return Stream.of(reference, new ConfigString.Quoted(
            origin, string.substring(index + 1)));
    }

}
