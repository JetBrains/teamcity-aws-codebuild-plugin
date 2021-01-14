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

import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.aws.codebuild.CodeBuildConstants.*;
import static jetbrains.buildServer.aws.codebuild.CodeBuildUtil.*;

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
    if (StringUtil.isEmptyOrSpaces(projectName)) {
      invalids.put(PROJECT_NAME_PARAM, PROJECT_NAME_LABEL + " mustn't be empty");
    }

    if (isUploadS3Artifacts(params)) {
      if (StringUtil.isEmptyOrSpaces(getArtifactS3Bucket(params))) {
        invalids.put(ARTIFACTS_S3_BUCKET_PARAM, ARTIFACTS_S3_BUCKET_LABEL + " mustn't be empty");
      }
    }

    final String timeoutMinutes = getTimeoutMinutes(params);
    if (StringUtil.isNotEmpty(timeoutMinutes)) {
      try {
        final int timeoutMinutesInt = Integer.parseInt(timeoutMinutes);
        if (timeoutMinutesInt < 5 || timeoutMinutesInt > 480) {
          invalids.put(TIMEOUT_MINUTES_PARAM, TIMEOUT_MINUTES_LABEL + " must be 5 to 480 minutes");
        }
      } catch (NumberFormatException e) {
        invalids.put(TIMEOUT_MINUTES_PARAM, TIMEOUT_MINUTES_LABEL + " must be 5 to 480 minutes");
      }
    }

    return invalids;
  }
//
//  private static boolean isReference(@NotNull String param, boolean acceptReference) {
//    return ReferencesResolverUtil.containsReference(param, new String[]{}, true) && acceptReference;
//  }
}
