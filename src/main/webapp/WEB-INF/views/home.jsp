<%@ page import="com.msal.model.UserProfile" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Home</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<div class="container mt-4">
    <h1>Welcome to Spring MSAL Demo</h1>

    <%
        UserProfile userProfile = (UserProfile) session.getAttribute("userInfo");
        if (userProfile != null) {
    %>
    <div class="card mt-3">
        <div class="card-body">
            <p class="card-text">Name: <%= userProfile.getName() %></p>
            <p class="card-text">Sub: <%= userProfile.getSub() %></p>
        </div>
    </div>
    <%
    } else {
    %>
    <p>No user information found in the session.</p>
    <%
        }
    %>

    <div class="mt-3">
        <form action="${pageContext.request.contextPath}/logout" method="post" class="d-inline">
            <button type="submit" class="btn btn-danger">Logout</button>
        </form>
    </div>
</div>
</body>
</html>