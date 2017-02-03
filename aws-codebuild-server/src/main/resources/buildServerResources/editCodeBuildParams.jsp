<%--
  ~ Copyright 2000-2016 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<style type="text/css">
    .runnerFormTable .artifactsSetting {
        display: none;
    }
</style>

<%@include file="paramsConstants.jspf"%>
<jsp:include page="editAWSCommonParams.jsp"/>

<l:settingsGroup title="AWS CodeBuild settings">
    <tr>
        <th><label for="${project_name_param}">${project_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${project_name_param}" className="longField" maxlength="256"/>
            <span class="error" id="error_${project_name_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${source_version_param}">${source_version_label}:</label></th>
        <td><props:textProperty name="${source_version_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Version ID (if the source code is in Amazon S3) or commit ID (if AWS CodeCommit or GitHub).</span>
            <span class="smallNote">Leave blank to:</span>
            <span class="smallNote">- use TeamCity %build.vcs.number.&lt;VCS root ID&gt;% if there is a GitHub VCS root attached to the build type</span>
            <span class="smallNote">- build the the latest version.</span>
            <span class="error" id="error_${source_version_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${build_spec_param}">${build_spec_label}:</label></th>
        <td><props:multilineProperty name="${build_spec_param}" linkTitle="Enter the build specification" rows="10" cols="58" className="longField"/>
            <span class="smallNote">Build specification in YAML format. Leave blank to use the project's default build spec.</span>
            <span class="error" id="error_${build_spec_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${artifacts_param}">${artifacts_label}: <l:star/></label></th>
        <td><props:selectProperty name="${artifacts_param}" onchange="codeBuildUpdateArtifactsSettingsVisibility();" className="longField" enableFilter="true">
            <props:option value="${artifacts_none}">${artifacts_none_label}</props:option>
            <props:option value="${artifacts_s3}">${artifacts_s3_label}</props:option>
        </props:selectProperty></td>
    </tr>
    <tr class="advancedSetting artifactsSetting">
        <th><label for="${zip_param}">${zip_label}:</label></th>
        <td><props:checkboxProperty name="${zip_param}" onclick="codeBuildUpdateArtifactsName();"/></td>
    </tr>
    <tr class="advancedSetting artifactsSetting">
        <th><label for="${artifacts_name_param}">${artifacts_name_label}:</label></th>
        <td><props:textProperty name="${artifacts_name_param}" className="longField" maxlength="256"/>
            <span id="noteFolder" class="smallNote">Folder name. Leave blank to use the default value.</span>
            <span id="noteArchive" class="smallNote">Archive name. Leave blank to use the default value.</span>
            <span class="error" id="error_${artifacts_name_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting artifactsSetting">
        <th><label for="${bucket_param}">${bucket_label}: <l:star/></label></th>
        <td><props:textProperty name="${bucket_param}" className="longField" maxlength="256"/>
            <span class="smallNote">S3 bucket to upload artifacts to.</span>
            <span class="error" id="error_${bucket_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${timeout_minutes_param}">${timeout_minutes_label}:</label></th>
        <td><props:textProperty name="${timeout_minutes_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Build timeout must be 5 to 480 minutes. Leave blank to use the default value.</span>
            <span class="error" id="error_${timeout_minutes_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${wait_param}">${wait_label}:</label></th>
        <td><props:selectProperty name="${wait_param}" className="longField" enableFilter="true">
            <props:option value="${wait_none}">${wait_none_label}</props:option>
            <props:option value="${wait_build}">${wait_build_label}</props:option>
            <props:option value="${wait_step}">${wait_step_label}</props:option>
        </props:selectProperty></td>
    </tr>
</l:settingsGroup>
<script type="application/javascript">
    window.codeBuildUpdateArtifactsSettingsVisibility = function () {
        var showArtifactsSettings = $j('#runnerParams #${artifacts_param} option:selected').val() == '${artifacts_s3}';
        $j('#runnerParams .artifactsSetting').each(function() {
            if (showArtifactsSettings) BS.Util.show(this);
            else BS.Util.hide(this);
        });

        BS.VisibilityHandlers.updateVisibility('runnerParams');
    };
    window.codeBuildUpdateArtifactsName = function () {
        if ($j('#${zip_param}').is(':checked')) {
            BS.Util.show('noteArchive');
            BS.Util.hide('noteFolder');
        } else {
            BS.Util.hide('noteArchive');
            BS.Util.show('noteFolder');
        }
        BS.VisibilityHandlers.updateVisibility('runnerParams');
    };
    codeBuildUpdateArtifactsSettingsVisibility();
    codeBuildUpdateArtifactsName();
</script>
