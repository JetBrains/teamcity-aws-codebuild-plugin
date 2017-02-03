package jetbrains.buildServer.aws.codebuild;

import jetbrains.buildServer.util.CollectionsUtil;

import java.util.Map;

/**
 * @author vbedrosova
 */
public interface CodeBuildConstants {
  String RUNNER_TYPE = "aws.codeBuild";
  String RUNNER_DISPLAY_NAME = "AWS CodeBuild";
  String RUNNER_DESCRIPTION = "Build runner for building projects on AWS CodeBuild";

  String EDIT_PARAMS_JSP = "editCodeBuildParams.jsp";
  String VIEW_PARAMS_JSP = "viewCodeBuildParams.jsp";

  String PROJECT_NAME_PARAM = "codebuild_project_name";
  String PROJECT_NAME_LABEL = "Project name";

  String SOURCE_VERSION_PARAM = "codebuild_source_version";
  String SOURCE_VERSION_LABEL = "Source version";

  String BUILD_SPEC_PARAM = "codebuild_build_spec";
  String BUILD_SPEC_LABEL = "Build specification";

  String ARTIFACTS_PARAM = "codebuild_artifacts";
  String ARTIFACTS_NONE = "none";
  String ARTIFACTS_S3 = "s3";
  String ARTIFACTS_LABEL = "Artifacts type";
  String ARTIFACTS_NONE_LABEL = "No artifacts";
  String ARTIFACTS_S3_LABEL = "Amazon S3";

  String ARTIFACTS_S3_NAME_PARAM = "codebuild_artifacts_s3_name";
  String ARTIFACTS_S3_NAME_LABEL = "Artifacts name";

  String ARTIFACTS_S3_BUCKET_PARAM = "codebuild_artifacts_s3_bucket";
  String ARTIFACTS_S3_BUCKET_LABEL = "Bucket name";

  String ARTIFACTS_S3_ZIP_PARAM = "codebuild_artifacts_s3_zip";
  String ARTIFACTS_S3_ZIP_LABEL = "Zip artifacts";

  String WAIT_PARAM = "codebuild_wait";
  String WAIT_NONE = "none";
  String WAIT_STEP = "step";
  String WAIT_BUILD = "build";
  String WAIT_LABEL = "Wait";
  String WAIT_NONE_LABEL = "Do not wait";
  String WAIT_STEP_LABEL = "On step finish";
  String WAIT_BUILD_LABEL = "On build finish";

  String TIMEOUT_MINUTES_PARAM = "codebuild_timeout_minutes";
  String TIMEOUT_MINUTES_LABEL = "Timeout (minutes)";
  int DEFAULT_TIMEOUT_MINUTES = 60;

  String BUILD_PROBLEM_TYPE = "CODEBUILD_FAILURE";

  long POLL_INTERVAL = 10000;

  String SUCCEEDED = "SUCCEEDED";
  String FAILED = "FAILED";
  String IN_PROGRESS = "IN_PROGRESS";

  Map<String, String> DEFAULTS = CollectionsUtil.asMap(
    WAIT_PARAM, WAIT_BUILD,
    ARTIFACTS_PARAM, ARTIFACTS_NONE
  );

  String GIT_HUB_VCS_ROOT_ID_CONFIG_PARAM = "codebuild.github.vcs.root.id";
  String UNKNOWN_GIT_HUB_VCS_ROOT_ID = "<unknown>";
}
