package jetbrains.buildServer.aws.codebuild;

import com.amazonaws.services.codebuild.AWSCodeBuildClient;
import com.amazonaws.services.codebuild.model.BatchGetBuildsRequest;
import com.amazonaws.services.codebuild.model.Build;
import com.amazonaws.services.codebuild.model.EnvironmentVariable;
import com.amazonaws.services.codebuild.model.StartBuildRequest;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jetbrains.buildServer.aws.codebuild.CodeBuildConstants.ENV_VAR_PREFIX;
import static jetbrains.buildServer.aws.codebuild.CodeBuildUtil.*;

/**
 * @author vbedrosova
 */
public class CodeBuildRunner extends AgentLifeCycleAdapter implements AgentBuildRunner {
  @NotNull
  private final ArrayList<CodeBuildBuildContext> myCodeBuildBuilds = new ArrayList<>();

  public CodeBuildRunner(@NotNull EventDispatcher<AgentLifeCycleListener> eventDispatcher) {
    eventDispatcher.addListener(this);
  }

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {
    return new BuildProcessAdapter() {
      @Override
      public void start() throws RunBuildException {
        final Map<String, String> params = validateParams();
        // artifacts
        // variables
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final String buildId = createClient(params).startBuild(
            new StartBuildRequest()
              .withProjectName(getProjectName(runnerParameters))
              .withSourceVersion(getSourceVersion(runnerParameters))
              .withTimeoutInMinutesOverride(getTimeoutMinutesInt(runnerParameters))
              .withEnvironmentVariablesOverride(getEnvironmentVariables())).getBuild().getId();

        runningBuild.getBuildLogger().message(getProjectName(params) + " build with id=" + buildId + " started");

        if (isWaitStep(runnerParameters)) {
          final CodeBuildBuildContext c = new CodeBuildBuildContext(buildId, runnerParameters);
          while (!finished(c, runningBuild)) {
            try {
              Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
            } catch (InterruptedException e) { /* do nothing */ }
          }
        } else if (isWaitBuild(runnerParameters)) {
          myCodeBuildBuilds.add(new CodeBuildBuildContext(buildId, runnerParameters));
        }
      }

      @NotNull
      private Collection<EnvironmentVariable> getEnvironmentVariables() {
        return CollectionsUtil.convertAndFilterNulls(context.getBuildParameters().getEnvironmentVariables().entrySet(), new Converter<EnvironmentVariable, Map.Entry<String, String>>() {
          @Override
          public EnvironmentVariable createFrom(@NotNull Map.Entry<String, String> e) {
            if (e.getKey().startsWith(ENV_VAR_PREFIX)) {
              return new EnvironmentVariable().withName(e.getKey().substring(ENV_VAR_PREFIX.length())).withValue(e.getValue());
            }
            return null;
          }
        });
      }

      @NotNull
      @Override
      public BuildFinishedStatus waitFor() throws RunBuildException {
        return BuildFinishedStatus.FINISHED_SUCCESS;
      }

      @NotNull
      private Map<String, String> validateParams() throws RunBuildException {
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final Map<String, String> invalids = ParametersValidator.validateSettings(runnerParameters, false);
        if (invalids.isEmpty()) return runnerParameters;
        throw new RunBuildException(StringUtil.join(invalids.values(), "\n"), null, ErrorData.BUILD_RUNNER_ERROR_TYPE);
      }
    };
  }

  @NotNull
  private AWSCodeBuildClient createClient(Map<String, String> params) {
    return AWSCommonParams.createAWSClients(params).createCodeBuildClient();
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new AgentBuildRunnerInfo() {
      @NotNull
      @Override
      public String getType() {
        return CodeBuildConstants.RUNNER_TYPE;
      }

      @Override
      public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
        return true;
      }
    };
  }

  @Override
  public void beforeBuildFinish(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    super.beforeBuildFinish(build, buildStatus);

    while (!myCodeBuildBuilds.isEmpty()) { // timeout?
      final Iterator<CodeBuildBuildContext> it = myCodeBuildBuilds.iterator();
      while (it.hasNext()) {
        if (finished(it.next(), build)) it.remove();
      }
      try {
        Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
      } catch (InterruptedException e) { /* do nothing */}
    }
  }

  private boolean finished(@NotNull CodeBuildBuildContext context, @NotNull AgentRunningBuild build) {
    final List<Build> builds = createClient(context.params).batchGetBuilds(new BatchGetBuildsRequest().withIds(context.codeBuildBuildId)).getBuilds();

    if (builds.isEmpty()) {
      build.getBuildLogger().warning("No AWS CodeBuild build with id=" + context.codeBuildBuildId + " found");
      return true;
    }

    if (builds.size() > 1) {
      build.getBuildLogger().warning("Found several AWS CodeBuild builds with id=" + context.codeBuildBuildId + ". Will process the first one.");
    }

    final Build codeBuildBuild = builds.iterator().next();
    if (codeBuildBuild.getBuildComplete()) {
      // import logs?
      if (CodeBuildConstants.SUCCEEDED.equals(codeBuildBuild.getBuildStatus())) {
        build.getBuildLogger().message(getProjectName(context.params) + " build with id=" + context.codeBuildBuildId + " succeeded");
      } else {
        build.getBuildLogger().logBuildProblem(createBuildProblem(build, codeBuildBuild, context.params));
      }
      return true;
    }
    return false;
  }

  @NotNull
  private BuildProblemData createBuildProblem(@NotNull AgentRunningBuild build,@NotNull Build codeBuildBuild, @NotNull Map<String, String> runnerParams) {
    return BuildProblemData.createBuildProblem(
      String.valueOf(getProblemIdentity(build, codeBuildBuild, runnerParams)),
      CodeBuildConstants.BUILD_PROBLEM_TYPE,
      getProblemDescription(codeBuildBuild, runnerParams));
  }

  @NotNull
  private String getProblemDescription(@NotNull Build codeBuildBuild, @NotNull Map<String, String> runnerParams) {
    final String status = codeBuildBuild.getBuildStatus();
    final StringBuilder res = new StringBuilder(getProjectName(runnerParams));
    res.append(" build ");
    if ("FAILED".equals(status)) {
      res.append("failed");
    } else {
      res.append("finished with status: ").append(status);
    }
    return res.toString();
  }

  private int getProblemIdentity(@NotNull AgentRunningBuild build, @NotNull Build codeBuildBuild, @NotNull Map<String, String> runnerParams) {
    return AWSCommonParams.calculateIdentity(build.getCheckoutDirectory().getAbsolutePath(), runnerParams, getProjectName(runnerParams), codeBuildBuild.getBuildStatus());
  }

  private static final class CodeBuildBuildContext {
    @NotNull private final String codeBuildBuildId;
    @NotNull private final Map<String, String> params;

    private CodeBuildBuildContext(@NotNull String codeBuildBuildId, @NotNull Map<String, String> params) {
      this.codeBuildBuildId = codeBuildBuildId;
      this.params = params;
    }
  }
}
