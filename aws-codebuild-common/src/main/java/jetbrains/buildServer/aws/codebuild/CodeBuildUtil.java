/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import com.amazonaws.services.codebuild.model.BatchGetProjectsRequest;
import com.amazonaws.services.codebuild.model.Project;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.amazon.AWSClients;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.aws.codebuild.CodeBuildConstants.*;

/**
 * @author vbedrosova
 */
public final class CodeBuildUtil {

  public static final String ARN_AWS_S3 = "arn:aws:s3:::";

  @Nullable
  public static String getProjectName(@NotNull Map<String, String> params) {
    return params.get(PROJECT_NAME_PARAM);
  }

  public static boolean isUseBuildRevision(@NotNull Map<String, String> params) {
    return Boolean.parseBoolean(params.get(USE_BUILD_REVISION_PARAM));
  }

  @Nullable
  public static String getSourceVersion(@NotNull Map<String, String> params) {
    return params.get(SOURCE_VERSION_PARAM);
  }

  @Nullable
  public static String getBuildSpec(@NotNull Map<String, String> params) {
    return params.get(BUILD_SPEC_PARAM);
  }

  public static boolean isUploadS3Artifacts(@NotNull Map<String, String> params) {
    return ARTIFACTS_S3.equals(params.get(ARTIFACTS_PARAM));
  }

  public static boolean isZipS3Artifacts(@NotNull Map<String, String> params) {
    return Boolean.parseBoolean(params.get(ARTIFACTS_S3_ZIP_PARAM));
  }

  @Nullable
  public static String getArtifactS3Name(@NotNull Map<String, String> params) {
    return params.get(ARTIFACTS_S3_NAME_PARAM);
  }

  @Nullable
  public static String getArtifactS3Bucket(@NotNull Map<String, String> params) {
    return params.get(ARTIFACTS_S3_BUCKET_PARAM);
  }

  @Nullable
  public static String getTimeoutMinutes(@NotNull Map<String, String> params) {
    return params.get(TIMEOUT_MINUTES_PARAM);
  }

  public static boolean isWaitStep(@NotNull Map<String, String> params) {
    return WAIT_STEP.equals(params.get(WAIT_PARAM));
  }

  public static boolean isWaitBuild(@NotNull Map<String, String> params) {
    return WAIT_BUILD.equals(params.get(WAIT_PARAM));
  }

  @NotNull
  public static int getTimeoutMinutesInt(@NotNull Map<String, String> params) {
    try {
      return Integer.parseInt(params.get(TIMEOUT_MINUTES_PARAM));
    } catch (NumberFormatException e) {
      return DEFAULT_TIMEOUT_MINUTES;
    }
  }

  public static boolean isSucceeded(@NotNull String status) {
    return CodeBuildConstants.SUCCEEDED.equals(status);
  }

  public static boolean isInProgress(@NotNull String status) {
    return CodeBuildConstants.IN_PROGRESS.equals(status);
  }

  public static boolean isFailed(@NotNull String status) {
    return CodeBuildConstants.FAILED.equals(status);
  }

  @NotNull
  public static String getBuildLink(@NotNull String buildId, @NotNull String region) {
    return String.format("https://console.aws.amazon.com/codebuild/home?region=%s#/builds/%s/view/new", region, buildId);
  }

  @NotNull
  public static String getBuildLogLink(@NotNull String buildId, @NotNull String projectName, @NotNull String region) {
    return String.format("https://console.aws.amazon.com/cloudwatch/home?region=%s#logEventViewer:group=/aws/codebuild/%s;stream=%s", region, projectName, buildId.replace(projectName + ":", ""));
  }

  @NotNull
  public static List<ProjectInfo> getProjects(@NotNull Map<String, String> params, @NotNull final Collection<String> names) {
    return AWSCommonParams.withAWSClients(params, new AWSCommonParams.WithAWSClients<List<ProjectInfo>, RuntimeException>() {
      @Nullable
      @Override
      public List<ProjectInfo> run(@NotNull AWSClients clients) throws RuntimeException {
        return CollectionsUtil.convertCollection(clients.createCodeBuildClient().batchGetProjects(new BatchGetProjectsRequest().withNames(names)).getProjects(), new Converter<ProjectInfo, Project>() {
          @Override
          public ProjectInfo createFrom(@NotNull Project p) {
            return new ProjectInfo(p.getName(), p.getSource().getType(), p.getSource().getLocation());
          }
        });
      }
    });
  }

  @Nullable
  public static ProjectInfo getProject(@NotNull Map<String, String> params, @NotNull String name) {
    final List<ProjectInfo> projects = getProjects(params, Collections.singletonList(name));
    return projects.isEmpty() ? null : projects.get(0);
  }

  @Nullable
  public static String getBucketName(@Nullable String location) {
    if (location == null) return null;
    final int slashIndex = location.indexOf("/");
    return slashIndex > 0 ? removeServicePrefix(location.substring(0, slashIndex)) : null;
  }

  @Nullable
  public static String getObjectKey(@Nullable String location) {
    if (location == null) return null;
    final int slashIndex = location.indexOf("/");
    return slashIndex > 0 && slashIndex + 1 < location.length() ? location.substring(slashIndex + 1) : null;
  }

  @NotNull
  private static String removeServicePrefix(@NotNull String location) {
    return location.startsWith(ARN_AWS_S3) ? location.substring(ARN_AWS_S3.length()) : location;
  }

  public static class ProjectInfo {
    @NotNull
    private final String myName;
    @NotNull
    private final String mySourceType;
    @NotNull
    private final String mySourceLocation;


    private ProjectInfo(@NotNull String name, @NotNull String sourceType, @NotNull String sourceLocation) {
      myName = name;
      mySourceType = sourceType;
      mySourceLocation = sourceLocation;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    @NotNull
    public String getSourceType() {
      return mySourceType;
    }

    @NotNull
    public String getSourceLocation() {
      return mySourceLocation;
    }
  }
}
