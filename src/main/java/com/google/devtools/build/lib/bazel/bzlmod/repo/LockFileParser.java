package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Map.Entry;

public abstract class LockFileParser {
  private static Object convertToJavaObject(JsonElement element) {
    if (element.isJsonNull()) {
      return null;
    }
    if (element.isJsonPrimitive()) {
      JsonPrimitive value = element.getAsJsonPrimitive();
      if (value.isBoolean()) {
        return value.getAsBoolean();
      } else if (value.isString()) {
        return value.getAsString();
      } else if (value.isNumber()) {
        return value.getAsInt();
      }
    } else if (element.isJsonArray()) {
      ImmutableList.Builder<Object> builder = ImmutableList.builder();
      for (JsonElement e : element.getAsJsonArray()) {
        builder.add(convertToJavaObject(e));
      }
      return builder.build();
    } else if (element.isJsonObject()) {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      for (Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
        builder.put(entry.getKey(), convertToJavaObject(entry.getValue()));
      }
      return builder.build();
    }
    throw new IllegalArgumentException("Wrong element type");
  }

  static ImmutableMap<String, RepoSpec> loadRepoSpecs(String content) {
    JsonParser parser = new JsonParser();
    JsonElement json = parser.parse(content);

    ImmutableMap.Builder<String, RepoSpec> repositoryInfos = ImmutableMap.builder();

    for (JsonElement repoElement: json.getAsJsonObject().get("repositories").getAsJsonArray()) {
      JsonObject repo = repoElement.getAsJsonObject();

      ImmutableMap.Builder<String, Object> attributesBuilder = ImmutableMap.builder();
      if (repo.get("rule_class").getAsString().contains("%")) {
        attributesBuilder.put("visibility", ImmutableList.of());
      }
      for (Entry<String, JsonElement> entry : repo.get("attributes").getAsJsonObject().entrySet()) {
        attributesBuilder.put(entry.getKey(), convertToJavaObject(entry.getValue()));
      }

      String name = "@" + repo.get("name").getAsString();
      RepoSpec repoSpec = new RepoSpec(repo.get("rule_class").getAsString(), attributesBuilder.build());
      repositoryInfos.put(name, repoSpec);
    }

    return repositoryInfos.build();
  }
}
