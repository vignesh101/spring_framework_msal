<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Welcome</title>
</head>
<body>
<div class="container mt-4">
    <h1>Welcome to Spring MSAL Demo</h1>

    <p>Welcome to the User Home Page</p>

    <div class="mt-3">
        <form action="${pageContext.request.contextPath}/logout" method="post" class="d-inline">
            <button type="submit" class="btn btn-danger">Logout</button>
        </form>
    </div>
</div>
</body>
</html>