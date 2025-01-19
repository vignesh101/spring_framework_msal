<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Login</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="container mt-5">
<div class="row justify-content-center">
  <div class="col-md-6">
    <div class="card">
      <div class="card-header">
        <h2 class="text-center">Login</h2>
      </div>
      <div class="card-body">
        <c:if test="${error != null}">
          <div class="alert alert-danger">${error}</div>
        </c:if>
        <c:if test="${message != null}">
          <div class="alert alert-success">${message}</div>
        </c:if>

        <!-- Form Login -->
        <form action="${pageContext.request.contextPath}/login" method="post">
          <div class="mb-3">
            <label for="username" class="form-label">Username:</label>
            <input type="text" class="form-control" id="username" name="username" required>
          </div>
          <div class="mb-3">
            <label for="password" class="form-label">Password:</label>
            <input type="password" class="form-control" id="password" name="password" required>
          </div>
          <div class="d-grid gap-2">
            <button type="submit" class="btn btn-primary">Login</button>
          </div>
        </form>

        <hr>

        <!-- Microsoft Login -->
        <div class="d-grid gap-2">
          <a href="${pageContext.request.contextPath}/auth/microsoft" class="btn btn-secondary">
            <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/icons/microsoft.svg" alt="Microsoft" width="20">
            Login with Microsoft
          </a>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>