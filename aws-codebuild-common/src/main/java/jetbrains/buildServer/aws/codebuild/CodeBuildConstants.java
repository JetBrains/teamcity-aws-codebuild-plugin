package jetbrains.buildServer.aws.codebuild;

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

  String BUILD_PROBLEM_TYPE = "CODEBUILD_FAILURE";
}
