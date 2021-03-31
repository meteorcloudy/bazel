package com.google.devtools.build.lib.bazel.bzlmod;

import static com.google.common.collect.Comparators.lexicographical;
import static java.util.Comparator.comparing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@AutoValue
abstract class ParsedVersion implements Comparable<ParsedVersion> {

  // This is intentionally looser than the semver spec. In particular, the "release" part isn't
  // limited to exactly 3 numbers (major, minor, patch), but can be fewer or more. Underscore is
  // allowed in prerelease and build metadata for regex brevity.
  private static final Pattern PATTERN =
      Pattern.compile("([\\d.]*)(?:-([\\w.-]*))?(?:\\+[\\w.-]*)?");

  /**
   * Represents a segment in the prerelease part of the version string. This is separated from other
   * "Identifier"s by a dot. An identifier is compared differently based on whether it's digits-only
   * or not.
   */
  @AutoValue
  static abstract class Identifier {

    abstract boolean isDigitsOnly();

    abstract int asNumber();

    abstract String asString();

    static Identifier from(String string) {
      // TODO: throw when string is empty
      if (string.chars().allMatch(Character::isDigit)) {
        return new AutoValue_ParsedVersion_Identifier(true, Integer.parseInt(string), string);
      } else {
        return new AutoValue_ParsedVersion_Identifier(false, 0, string);
      }
    }
  }

  abstract ImmutableList<Integer> getRelease();

  abstract ImmutableList<Identifier> getPrerelease();

  public abstract String getOriginal();

  boolean isOverride() {
    return getRelease().isEmpty();
  }

  boolean isPrereleaseEmpty() {
    return getPrerelease().isEmpty();
  }

  public static ParsedVersion parse(String version) {
    Matcher matcher = PATTERN.matcher(version);
    if (!matcher.matches()) {
      // TODO: wrap exception
      throw new IllegalArgumentException("bad version: " + version);
    }
    String release = matcher.group(1);
    @Nullable String prerelease = matcher.group(2);
    ImmutableList<Integer> releaseSplit;
    if (release.isEmpty()) {
      releaseSplit = ImmutableList.of();
    } else {
      try {
        releaseSplit = Arrays.stream(release.split("\\."))
            .map(Integer::valueOf)
            .collect(ImmutableList.toImmutableList());
      } catch (NumberFormatException e) {
        // TODO wrap exception
        e.getCause();
        throw e;
      }
    }
    ImmutableList<Identifier> prereleaseSplit =
        Strings.isNullOrEmpty(prerelease) ? ImmutableList.of() :
            Arrays.stream(prerelease.split("\\."))
                .map(Identifier::from)
                .collect(ImmutableList.toImmutableList());
    return new AutoValue_ParsedVersion(releaseSplit, prereleaseSplit, version);
  }

  private static final Comparator<ParsedVersion> COMPARATOR = Comparator.nullsFirst(
      comparing(ParsedVersion::isOverride)
          .thenComparing(ParsedVersion::getRelease,
              lexicographical(Comparator.<Integer>naturalOrder()))
          .thenComparing(ParsedVersion::isPrereleaseEmpty)
          .thenComparing(ParsedVersion::getPrerelease,
              lexicographical(
                  comparing(Identifier::isDigitsOnly, Comparator.reverseOrder())
                      .thenComparingInt(Identifier::asNumber)
                      .thenComparing(Identifier::asString))));

  @Override
  public int compareTo(ParsedVersion o) {
    return Objects.compare(this, o, COMPARATOR);
  }

  public static ParsedVersion max(@Nullable ParsedVersion a, @Nullable ParsedVersion b) {
    return Objects.compare(a, b, COMPARATOR) >= 0 ? a : b;
  }
}
