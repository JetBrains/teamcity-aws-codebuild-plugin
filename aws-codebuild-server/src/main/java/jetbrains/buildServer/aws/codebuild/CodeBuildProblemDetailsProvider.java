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
