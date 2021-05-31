<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.database.*" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.UserInfo" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.Constants" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<jsp:include page="/jsp/header_main.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminUserMgmt"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <%
        String editUser = request.getParameter("editUser");
        if (editUser != null && editUser.length() > 0 && StringUtils.isNumeric(editUser)) {
            User user = UserDAO.getUserById(Integer.parseInt(editUser));
            int pwMinLength = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();
            if (user != null) {
    %>
        <h3>Editing User <%=user.getId()%></h3>

        <form id="editUser" action="<%=request.getContextPath() + Paths.ADMIN_USERS%>" method="post" class="needs-validation mb-4">
            <input type="hidden" name="formType" value="editUser">
            <input type="hidden" name="uid" value="<%=user.getId()%>">

            <div class="row mb-3">
                <div class="col-sm-12">
                    <label for="name" class="form-label">Username</label>
                    <div class="input-group has-validation">
                        <span class="input-group-text">
                            <i class="fa fa-user"></i>
                        </span>
                        <input id="name" type="text" class="form-control" name="name" value="<%=user.getUsername()%>" placeholder="Username"
                               required minlength="3" maxlength="20" pattern="[a-z][a-zA-Z0-9]*" autofocus>
                        <div class="invalid-feedback">
                            Please enter a valid username.
                        </div>
                    </div>
                    <div class="form-text">
                        3-20 alphanumerics starting with a lowercase letter (a-z), no space or special characters.
                    </div>
                </div>
            </div>

            <div class="row mb-3">
                <div class="col-sm-12">
                    <label for="email" class="form-label">Email</label>
                    <div class="input-group has-validation">
                        <span class="input-group-text">
                            <i class="fa fa-envelope"></i>
                        </span>
                        <input id="email" type="email" class="form-control" name="email" value="<%=user.getEmail()%>" placeholder="Email"
                               required>
                        <div class="invalid-feedback">
                            Please enter a valid email address.
                        </div>
                    </div>
                </div>
            </div>

            <div class="row mb-3">
                <div class="col-sm-12">
                    <label for="password" class="form-label">Password</label>
                    <div class="input-group has-validation">
                        <span class="input-group-text">
                            <i class="fa fa-key"></i>
                        </span>
                        <input id="password" type="password" class="form-control"
                               name="password" placeholder="Password (leave empty for unchanged)"
                               minlength="<%=pwMinLength%>" maxlength="20" pattern="[a-zA-Z0-9]*">
                        <div class="invalid-feedback">
                            Please enter a valid password.
                        </div>
                    </div>

                    <div class="input-group has-validation mt-2">
                        <span class="input-group-text">
                            <i class="fa fa-key"></i>
                        </span>
                        <input id="confirm_password" type="password" class="form-control"
                               name="confirm_password" placeholder="Confirm Password">
                        <div class="invalid-feedback" id="confirm-password-feedback">
                            Please confirm your password.
                        </div>
                    </div>
                    <div class="form-text">
                        <%=pwMinLength%>-20 alphanumeric characters, no whitespace or special characters.
                    </div>
                </div>
            </div>

            <div class="row g-2">
                <div class="col-auto">
                    <a href="<%=request.getContextPath() + Paths.ADMIN_USERS%>" class="btn btn-secondary">Cancel</a>
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-primary btn-block" name="submit_edit_user" id="submit_edit_user">Save</button>
                </div>
            </div>

            <script>
                $(document).ready(() => {
                    const passwordInput = document.getElementById('password');
                    const confirmPasswordInput = document.getElementById('confirm_password');
                    const confirmPasswordFeedback = document.getElementById('confirm-password-feedback');

                    const validateConfirmPassword = function () {
                        if (passwordInput.value === confirmPasswordInput.value)  {
                            confirmPasswordInput.setCustomValidity('');
                            confirmPasswordFeedback.innerText = '';
                        } else {
                            confirmPasswordInput.setCustomValidity('password-mismatch');
                            confirmPasswordFeedback.innerText = "Passwords don't match.";
                        }
                    };

                    passwordInput.addEventListener('input', validateConfirmPassword);
                    confirmPasswordInput.addEventListener('input', validateConfirmPassword);
                });
            </script>
        </form>
    <%
            }
        }
    %>

    <h3>Users</h3>

    <form id="manageUsers" action="<%=request.getContextPath() + Paths.ADMIN_USERS%>" method="post">
        <input type="hidden" name="formType" value="manageUsers">

        <%
            List<UserInfo> unassignedUsersInfo = AdminDAO.getAllUsersInfo();
            if (unassignedUsersInfo.isEmpty()) {
        %>
                <div class="card">
                    <div class="card-body text-muted text-center">
                        There are currently no created users.
                    </div>
                </div>
        <%
            } else {
        %>
            <table id="tableUsers" class="table table-striped">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>User</th>
                        <th>Email</th>
                        <th>Total Score</th>
                        <th>Last Login</th>
                        <th></th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        for (UserInfo userInfo : unassignedUsersInfo) {
                            int userId = userInfo.getUser().getId();
                            String username = userInfo.getUser().getUsername();
                            String email = userInfo.getUser().getEmail();
                            boolean active = userInfo.getUser().isActive();
                            String lastLogin = userInfo.getLastLoginString();
                            int totalScore = userInfo.getTotalScore();
                    %>
                        <tr id="<%="user_row_"+userId%>" <%=active ? "" : "class=\"text-muted\""%>>
                            <td>
                                <%=userId%>
                                <input type="hidden" name="added_uid" value=<%=userId%>>
                            </td>
                            <td><%=username%></td>
                            <td><%=email%></td>
                            <td><%=totalScore%></td>
                            <td><%=lastLogin%></td>
                            <td>
                                <button class="btn btn-sm btn-primary" id="<%="edit_user_"+userId%>" name="editUserInfo" type="submit" value="<%=userId%>">
                                    <i class="fa fa-edit"></i>
                                </button>
                            </td>
                            <td>
                                <% if (login.getUserId() != userId) { %>
                                    <button class="btn btn-sm btn-danger" id="<%="inactive_user_"+userId%>" type="submit" value="<%=userId%>" name="setUserInactive"
                                            <% if (!active) { %>
                                                title="User is already set inactive." disabled
                                            <% } %>
                                            onclick="return confirm('Are you sure you want to set <%=username%>\'s account to inactive?');">
                                        <i class="fa fa-power-off"></i>
                                    </button>
                                <% } %>
                            </td>
                        </tr>
                    <%
                        }
                    %>
                </tbody>
            </table>
        <%
            }
        %>

        <script>
            $(document).ready(function () {
                $('#tableUsers').DataTable({
                    searching: true,
                    order: [[4, "desc"]],
                    "columnDefs": [{
                        "targets": 5,
                        "orderable": false
                    }, {
                        "targets": 6,
                        "orderable": false
                    }],
                    scrollY: '800px',
                    scrollCollapse: true,
                    paging: false,
                    language: {info: 'Showing _TOTAL_ entries'}
                });
            });
        </script>
    </form>

    <h3 class="mt-4">Create Accounts</h3>

    <form id="createUsers" action="<%=request.getContextPath() + Paths.ADMIN_USERS%>" method="post">
        <input type="hidden" name="formType" value="createUsers">

        <div class="row mb-3">
            <div class="col-sm-12">
                <label for="user_name_list" class="form-label">
                    <a data-bs-toggle="collapse" data-bs-target="#demo" class="text-decoration-none text-reset cursor-pointer">
                        List of user credentials
                        <span class="fa fa-question-circle"></span>
                    </a>
                </label>
                <div id="demo" class="collapse card mb-2">
                    <div class="card-body">
                        <p>List of usernames, passwords and (optional) emails.</p>
                        <p class="m-0">Fields are separated by commas (<code>,</code>) or semicolons (<code>;</code>).</p>
                        <p class="m-0">Users are separated by new lines.</p>
                        <p>If an email is provided and sending emails is enabled, created users receive an email with their credentials.</p>
                        <p class="mb-2">Valid input format examples:</p>
                        <pre class="m-0"><code>username,password
username2,password,example@mail.com
username3;password
username4;password;example@mail.com</code></pre>
                    </div>
                </div>
                <textarea class="form-control" rows="5" id="user_name_list" name="user_name_list"
                          oninput="document.getElementById('submit_users_btn').disabled = this.value.length === 0;"></textarea>
            </div>
        </div>

        <div class="row">
            <div class="col-auto">
                <button class="btn btn-primary" type="submit" name="submit_users_btn" id="submit_users_btn" disabled>
                    Create Accounts
                </button>
            </div>
        </div>

    </form>
</div>

<%@ include file="/jsp/footer.jsp" %>
