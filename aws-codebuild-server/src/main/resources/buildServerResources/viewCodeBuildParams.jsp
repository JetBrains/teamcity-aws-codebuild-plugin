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

<%@include file="paramsConstants.jspf"%>

<jsp:include page="viewAWSCommonParams.jsp"/>

<div class="parameter">
    ${project_name_label}: <props:displayValue name="${project_name_param}" emptyValue="empty"/>
</div>

<div class="parameter">
    ${source_version_label}: <props:displayValue name="${source_version_param}" emptyValue="latest"/>
</div>

<div class="parameter">
    ${timeout_minutes_label}: <props:displayValue name="${timeout_minutes_param}" emptyValue="defult"/>
</div>
