

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