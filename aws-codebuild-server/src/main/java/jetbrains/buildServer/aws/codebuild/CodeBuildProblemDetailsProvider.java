

package jetbrains.buildServer.aws.codebuild;

import jetbrains.buildServer.serverSide.problems.BaseBuildProblemTypeDetailsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public class CodeBuildProblemDetailsProvider extends BaseBuildProblemTypeDetailsProvider {
  @NotNull
  @Override
  public String getType() {
    return CodeBuildConstants.BUILD_PROBLEM_TYPE;
  }

  @Nullable
  @Override
  public String getTypeDescription() {
    return CodeBuildConstants.RUNNER_DISPLAY_NAME;
  }
}