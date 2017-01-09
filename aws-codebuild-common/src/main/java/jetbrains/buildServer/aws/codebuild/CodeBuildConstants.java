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

  Map<String, String> DEFAULTS = CollectionsUtil.asMap(WAIT_PARAM, WAIT_BUILD);

  String ENV_VAR_PREFIX = "aws.codeBuild.";
}
