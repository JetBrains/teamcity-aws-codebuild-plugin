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

  @Nullable
  public static String getBuildSpec(@NotNull Map<String, String> params) {
    return params.get(BUILD_SPEC_PARAM);
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
}
