

package jetbrains.buildServer.aws.codebuild;

import com.amazonaws.services.codebuild.AWSCodeBuildClient;
import com.amazonaws.services.codebuild.model.ListProjectsRequest;
import com.amazonaws.services.codebuild.model.ListProjectsResult;
import com.amazonaws.services.codebuild.model.ProjectSortByType;
import com.amazonaws.services.codebuild.model.SortOrderType;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author vbedrosova
 */
public class CodeBuildListProjectsController extends BaseController {

  @NotNull
  private final PluginDescriptor myPluginDescriptor;

  public CodeBuildListProjectsController(@NotNull WebControllerManager controllerManager, @NotNull PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
    controllerManager.registerController(myPluginDescriptor.getPluginResourcesPath("listProjects.html"), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    final Map<String, String> params = gerParams(request);
    final Map<String, String> invalids = AWSCommonParams.validate(params, false);
    if (invalids.isEmpty()) {
      final Map<String, Object> model = new HashMap<>();
      try {
        model.put("projects", getProjects(params));
      } catch (Throwable e) {
        return handle("errors.jsp", Collections.<String, Object>singletonMap("invalids", Collections.singletonMap(CodeBuildConstants.PROJECT_NAME_PARAM, e.getMessage())));
      }
      return handle("projects.jsp", model);
    } else {
      return handle("errors.jsp", Collections.<String, Object>singletonMap("invalids", invalids)); // escaped in the jsp
    }
  }

  @NotNull
  private List<CodeBuildUtil.ProjectInfo> getProjects(@NotNull Map<String, String> params) {
    return AWSCommonParams.withAWSClients(params, clients -> {
      final AWSCodeBuildClient client = clients.createCodeBuildClient();
      final List<CodeBuildUtil.ProjectInfo> res = new ArrayList<>();
      String nextToken = null;
      do {
        final ListProjectsResult result = client.listProjects(new ListProjectsRequest().withSortBy(ProjectSortByType.LAST_MODIFIED_TIME).withSortOrder(SortOrderType.DESCENDING)).withNextToken(nextToken);
        if (result.getProjects().isEmpty()) break;
        res.addAll(CodeBuildUtil.getProjects(params, result.getProjects()));
        nextToken = result.getNextToken();
      } while (StringUtil.isNotEmpty(nextToken));
      return res;
    });
  }

  @NotNull
  private Map<String, String> gerParams(@NotNull HttpServletRequest request) {
    final BasePropertiesBean propertiesBean = new BasePropertiesBean(Collections.<String, String>emptyMap());
    PluginPropertiesUtil.bindPropertiesFromRequest(request, propertiesBean, true);
    return propertiesBean.getProperties();
  }

  @NotNull
  private ModelAndView handle(@NotNull String view, @NotNull Map<String, Object> model) {
    return new ModelAndView(myPluginDescriptor.getPluginResourcesPath(view), model);
  }
}