package jetbrains.buildServer.aws.codebuild;

import com.amazonaws.services.codebuild.AWSCodeBuildClient;
import com.amazonaws.services.codebuild.model.*;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static jetbrains.buildServer.aws.codebuild.CodeBuildUtil.*;

/**
 * @author vbedrosova
 */
public class CodeBuildRunner extends AgentLifeCycleAdapter implements AgentBuildRunner {
  @NotNull
  private final List<CodeBuildBuildContext> myCodeBuildBuilds = new CopyOnWriteArrayList<>();

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
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final String projectName = getProjectName(runnerParameters);
        final String buildId = createClient(params).startBuild(
            new StartBuildRequest()
              .withProjectName(projectName)
              .withSourceVersion(getSourceVersion(runnerParameters))
              .withBuildspecOverride(getBuildSpec(runnerParameters))
              .withArtifactsOverride(getArtifacts())
              .withTimeoutInMinutesOverride(getTimeoutMinutesInt(runnerParameters))
              .withEnvironmentVariablesOverride(getEnvironmentVariables())).getBuild().getId();

        final String region = params.get(AWSCommonParams.REGION_NAME_PARAM);
        runningBuild.getBuildLogger().message("Build " + getBuildLink(buildId, region) + " started");
        runningBuild.getBuildLogger().message("View the entire log in the AWS CloudWatch console " + getBuildLogLink(buildId, projectName, region));

        if (isWaitStep(runnerParameters)) {
          final CodeBuildBuildContext c = new CodeBuildBuildContext(buildId, runnerParameters);
          while (!finished(c, runningBuild)) {
            try {
              Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
            } catch (InterruptedException e) {
              break;
            }
          }
        } else if (isWaitBuild(runnerParameters)) {
          myCodeBuildBuilds.add(new CodeBuildBuildContext(buildId, runnerParameters));
        }
      }

      @NotNull
      private Collection<EnvironmentVariable> getEnvironmentVariables() {
        return CollectionsUtil.convertCollection(context.getBuildParameters().getSystemProperties().entrySet(), new Converter<EnvironmentVariable, Map.Entry<String, String>>() {
          @Override
          public EnvironmentVariable createFrom(@NotNull Map.Entry<String, String> e) {
            return new EnvironmentVariable().withName(e.getKey()).withValue(e.getValue());
          }
        });
      }

      @Nullable
      private ProjectArtifacts getArtifacts() {
        final Map<String, String> params = context.getRunnerParameters();
        return isUploadS3Artifacts(params) ?
          new ProjectArtifacts()
            .withType(ArtifactsType.S3)
            .withPackaging(isZipS3Artifacts(params) ? ArtifactPackaging.ZIP : ArtifactPackaging.NONE)
            .withName(getArtifactS3Name(params))
            .withLocation(getArtifactS3Bucket(params))
          : null;
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
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    super.buildStarted(runningBuild);
    myCodeBuildBuilds.clear();
  }

  @Override
  public void beforeBuildFinish(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    super.beforeBuildFinish(build, buildStatus);

    while (!myCodeBuildBuilds.isEmpty() && build.getInterruptReason() == null) {
      for (CodeBuildBuildContext next : new ArrayList<>(myCodeBuildBuilds)) {
        if (finished(next, build)) myCodeBuildBuilds.remove(next);
      }
      try {
        Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
      } catch (InterruptedException e) {
        break;
      }
    }
    myCodeBuildBuilds.clear();
  }

  private boolean finished(@NotNull CodeBuildBuildContext context, @NotNull AgentRunningBuild build) {
    final List<Build> builds = createClient(context.params).batchGetBuilds(new BatchGetBuildsRequest().withIds(context.codeBuildBuildId)).getBuilds();

    if (builds == null || builds.isEmpty()) {
      build.getBuildLogger().warning("No AWS CodeBuild build with id=" + context.codeBuildBuildId + " found");
      return true;
    }

    if (builds.size() > 1) {
      build.getBuildLogger().warning("Found several AWS CodeBuild builds with id=" + context.codeBuildBuildId + ". Will process the first one.");
    }

    final Build codeBuildBuild = builds.iterator().next();

    reportPhases(codeBuildBuild, context, build);

    if (codeBuildBuild.getBuildComplete()) {
      // import logs?
      if (isSucceeded(codeBuildBuild.getBuildStatus())) {
        build.getBuildLogger().message(getBuildString(context) + " succeeded");
      }
      return true;
    }
    return false;
  }

  @NotNull
  private static String getBuildString(@NotNull CodeBuildBuildContext context) {
    return "Build " + context.codeBuildBuildId;
  }

  private void reportPhases(@NotNull Build codeBuildBuild, @NotNull CodeBuildBuildContext context, @NotNull AgentRunningBuild build) {
    if (codeBuildBuild.getPhases() == null || codeBuildBuild.getPhases().size() <= context.prevPhases.size()) return;

    final String buildString = getBuildString(context);
    build.getBuildLogger().targetStarted(buildString);
    try {
      for (BuildPhase phase : codeBuildBuild.getPhases()) {
        final String phaseName = phase.getPhaseType();
        if (context.prevPhases.contains(phaseName)) continue;

        final String format = getFormat(phase, phaseName);
        final String status = phase.getPhaseStatus();
        if (status == null || isInProgress(status)) {
          build.getBuildLogger().message(String.format(format, "is in progress"));
        } else {
          context.prevPhases.add(phaseName);

          if (isSucceeded(status)) {
            build.getBuildLogger().message(String.format(format, "succeeded"));
          } else {
            if (isFailed(status)) {
              build.getBuildLogger().error(String.format(format, "failed"));
            } else {
              build.getBuildLogger().error(String.format(format, "finished with status " + status));
            }
            build.getBuildLogger().logBuildProblem(createBuildProblem(phase, context.params, build.getCheckoutDirectory().getAbsolutePath()));
          }
        }
      }
    } finally {
      build.getBuildLogger().targetFinished(buildString);
    }
  }

  @NotNull
  private String getFormat(BuildPhase phase, String phaseName) {
    final Long phaseDuration = phase.getDurationInSeconds();
    return phaseDuration == null ?
      phaseName + " %s" :
      phaseName + " %s in " + phaseDuration + StringUtil.pluralize(" seconds", phaseDuration.intValue());
  }

  @NotNull
  private BuildProblemData createBuildProblem(@NotNull BuildPhase failedPhase, @NotNull Map<String, String> runnerParams, @NotNull String checkoutDir) {
    return BuildProblemData.createBuildProblem(
      getProblemIdentity(checkoutDir, failedPhase, runnerParams),
      CodeBuildConstants.BUILD_PROBLEM_TYPE,
      getProblemDescription(failedPhase, runnerParams));
  }

  @NotNull
  private String getProblemDescription(@NotNull BuildPhase failedPhase, @NotNull Map<String, String> runnerParams) {
    final StringBuilder res = new StringBuilder(getProjectName(runnerParams));
    res.append(" ").append(failedPhase.getPhaseType()).append(" phase ");
    if (failedPhase.getContexts().isEmpty()) {
      if (isFailed(failedPhase.getPhaseStatus())) {
        res.append("failed");
      } else {
        res.append("finished with status: ").append(failedPhase.getPhaseStatus());
      }
    } else {
      res.append(": ");
      for (int i = 0; i < failedPhase.getContexts().size(); ++i) {
        if (i > 0) {
          res.append("; ");
        }
        res.append(failedPhase.getContexts().get(i).getMessage());
      }
    }
    return res.toString();
  }


  @NotNull
  private String getProblemIdentity(@NotNull String checkoutDir, @NotNull BuildPhase failedPhase, @NotNull Map<String, String> runnerParams) {
    final ArrayList<String> otherParts = new ArrayList<>();
    otherParts.add(getProjectName(runnerParams));
    otherParts.add(failedPhase.getPhaseType());
    otherParts.add(failedPhase.getPhaseStatus());
    for (PhaseContext phaseContext : failedPhase.getContexts()) {
      if (StringUtil.isNotEmpty(phaseContext.getStatusCode())) otherParts.add(phaseContext.getStatusCode());
      if (StringUtil.isNotEmpty(phaseContext.getMessage())) otherParts.add(phaseContext.getMessage());
    }
    return String.valueOf(AWSCommonParams.calculateIdentity(checkoutDir, runnerParams, otherParts));
  }

  private static final class CodeBuildBuildContext {
    @NotNull private final String codeBuildBuildId;
    @NotNull private final Map<String, String> params;
    @NotNull private Set<String> prevPhases = new HashSet<>();

    private CodeBuildBuildContext(@NotNull String codeBuildBuildId, @NotNull Map<String, String> params) {
      this.codeBuildBuildId = codeBuildBuildId;
      this.params = params;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CodeBuildBuildContext that = (CodeBuildBuildContext) o;

      return codeBuildBuildId.equals(that.codeBuildBuildId);
    }

    @Override
    public int hashCode() {
      return codeBuildBuildId.hashCode();
    }
  }
}
