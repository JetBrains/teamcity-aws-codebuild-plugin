package jetbrains.buildServer.aws.codebuild;

import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.aws.codebuild.CodeBuildUtil.*;
import static jetbrains.buildServer.aws.codebuild.CodeBuildConstants.*;

/**
 * @author vbedrosova
 */
public class ParametersValidator {
  /**
   * Returns map from parameter name to invalidity reason
   */
  @NotNull
  static Map<String, String> validateSettings(@NotNull Map<String, String> params, boolean acceptReferences) {
    final Map<String, String> invalids = new HashMap<String, String>();

    invalids.putAll(AWSCommonParams.validate(params, acceptReferences));

    final String projectName = getProjectName(params);
    if (StringUtil.isEmpty(projectName)) {
      invalids.put(PROJECT_NAME_PARAM, PROJECT_NAME_LABEL + " mustn't be empty");
    }

    return invalids;
  }
//
//  private static boolean isReference(@NotNull String param, boolean acceptReference) {
//    return ReferencesResolverUtil.containsReference(param, new String[]{}, true) && acceptReference;
//  }
}
