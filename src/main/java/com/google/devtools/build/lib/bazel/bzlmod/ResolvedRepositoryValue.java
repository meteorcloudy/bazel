package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.RepositoryInfo.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map.Entry;

public abstract class ResolvedRepositoryValue {
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

  static ImmutableList<RepositoryInfo> loadRepositoryFromFile(String file)
      throws FileNotFoundException {
    JsonParser parser = new JsonParser();
    JsonElement json = parser.parse(new FileReader(file));

    ImmutableList.Builder<RepositoryInfo> repositoryInfoList = ImmutableList.builder();

    for (JsonElement repoElement: json.getAsJsonObject().get("repositories").getAsJsonArray()) {
      JsonObject repo = repoElement.getAsJsonObject();

      ImmutableMap.Builder<String, Object> attributesBuilder = ImmutableMap.builder();
      if (repo.get("rule_class").getAsString().contains("%")) {
        attributesBuilder.put("visibility", ImmutableList.of());
      }
      for (Entry<String, JsonElement> entry : repo.get("attributes").getAsJsonObject().entrySet()) {
        attributesBuilder.put(entry.getKey(), convertToJavaObject(entry.getValue()));
      }

      RepositoryInfo.Builder builder = new Builder();
      RepositoryInfo repositoryInfo = builder.setName("@" + repo.get("name").getAsString())
          .setRuleClass(repo.get("rule_class").getAsString())
          .setAttributes(attributesBuilder.build())
          .setRepoDeps((ImmutableList) convertToJavaObject(repo.get("repo_deps")))
          .setRepoMappings((ImmutableMap<String, String>) convertToJavaObject(repo.get("repo_mappings")))
          .setVendorDir(repo.get("vendor_dir").getAsString())
          .build();

      repositoryInfoList.add(repositoryInfo);
    }

    return repositoryInfoList.build();
  }
}
