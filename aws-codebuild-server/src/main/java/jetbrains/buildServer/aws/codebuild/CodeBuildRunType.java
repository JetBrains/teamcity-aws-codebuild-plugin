

package jetbrains.buildServer.aws.codebuild;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vbedrosova
 */
public class CodeBuildRunType extends RunType {
  @NotNull
  private final PluginDescriptor myPluginDescriptor;
  @NotNull
  private final ServerSettings myServerSettings;

  public CodeBuildRunType(@NotNull RunTypeRegistry registry,
                          @NotNull PluginDescriptor pluginDescriptor,
                          @NotNull ServerSettings serverSettings) {
    registry.registerRunType(this);
    myPluginDescriptor = pluginDescriptor;
    myServerSettings = serverSettings;
  }

  @NotNull
  @Override
  public String getDescription() {
    return CodeBuildConstants.RUNNER_DESCRIPTION;
  }

  @Nullable
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new PropertiesProcessor() {
      @Override
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        return CollectionsUtil.convertCollection(ParametersValidator.validateSettings(properties, true).entrySet(), new Converter<InvalidProperty, Map.Entry<String, String>>() {
          @Override
          public InvalidProperty createFrom(@NotNull Map.Entry<String, String> source) {
            return new InvalidProperty(source.getKey(), source.getValue());
          }
        });
      }
    };
  }

  @Nullable
  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myPluginDescriptor.getPluginResourcesPath(CodeBuildConstants.EDIT_PARAMS_JSP);
  }

  @Nullable
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myPluginDescriptor.getPluginResourcesPath(CodeBuildConstants.VIEW_PARAMS_JSP);
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    final Map<String, String> defaults = new HashMap<>();
    defaults.putAll(AWSCommonParams.getDefaults(myServerSettings.getServerUUID()));
    defaults.putAll(CodeBuildConstants.DEFAULTS);
    return defaults;
  }

  @NotNull
  @Override
  public String getType() {
    return CodeBuildConstants.RUNNER_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return CodeBuildConstants.RUNNER_DISPLAY_NAME;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> parameters) {
    final Map<String, String> invalids = ParametersValidator.validateSettings(parameters, true);
    if (invalids.isEmpty()) {
      final StringBuilder descr = new StringBuilder("Run AWS CodeBuild in ");
      descr.append(CodeBuildUtil.getProjectName(parameters)).append(" project");

      final String sourceVersion = CodeBuildUtil.getSourceVersion(parameters);
      if (StringUtil.isNotEmpty(sourceVersion)) {
        descr.append(" with source version ").append(sourceVersion);
      } else {
        descr.append(" with the latest source version");
      }

      return descr.toString();
    } else {
      return StringUtil.join(invalids.values(), ", ");
    }
  }
}