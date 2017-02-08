<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="projects" scope="request" type="java.util.Collection<java.lang.String>"/>

<div class="codeBuildPopup">
    <c:if test="${fn:length(projects) >= 1}">
        <c:set var="containerId"><bs:id/></c:set>
        <bs:inplaceFilter containerId="${containerId}" activate="true" filterText="&lt;filter projects>"/>
    </c:if>
    <ul id="${containerId}">
    <c:forEach items="${projects}" var="p">
        <li class="inplaceFiltered" onclick="BS.CodeBuildProjectNamePopup.fillProjectName('<bs:escapeForJs text="${p}"/>');"><c:out value="${p}"/></li>
    </c:forEach>
    </ul>
</div>