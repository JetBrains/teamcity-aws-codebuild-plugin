/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.aws.codebuild;

import com.amazonaws.services.codebuild.model.*;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.util.*;
import jetbrains.buildServer.util.amazon.AWSClients;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipOutputStream;

import static jetbrains.buildServer.aws.codebuild.CodeBuildUtil.*;
import static jetbrains.buildServer.messages.DefaultMessagesInfo.*;

/**
 * @author vbedrosova
 */
public class CodeBuildRunner extends AgentLifeCycleAdapter implements AgentBuildRunner {
  @NotNull
  private final List<CodeBuildBuildContext> myCodeBuildBuilds = new CopyOnWriteArrayList<CodeBuildBuildContext>();

  public CodeBuildRunner(@NotNull EventDispatcher<AgentLifeCycleListener> eventDispatcher) {
    eventDispatcher.addListener(this);
  }

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException {
    return new SyncBuildProcessAdapter() {
      @NotNull
      @Override
      protected BuildFinishedStatus runImpl() throws RunBuildException {
        final Map<String, String> runnerParameters = validateParams();
        final String projectName = getProjectName(runnerParameters);
        final String buildId = AWSCommonParams.withAWSClients(runnerParameters, new AWSCommonParams.WithAWSClients<String, RunBuildException>() {
          @Nullable
          @Override
          public String run(@NotNull AWSClients clients) throws RunBuildException {
            return clients.createCodeBuildClient().startBuild(
              new StartBuildRequest()
                .withProjectName(projectName)
                .withSourceVersion(getSourceVersion(projectName))
                .withBuildspecOverride(getBuildSpec(runnerParameters))
                .withArtifactsOverride(getArtifacts())
                .withTimeoutInMinutesOverride(getTimeoutMinutesInt(runnerParameters))
                .withEnvironmentVariablesOverride(getEnvironmentVariables())).getBuild().getId();
          }
        });

        runningBuild.addSharedSystemProperty(String.format(CodeBuildConstants.BUILD_ID_SYSTEM_PROPERTY_FORMAT, context.getId()), buildId);

        final String region = runnerParameters.get(AWSCommonParams.REGION_NAME_PARAM);
        runningBuild.getBuildLogger().message(projectName + " build " + getBuildLink(buildId, region) + " started");
        runningBuild.getBuildLogger().message("View the entire log in the AWS CloudWatch console " + getBuildLogLink(buildId, projectName, region));

        final CodeBuildBuildContext c = new CodeBuildBuildContext(buildId, projectName, runnerParameters);
        if (isWaitStep(runnerParameters)) {
          startContext(c, runningBuild);
          try {
            while (!finished(c, runningBuild)) {
              if (isInterrupted()) {
                CodeBuildRunner.this.interrupt(c, runningBuild);
                break;
              }
              try {
                Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
              } catch (InterruptedException e) {
                break;
              }
            }
          } finally {
            log(runningBuild, getBlockEnd(c));
          }
        } else if (isWaitBuild(runnerParameters)) {
          myCodeBuildBuilds.add(c);
        }
        return isInterrupted() ? BuildFinishedStatus.INTERRUPTED : BuildFinishedStatus.FINISHED_SUCCESS;
      }

      @Nullable
      private String getSourceVersion(@NotNull String projectName) throws RunBuildException {
        final Map<String, String> params = context.getRunnerParameters();
        if (isUseBuildRevision(params)) {
          final ProjectInfo project = getProject(params, projectName);
          if (project == null) {
            throw new RunBuildException("No AWS CodeBuild project " + projectName + " found. Please check the settings.");
          }
          if (SourceType.GITHUB.toString().equals(project.getSourceType())) {
            final String vcsRootId = runningBuild.getSharedConfigParameters().get(CodeBuildConstants.GIT_HUB_VCS_ROOT_ID_CONFIG_PARAM);

            if (StringUtil.isEmptyOrSpaces(vcsRootId) || CodeBuildConstants.UNKNOWN_GIT_HUB_VCS_ROOT_ID.equals(vcsRootId)) {
              throw new RunBuildException("Failed to find the GitHub VCS root ID and use it to resolve " + CodeBuildConstants.SOURCE_VERSION_LABEL + " AWS CodeBuild setting");
            }

            final String sysPropName = "build.vcs.number." + vcsRootId;
            final String sourceVersion = context.getBuildParameters().getSystemProperties().get(sysPropName);

            if (StringUtil.isEmptyOrSpaces(sourceVersion)) {
              throw new RunBuildException("Can't use empty %" + sysPropName + "% system property value as " + CodeBuildConstants.SOURCE_VERSION_LABEL + " AWS CodeBuild setting");
            }

            runningBuild.getBuildLogger().message("Using %" + sysPropName + "% system property value " + sourceVersion + " as the AWS CodeBuild source version");
            return sourceVersion;

          } else if (SourceType.S3.toString().equals(project.getSourceType())) {
            final File revision = prepareRevision();
            if (revision == null) {
              throw new RunBuildException("Unable to upload sources to the AWS S3: build checkout directory " + runningBuild.getCheckoutDirectory() + " is empty");
            }
            try {
              return AWSCommonParams.withAWSClients(params, new AWSCommonParams.WithAWSClients<String, RuntimeException>() {
                @Nullable
                @Override
                public String run(@NotNull AWSClients clients) throws RuntimeException {
                  return clients.createS3Client().putObject(getBucketName(project.getSourceLocation()), getObjectKey(project.getSourceLocation()), revision).getVersionId();
                }
              });
            } finally {
              FileUtil.delete(revision);
            }
          } else {
            throw new RunBuildException(CodeBuildConstants.USE_BUILD_REVISION_LABEL + " setting is supported only for Amazon S3 and GitHub AWS CodeBuild project source provider and can't be combined with " + project.getSourceType() + " source provider");
          }
        } else {
          return CodeBuildUtil.getSourceVersion(params);
        }
      }

      @Nullable
      private File prepareRevision() throws RunBuildException {
        final File[] files = runningBuild.getCheckoutDirectory().listFiles();
        if (files == null || files.length == 0) return null;
        if (files.length == 1 && files[0].getName().endsWith(".zip")) {
          return files[0];
        } else {
          final File revision = new File(runningBuild.getBuildTempDirectory() + "/" + runningBuild.getCheckoutDirectory().getName() + ".zip");
          try {
            ArchiveUtil.packZip(runningBuild.getCheckoutDirectory(), new ZipOutputStream(new FileOutputStream(revision)));
          } catch (FileNotFoundException e) {
            throw new RunBuildException("Failed to package the checkout directory content", e);
          }
          return revision;
        }
      }

      @NotNull
      private Collection<EnvironmentVariable> getEnvironmentVariables() {
        runningBuild.getBuildLogger().message("Will pass build system properties as Environment variables to the AWS CodeBuild");
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
      private Map<String, String> validateParams() throws RunBuildException {
        final Map<String, String> runnerParameters = context.getRunnerParameters();
        final Map<String, String> invalids = ParametersValidator.validateSettings(runnerParameters, false);
        if (invalids.isEmpty()) return runnerParameters;
        throw new RunBuildException(StringUtil.join(invalids.values(), "\n"), null, ErrorData.BUILD_RUNNER_ERROR_TYPE);
      }
    };
  }

  private void startContext(@NotNull CodeBuildBuildContext c, @NotNull AgentRunningBuild runningBuild) {
    log(runningBuild, getBlockStart(c));
    log(runningBuild, forContext(c, createTextMessage("Waiting for build " + c.codeBuildBuildId + " finish")));
  }

  @NotNull
  private BuildMessage1 getBlockStart(@NotNull CodeBuildBuildContext c) {
    return forContext(c, createBlockStart(c.codeBuildProjectName, BLOCK_TYPE_TARGET));
  }

  @NotNull
  private BuildMessage1 getBlockEnd(@NotNull CodeBuildBuildContext c) {
    return forContext(c, createBlockEnd(c.codeBuildProjectName, BLOCK_TYPE_TARGET));
  }

  @NotNull
  private BuildMessage1 forContext(@NotNull CodeBuildBuildContext c, @NotNull BuildMessage1 m) {
    return m.updateFlowId(c.codeBuildBuildId);
  }

  private void log(@NotNull AgentRunningBuild b, @NotNull BuildMessage1 m) {
    b.getBuildLogger().getFlowLogger(m.getFlowId()).logMessage(m);
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

    for (CodeBuildBuildContext c : myCodeBuildBuilds) {
      startContext(c, build);
    }

    while (!myCodeBuildBuilds.isEmpty()) {
      for (CodeBuildBuildContext next : new ArrayList<CodeBuildBuildContext>(myCodeBuildBuilds)) {
        final boolean buildInterrupted = build.getInterruptReason() != null;
        boolean finished = false;
        try {
          finished = buildInterrupted || finished(next, build);
          if (buildInterrupted) {
            interrupt(next, build);
          }
        } finally {
          if (finished) {
            myCodeBuildBuilds.remove(next);
            log(build, getBlockEnd(next));
          }
        }
      }
      if (build.getInterruptReason() == null) {
        try {
          Thread.sleep(CodeBuildConstants.POLL_INTERVAL);
        } catch (InterruptedException e) {
          break;
        }
      }
    }
    myCodeBuildBuilds.clear();
  }

  private boolean finished(@NotNull final CodeBuildBuildContext c, @NotNull AgentRunningBuild build) {
    final List<Build> builds = AWSCommonParams.withAWSClients(c.params, new AWSCommonParams.WithAWSClients<List<Build>, RuntimeException>() {
      @Nullable
      @Override
      public List<Build> run(@NotNull AWSClients clients) throws RuntimeException {
        return clients.createCodeBuildClient().batchGetBuilds(new BatchGetBuildsRequest().withIds(c.codeBuildBuildId)).getBuilds();
      }
    });

    if (builds == null || builds.isEmpty()) {
      log(build, forContext(c, createTextMessage("No AWS CodeBuild build with id=" + c.codeBuildBuildId + " found", Status.WARNING)));
      return true;
    }

    if (builds.size() > 1) {
      log(build, forContext(c, createTextMessage("Found several AWS CodeBuild builds with id=" + c.codeBuildBuildId + ". Will process the first one.", Status.WARNING)));
    }

    final Build codeBuildBuild = builds.iterator().next();

    reportPhases(codeBuildBuild, c, build);

    if (codeBuildBuild.getBuildComplete()) {
      // import logs in case of failure?
      final String format = getBuildString(c) + " %s " + getBuildLink(c.codeBuildBuildId, c.params.get(AWSCommonParams.REGION_NAME_PARAM));
      final String status = codeBuildBuild.getBuildStatus();
      if (isSucceeded(status)) {
        log(build, (forContext(c, createTextMessage(String.format(format, "succeeded")))));
      } else {
        log(build, (forContext(c, createTextMessage(String.format(format, isFailed(status) ? "failed" : "finished with status " + status), Status.ERROR))));
      }
      return true;
    }
    return false;
  }

  private void interrupt(@NotNull final CodeBuildBuildContext c, @NotNull AgentRunningBuild build) {
    log(build, forContext(c, createTextMessage("Stopping " + getBuildString(c), Status.WARNING)));
    AWSCommonParams.withAWSClients(c.params, new AWSCommonParams.WithAWSClients<Void, RuntimeException>() {
      @Nullable
      @Override
      public Void run(@NotNull AWSClients clients) throws RuntimeException {
        clients.createCodeBuildClient().stopBuild(new StopBuildRequest().withId(c.codeBuildBuildId));
        return null;
      }
    });
  }

  @NotNull
  private static String getBuildString(@NotNull CodeBuildBuildContext c) {
    return "Build " + c.codeBuildBuildId;
  }

  private void reportPhases(@NotNull Build codeBuildBuild, @NotNull CodeBuildBuildContext c, @NotNull AgentRunningBuild build) {
    if (codeBuildBuild.getPhases() == null || codeBuildBuild.getPhases().size() <= c.prevPhases.size()) return;

    for (BuildPhase phase : codeBuildBuild.getPhases()) {
      final String phaseName = phase.getPhaseType();
      if (isPhaseReported(phaseName, c)) continue;

      final String format = getFormat(phase, phaseName);
      final String status = phase.getPhaseStatus();
      if (status == null || isInProgress(status)) {
        if (c.prevPhases.get(phaseName) == null) { // not yet reported
          log(build, forContext(c, createProgressMessage(String.format(format, "in progress")).updateTags(DefaultMessagesInfo.TAG_INTERNAL)));
        }
        c.prevPhases.put(phaseName, status);
      } else {
        c.prevPhases.put(phaseName, status);

        if (isSucceeded(status)) {
          log(build, forContext(c, createTextMessage(String.format(format, "succeeded"))));
        } else {
          log(build, forContext(c, createTextMessage(String.format(format, isFailed(status) ? "failed" : "finished with status " + status), Status.ERROR)));
          log(build, forContext(c, createBuildProblemMessage(createBuildProblem(phase, c.params, build.getCheckoutDirectory().getAbsolutePath()))));
        }
      }
    }
  }

  private boolean isPhaseReported(@NotNull String phaseName, @NotNull CodeBuildBuildContext c) {
    final String status = c.prevPhases.get(phaseName);
    return status != null && !isInProgress(status);
  }

  @NotNull
  private String getFormat(@NotNull BuildPhase phase, @NotNull String phaseName) {
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
    final ArrayList<String> otherParts = new ArrayList<String>();
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
    @NotNull private final String codeBuildProjectName;
    @NotNull private final Map<String, String> params;
    @NotNull private Map<String, String> prevPhases = new HashMap<String, String>();

    private CodeBuildBuildContext(@NotNull String codeBuildBuildId, @NotNull String codeBuildProjectName, @NotNull Map<String, String> params) {
      this.codeBuildBuildId = codeBuildBuildId;
      this.codeBuildProjectName = codeBuildProjectName;
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
