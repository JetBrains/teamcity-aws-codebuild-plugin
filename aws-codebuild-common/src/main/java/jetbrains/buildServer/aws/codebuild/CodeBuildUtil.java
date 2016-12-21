package jetbrains.buildServer.aws.codebuild;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static jetbrains.buildServer.aws.codebuild.CodeBuildConstants.*;

/**
 * @author vbedrosova
 */
public final class CodeBuildUtil {
  @NotNull
  public static String getProjectName(@NotNull Map<String, String> params) {
    return params.get(PROJECT_NAME_PARAM);
  }
}
