package com.google.devtools.build.lib.rules.repository;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.Type;
import com.google.devtools.build.lib.rules.repository.RepositoryDirectoryValue.Builder;
import com.google.devtools.build.lib.shell.Command;
import com.google.devtools.build.lib.shell.CommandException;
import com.google.devtools.build.lib.shell.CommandResult;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.skyframe.SkyFunction.Environment;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;

import net.starlark.java.eval.EvalException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Fetch a repository by executing given command
 */
public class RepoFunction extends RepositoryFunction {

  @Nullable
  @Override
  public Builder fetch(Rule rule, Path outputDirectory, BlazeDirectories directories,
      Environment env, Map<String, String> markerData, SkyKey key)
      throws InterruptedException, RepositoryFunctionException {
    WorkspaceAttributeMapper mapper = WorkspaceAttributeMapper.of(rule);
    List<String> args = RepoFunction.getAttribute(mapper, RepoRule.FETCH_COMMAND, Type.STRING_LIST);
    String[] argsArray = new String[args.size()];
    args.toArray(argsArray);
    Command command = new Command(
        argsArray, this.clientEnvironment, directories.getWorkspace().getPathFile());
    try {
      CommandResult result = command.execute();
      if (!result.getTerminationStatus().success()) {
        EvalException e =
            new EvalException(String.format(
                "Failed to run the fetch command '%s' (exit %d).\nStdout:\n%sStderr:\n%s",
                Joiner.on(" ").join(args),
                result.getTerminationStatus().getExitCode(),
                toString(result.getStdoutStream()),
                toString(result.getStderrStream())));
        throw new RepositoryFunctionException(e, Transience.TRANSIENT);
      }
      PathFragment repoPath = PathFragment.create(toString(result.getStdoutStream()).trim());
      if (!repoPath.isAbsolute()) {
        EvalException e =
            new EvalException(String.format(
                "Expect an absolute path printed by the fetch command '%s', but got: %s",
                Joiner.on(" ").join(args),
                repoPath));
        throw new RepositoryFunctionException(e, Transience.TRANSIENT);
      }
      return RepositoryDelegatorFunction.symlink(outputDirectory, repoPath, rule.getName(), env);
    } catch (CommandException e) {
      throw new RepositoryFunctionException(new EvalException(e.getMessage()), Transience.TRANSIENT);
    }
  }

  private static String toString(ByteArrayOutputStream stream) {
    try {
      return new String(stream.toByteArray(), UTF_8);
    } catch (IllegalStateException e) {
      return "";
    }
  }

  @Nullable
  private static <T> T getAttribute(
      WorkspaceAttributeMapper mapper, String attributeName, Type<T> type)
      throws RepositoryFunctionException {
    try {
      return mapper.get(attributeName, type);
    } catch (EvalException e) {
      throw new RepositoryFunctionException(e, Transience.PERSISTENT);
    }
  }

  @Override
  protected boolean isLocal(Rule rule) {
    return false;
  }

  @Override
  public Class<? extends RuleDefinition> getRuleDefinition() {
    return RepoRule.class;
  }
}
