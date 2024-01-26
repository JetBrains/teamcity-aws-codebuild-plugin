

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<bs:linkScript>
    /js/bs/basePopup.js
    /js/bs/pluginProperties.js
    ${teamcityPluginResourcesPath}scripts.js
</bs:linkScript>
<bs:linkCSS dynamic="${true}">
    ${teamcityPluginResourcesPath}styles.css
</bs:linkCSS>


<%@include file="paramsConstants.jspf"%>
<jsp:include page="editAWSCommonParams.jsp"/>

<l:settingsGroup title="AWS CodeBuild settings">
    <tr>
        <th><label for="${project_name_param}">${project_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${project_name_param}" className="longField" maxlength="256"/><span class="icon-magic magicButton" onclick="BS.CodeBuildProjectNamePopup.showPopup(this);" title="Suggest project name"></span>
            <span class="error" id="error_${project_name_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${source_version_param}">${source_version_label}: <l:star/></label></th>
        <td><props:radioButtonProperty name="${use_build_revision_param}" value="false" id="${use_build_revision_param}_false"/><props:textProperty name="${source_version_param}" className="longField" maxlength="256"/>
            <span class="smallNote">For GitHub or AWS CodeCommit: Commit ID. For S3: Version ID.</span>
            <span class="smallNote">Leave blank to build the the latest version.</span>
            <span class="error" id="error_${source_version_param}"></span>
            <br/>
            <props:radioButtonProperty name="${use_build_revision_param}" value="true" id="${use_build_revision_param}_true"/><label for="${use_build_revision_param}_true">${use_build_revision_label}</label>
            <span class="smallNote">For GitHub: use TeamCity %build.vcs.number.&lt;VCS root ID&gt;% as source version if there is a GitHub VCS root attached to the build configuration</span>
            <span class="smallNote">For S3: zip checkout directory contents, upload to S3 and use as the source code</span>
            <span class="error" id="error_${use_build_revision_param}"></span></td>
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
        <td><props:selectProperty name="${artifacts_param}" onchange="BS.CodeBuild.updateArtifactsSettingsVisibility();" className="longField" enableFilter="true">
            <props:option value="${artifacts_none}">${artifacts_none_label}</props:option>
            <props:option value="${artifacts_s3}">${artifacts_s3_label}</props:option>
        </props:selectProperty>
        <span class="smallNote">Will override the project's default artifact settings.</span>
        <span class="error" id="error_${artifacts_param}"></span></td>
    </tr>
    <tr class="advancedSetting artifactsSetting">
        <th><label for="${zip_param}">${zip_label}:</label></th>
        <td><props:checkboxProperty name="${zip_param}" onclick="BS.CodeBuild.updateArtifactsName();"/></td>
    </tr>
    <tr class="advancedSetting artifactsSetting">
        <th><label for="${artifacts_name_param}">${artifacts_name_label}:</label></th>
        <td><props:textProperty name="${artifacts_name_param}" className="longField" maxlength="256"/>
            <span id="noteFolder" class="smallNote">Folder name. Leave blank to use the project's default value.</span>
            <span id="noteArchive" class="smallNote">Archive name. Leave blank to use the project's default value.</span>
            <span class="error" id="error_${artifacts_name_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting artifactsSetting">
        <th><label for="${bucket_param}">${bucket_label}: <l:star/></label></th>
        <td><props:textProperty name="${bucket_param}" className="longField" maxlength="256"/>
            <span class="smallNote">S3 bucket to upload artifacts to. Leave blank to use the project's default value.</span>
            <span class="error" id="error_${bucket_param}"></span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${timeout_minutes_param}">${timeout_minutes_label}:</label></th>
        <td><props:textProperty name="${timeout_minutes_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Build timeout must be 5 to 480 minutes. Leave blank to use the project's default value.</span>
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