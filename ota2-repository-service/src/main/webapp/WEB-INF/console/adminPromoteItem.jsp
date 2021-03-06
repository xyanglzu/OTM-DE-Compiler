<%--

    Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form id="confirmForm" action="${pageContext.request.contextPath}/console/adminPromoteItem.html" method="POST">
	<c:set var="nextStatus" value="${item.status.nextStatus()}"/>
	<c:if test="${otm16Enabled}">
		<span class="confirmMessage">Promote repository item "${item.filename}" to <spring:message code="${nextStatus.toString()}" /> status.
		<br/>Are you sure?</span>
	</c:if>
	<c:if test="${!otm16Enabled}">
		<span class="confirmMessage">Promote repository item "${item.filename}" to FINAL status.  The library will no longer be editable.<br/>Are you sure?</span>
	</c:if>
	<p><br>
	<input name="baseNamespace" type="hidden" value="${item.baseNamespace}" />
	<input name="filename" type="hidden" value="${item.filename}" />
	<input name="version" type="hidden" value="${item.version}" />
	<input name="confirmPromote" type="hidden" value="true" />
	<input type="submit" value="Promote Item" class="formButton" />
</form>
