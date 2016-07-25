// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#include "src/main/cpp/blaze_startup_options.h"

#include <assert.h>
#include <errno.h>  // errno, ENOENT
#include <stdlib.h>  // getenv, exit
#include <string.h>  // strerror
#include <unistd.h>  // access

#include <cstdio>

#include "src/main/cpp/blaze_util_platform.h"
#include "src/main/cpp/blaze_util.h"
#include "src/main/cpp/util/exit_code.h"
#include "src/main/cpp/util/file.h"
#include "src/main/cpp/util/strings.h"

namespace blaze {

using std::vector;

struct StartupOptions {};

BlazeStartupOptions::BlazeStartupOptions() {
  Init();
}

BlazeStartupOptions::BlazeStartupOptions(const BlazeStartupOptions &rhs)
    : product_name(rhs.product_name),
      output_base(rhs.output_base),
      install_base(rhs.install_base),
      output_root(rhs.output_root),
      output_user_root(rhs.output_user_root),
      deep_execroot(rhs.deep_execroot),
      block_for_lock(rhs.block_for_lock),
      host_jvm_debug(rhs.host_jvm_debug),
      host_jvm_profile(rhs.host_jvm_profile),
      host_jvm_args(rhs.host_jvm_args),
      batch(rhs.batch),
      batch_cpu_scheduling(rhs.batch_cpu_scheduling),
      io_nice_level(rhs.io_nice_level),
      max_idle_secs(rhs.max_idle_secs),
      oom_more_eagerly(rhs.oom_more_eagerly),
      oom_more_eagerly_threshold(rhs.oom_more_eagerly_threshold),
      watchfs(rhs.watchfs),
      allow_configurable_attributes(rhs.allow_configurable_attributes),
      option_sources(rhs.option_sources),
      command_port(rhs.command_port),
      invocation_policy(rhs.invocation_policy),
      host_javabase(rhs.host_javabase) {}

BlazeStartupOptions::~BlazeStartupOptions() {
}

BlazeStartupOptions& BlazeStartupOptions::operator=(
    const BlazeStartupOptions &rhs) {
  Copy(rhs, this);
  return *this;
}

string BlazeStartupOptions::GetOutputRoot() {
  return blaze::GetOutputRoot();
}

void BlazeStartupOptions::AddExtraOptions(vector<string> *result) const {}

static const char kWorkspaceMarker[] = "WORKSPACE";

// static
bool BlazeStartupOptions::InWorkspace(const string &workspace) {
  return access(
      blaze_util::JoinPath(workspace, kWorkspaceMarker).c_str(), F_OK) == 0;
}

// static
string BlazeStartupOptions::GetWorkspace(const string &cwd) {
  assert(!cwd.empty());
  string workspace = cwd;

  do {
    if (access(blaze_util::JoinPath(
            workspace, kWorkspaceMarker).c_str(), F_OK) != -1) {
      return workspace;
    }
    workspace = blaze_util::Dirname(workspace);
  } while (!workspace.empty() && workspace != "/");
  return "";
}

blaze_exit_code::ExitCode BlazeStartupOptions::ProcessArgExtra(
    const char *arg, const char *next_arg, const string &rcfile,
    const char **value, bool *is_processed, string *error) {
  *is_processed = false;
  return blaze_exit_code::SUCCESS;
}

blaze_exit_code::ExitCode BlazeStartupOptions::CheckForReExecuteOptions(
      int argc, const char *argv[], string *error) {
  return blaze_exit_code::SUCCESS;
}

string BlazeStartupOptions::GetDefaultHostJavabase() const {
  return blaze::GetDefaultHostJavabase();
}

string BlazeStartupOptions::GetJvm() {
  string java_program = GetHostJavabase() + "/bin/java";
  if (access(java_program.c_str(), X_OK) == -1) {
    if (errno == ENOENT) {
      fprintf(stderr, "Couldn't find java at '%s'.\n", java_program.c_str());
    } else {
      fprintf(stderr, "Couldn't access %s: %s\n", java_program.c_str(),
          strerror(errno));
    }
    exit(1);
  }
  // If the full JDK is installed
  string jdk_rt_jar = GetHostJavabase() + "/jre/lib/rt.jar";
  // If just the JRE is installed
  string jre_rt_jar = GetHostJavabase() + "/lib/rt.jar";
  if ((access(jdk_rt_jar.c_str(), R_OK) == 0)
      || (access(jre_rt_jar.c_str(), R_OK) == 0)) {
    return java_program;
  }
  fprintf(stderr, "Problem with java installation: "
      "couldn't find/access rt.jar in %s\n", GetHostJavabase().c_str());
  exit(1);
}

string BlazeStartupOptions::GetExe(const string &jvm, const string &jar_path) {
  return jvm;
}

void BlazeStartupOptions::AddJVMArgumentPrefix(const string &javabase,
    std::vector<string> *result) const {
}

void BlazeStartupOptions::AddJVMArgumentSuffix(const string &real_install_dir,
                                               const string &jar_path,
    std::vector<string> *result) const {
  result->push_back("-jar");
  result->push_back(blaze::ConvertPath(
      blaze_util::JoinPath(real_install_dir, jar_path)));
}

blaze_exit_code::ExitCode BlazeStartupOptions::AddJVMArguments(
    const string &host_javabase, vector<string> *result,
    const vector<string> &user_options, string *error) const {
  // Configure logging
  const string propFile = output_base + "/javalog.properties";
  if (!WriteFile(
      "handlers=java.util.logging.FileHandler\n"
      ".level=INFO\n"
      "java.util.logging.FileHandler.level=INFO\n"
      "java.util.logging.FileHandler.pattern="
      + output_base + "/java.log\n"
      "java.util.logging.FileHandler.limit=50000\n"
      "java.util.logging.FileHandler.count=1\n"
      "java.util.logging.FileHandler.formatter="
      "java.util.logging.SimpleFormatter\n",
      propFile)) {
    perror(("Couldn't write logging file " + propFile).c_str());
  } else {
    result->push_back("-Djava.util.logging.config.file=" + propFile);
  }
  return blaze_exit_code::SUCCESS;
}

string BlazeStartupOptions::RcBasename() {
  return ".bazelrc";
}

static string FindDepotBlazerc(const string& workspace) {
  // Package semantics are ignored here, but that's acceptable because
  // blaze.blazerc is a configuration file.
  vector<string> candidates;
  BlazeStartupOptions::WorkspaceRcFileSearchPath(&candidates);
  for (const auto& candidate : candidates) {
    string blazerc = blaze_util::JoinPath(workspace, candidate);
    if (!access(blazerc.c_str(), R_OK)) {
      return blazerc;
    }
  }
  return "";
}

static string FindAlongsideBinaryBlazerc(const string& cwd,
                                                   const string& arg0) {
  string path = arg0[0] == '/' ? arg0 : blaze_util::JoinPath(cwd, arg0);
  string base = blaze_util::Basename(arg0);
  string binary_blazerc_path = path + "." + base + "rc";
  if (!access(binary_blazerc_path.c_str(), R_OK)) {
    return binary_blazerc_path;
  }
  return "";
}

static string FindSystemWideBlazerc() {
  string path = "/etc/bazel.bazelrc";
  if (!access(path.c_str(), R_OK)) {
    return path;
  }
  return "";
}

void BlazeStartupOptions::FindCandidateBlazercPaths(
    const string& workspace, const string& cwd, const string& arg0,
    std::vector<string>* result) {
  result->push_back(FindDepotBlazerc(workspace));
  result->push_back(FindAlongsideBinaryBlazerc(cwd, arg0));
  result->push_back(FindSystemWideBlazerc());
}

void BlazeStartupOptions::WorkspaceRcFileSearchPath(
    vector<string>* candidates) {
  candidates->push_back("tools/bazel.rc");
}

bool BlazeStartupOptions::WorkspaceRelativizeRcFilePath(const string &workspace,
                                                        string *path_fragment) {
  // Strip off the "%workspace%/" prefix and prepend the true workspace path.
  // In theory this could use alternate search paths for blazerc files.
  path_fragment->assign(
      blaze_util::JoinPath(workspace,
                           path_fragment->substr(WorkspacePrefixLength)));
  return true;
}

}  // namespace blaze
