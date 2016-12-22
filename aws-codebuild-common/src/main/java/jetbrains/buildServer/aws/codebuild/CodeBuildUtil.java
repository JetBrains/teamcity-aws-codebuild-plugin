package jetbrains.buildServer.aws.codebuild;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jetbrains.buildServer.aws.codebuild.CodeBuildConstants.*;

/**
 * @author vbedrosova
 */
public final class CodeBuildUtil {
  @Nullable
  public static String getProjectName(@NotNull Map<String, String> params) {
    return params.get(PROJECT_NAME_PARAM);
  }

  @Nullable
  public static String getSourceVersion(@NotNull Map<String, String> params) {
    return params.get(SOURCE_VERSION_PARAM);
  }
}
