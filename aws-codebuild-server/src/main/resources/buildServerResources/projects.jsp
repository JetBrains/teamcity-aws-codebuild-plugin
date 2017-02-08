<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="projects" scope="request" type="java.util.Collection<java.lang.String>"/>

<div class="codeBuildPopup">
    <ul>
    <c:forEach items="${projects}" var="p">
        <li onclick="BS.CodeBuildProjectNamePopup.fillProjectName('<bs:escapeForJs text="${p}"/>');"><c:out value="${p}"/></li>
    </c:forEach>
    </ul>
</div>