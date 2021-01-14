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

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.impl.BuildTypeImpl;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;
import jetbrains.buildServer.vcs.impl.VcsRootInstanceImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * @author vbedrosova
 */
public class GitHubVCSRootIdParameterProvider extends AbstractBuildParametersProvider {
  @NotNull
  @Override
  public Map<String, String> getParameters(@NotNull SBuild build, boolean emulationMode) {
    if (!BuildTypeImpl.isInsideVcsRootsResolving()) {
      for (VcsRootInstanceEntry e : build.getVcsRootEntries()) {
        if ("jetbrains.git".equals(e.getVcsName()) && e.getProperties().get("url").contains("github.com")) {
          final String vcsRootId = e.getVcsRoot() instanceof VcsRootInstanceImpl ? ((VcsRootInstanceImpl) e.getVcsRoot()).getExternalId() : CodeBuildConstants.UNKNOWN_GIT_HUB_VCS_ROOT_ID;
          return Collections.singletonMap(CodeBuildConstants.GIT_HUB_VCS_ROOT_ID_CONFIG_PARAM, vcsRootId);
        }
      }
    }
    return Collections.emptyMap();
  }
}
