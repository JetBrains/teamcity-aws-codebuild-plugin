<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
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

<%@include file="paramsConstants.jspf"%>

<jsp:include page="viewAWSCommonParams.jsp"/>

<div class="parameter">
    ${project_name_label}: <props:displayValue name="${project_name_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${source_version_label}: <props:displayValue name="${source_version_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${build_spec_label}: <props:displayValue name="${build_spec_param}" emptyValue="default" showInPopup="true" popupTitle="Build specification" popupLinkText="view build spec" syntax="yaml"/>
</div>

<c:set var="artifacts" value="${propertiesBean.properties[artifacts_param]}"/>
<div class="parameter">
    ${artifacts_label}: <strong><c:choose><c:when test="${artifacts_s3 eq artifacts}">${artifacts_s3_label}</c:when>
    <c:otherwise>${artifacts_none_label}</c:otherwise>
</c:choose></strong>
</div>
<c:if test="${artifacts_s3 eq artifacts}">
    <div class="parameter">
       ${zip_label}: <strong><props:displayCheckboxValue name="${zip_param}"/></strong>
    </div>
    <div class="parameter">
        ${artifacts_name_label}: <props:displayValue name="${artifacts_name_param}" emptyValue="empty"/>
    </div>
    <div class="parameter">
        ${bucket_label}: <props:displayValue name="${bucket_param}" emptyValue="empty"/>
    </div>
</c:if>

<div class="parameter">
    ${timeout_minutes_label}: <props:displayValue name="${timeout_minutes_param}" emptyValue="default"/>
</div>

<c:set var="wait" value="${propertiesBean.properties[wait_param]}"/>
<div class="parameter">
    ${wait_label}: <strong><c:choose><c:when test="${wait_build eq wait}">${wait_build_label}</c:when>
        <c:when test="${wait_step eq wait}">${wait_step_label}</c:when>
        <c:otherwise>${wait_none_label}</c:otherwise>
    </c:choose></strong>
</div>
