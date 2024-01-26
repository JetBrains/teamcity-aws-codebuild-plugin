<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>



<jsp:useBean id="invalids" scope="request" type="java.util.Map"/>

<script type="text/javascript">BS.CodeBuildProjectNamePopup.hidePopup(0, true);</script>
<c:forEach items="${invalids}" var="e">
    <script type="text/javascript">BS.CodeBuildFakeForm.showError('${e.key}', '<bs:escapeForJs text="${e.value}"/>');</script>
</c:forEach>