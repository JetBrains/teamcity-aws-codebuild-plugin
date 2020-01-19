<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%--
  ~ Copyright 2000-2020 JetBrains s.r.o.
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

<jsp:useBean id="projects" scope="request" type="java.util.Collection<java.lang.String>"/>

<div class="codeBuildPopup">
    <c:if test="${fn:length(projects) >= 5}">
        <c:set var="containerId"><bs:id/></c:set>
        <bs:inplaceFilter containerId="${containerId}" activate="true" filterText="&lt;filter projects>"/>
    </c:if>
    <ul id="${containerId}">
    <c:forEach items="${projects}" var="p">
        <li class="inplaceFiltered" onclick="BS.CodeBuildProjectNamePopup.fillProjectName('<bs:escapeForJs text="${p.name}"/>', '${p.sourceType}');"><c:out value="${p.name}"/></li>
    </c:forEach>
    </ul>
    <c:if test="${empty projects}"><span class="italic">No projects found</span></c:if>
</div>