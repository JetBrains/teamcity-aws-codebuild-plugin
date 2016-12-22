package jetbrains.buildServer.aws.codebuild;

import com.amazonaws.services.codebuild.AWSCodeBuildClient;
import com.amazonaws.services.codebuild.model.BatchGetBuildsRequest;
import com.amazonaws.services.codebuild.model.Build;
import com.amazonaws.services.codebuild.model.StartBuildRequest;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        // version
        // timeout
        // artifacts
        // variables
        // wait for build finish - in step, in build?
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final String buildId = createClient(params).startBuild(
            new StartBuildRequest()
              .withProjectName(getProjectName(runnerParameters))
              .withSourceVersion(getSourceVersion(runnerParameters))
              .withTimeoutInMinutesOverride(getTimeoutMinutesInt(runnerParameters))).getBuild().getId();

        myCodeBuildBuilds.add(new CodeBuildBuildContext(buildId, runnerParameters));
        runningBuild.getBuildLogger().message("AWS CodeBuild build with id=" + buildId + " started");
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

        final CodeBuildBuildContext context = it.next();

        final List<Build> builds = createClient(context.params).batchGetBuilds(new BatchGetBuildsRequest().withIds(context.codeBuildBuildId)).getBuilds();

        if (builds.isEmpty()) {
          build.getBuildLogger().warning("No AWS CodeBuild build with id=" + context.codeBuildBuildId + " found");
          it.remove();
          continue;
        }

        if (builds.size() > 1) {
          build.getBuildLogger().warning("Found several AWS CodeBuild builds with id=" + context.codeBuildBuildId + ". Will process the first one.");
        }

        final Build codeBuildBuild = builds.iterator().next();
        if (codeBuildBuild.getBuildComplete()) {
          // import logs?
          if (CodeBuildConstants.SUCCEEDED.equals(codeBuildBuild.getBuildStatus())) {
            build.getBuildLogger().message("AWS CodeBuild build with id=" + context.codeBuildBuildId + " succeeded");
          } else {
            build.getBuildLogger().logBuildProblem(createBuildProblem(codeBuildBuild));
          }
          it.remove();
        }
      }
      try {
        Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
      } catch (InterruptedException e) { /* do nothing */}
    }
  }

  @NotNull
  private BuildProblemData createBuildProblem(@NotNull Build codeBuildBuild) {
    return BuildProblemData.createBuildProblem(
      codeBuildBuild.getProjectName().hashCode() + codeBuildBuild.getBuildStatus(), // add settings hash
      CodeBuildConstants.BUILD_PROBLEM_TYPE,
      "AWS CodeBuild build finished with status: " + codeBuildBuild.getBuildStatus());
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
