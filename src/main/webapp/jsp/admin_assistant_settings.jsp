
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Smart Assistant"); %>

<jsp:include page="/jsp/header.jsp"/>

<link href="${url.forPath("/css/specific/smart_assistant.css")}" rel="stylesheet">

<div class="container">
    <% request.setAttribute("adminActivePage", "adminSmartAssistant"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <form>

        <table id="users-settings-table" class="table table-striped table-v-align-middle">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>User</th>
                    <th>Email</th>
                    <th>Total Questions</th>
                    <th>Smart Assistant</th>
                </tr>
            </thead>
            <tbody id="users-settings-table-body">

            </tbody>
        </table>

        <div class="row g-2 justify-content-end">
            <div class="col-auto">
                <button type="submit" class="btn btn-primary" id="users-save-btn">Save</button>
            </div>
            <div class="col-auto">
                <button type="button" class="btn btn-secondary" id="users-cancel-btn">Cancel</button>
            </div>
        </div>

    </form>

    <h3 class="mt-4 mb-3">Assistant Global Settings</h3>
    <div class="row">
        <div class="col-md-6 col-12">
            <div class="card mb-3">
                <div class="card-header">
                    Amount of questions per day
                </div>
                <div class="card-body">
                    <table id="questions-amount-table" class="table table-striped table-v-align-middle">
                        <thead>
                        <tr>
                            <th>Date</th>
                            <th>Total Questions</th>
                        </tr>
                        </thead>
                        <tbody id="questions-amount-table-body">

                        </tbody>
                    </table>
                </div>
                <div class="card-footer">
                    Total amount of questions until now: <b id="total-questions-amount"></b>
                </div>
            </div>

            <div class="card mb-3">
                <div class="card-header">
                    The Smart Assistant is <b id="assistant-status"></b>
                </div>
                <div class="card-body">
                    <div class="row g-2">
                        <div class="col-auto">
                            <button type="submit" class="btn btn-primary" id="enable-btn">Enable Assistant</button>
                        </div>
                        <div class="col-auto">
                            <button type="button" class="btn btn-danger" id="disable-btn">Disable Assistant</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-6 col-12 mb-3">
            <div class="card h-100">
                <div class="card-header">
                    <a class="text-decoration-none text-reset cursor-pointer" data-bs-toggle="modal" data-bs-target="#prompt-explanation">
                        Prompt
                        <i class="fa fa-question-circle ms-1"></i>
                    </a>
                </div>
                <div class="card-body">
                    <div class="d-flex flex-column h-100">
                        <label for="prompt-text">The currently used prompt is:</label>
                        <textarea id="prompt-text" name="prompt-text" class="form-control h-100"></textarea>
                        <div class="form-check form-switch mt-1">
                            <input class="form-check-input" type="checkbox" id="set-default-prompt" name="set-default-prompt">
                            <label class="form-check-label" for="set-default-prompt">Set prompt as default</label>
                        </div>
                        <div class="row g-2 justify-content-end">
                            <div class="col-auto">
                                <button type="submit" class="btn btn-primary" id="prompt-save-btn">Save</button>
                            </div>
                            <div class="col-auto">
                                <button type="button" class="btn btn-secondary" id="prompt-restore-btn">Restore Default Prompt</button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="card-footer">
                    <div class="btn-group">
                        <a download="prompts.csv" href="${url.forPath(Paths.ADMIN_ASSISTANT)}?action=downloadPrompts&fileType=csv"
                           type="button" class="btn btn-sm btn-outline-secondary" id="download">
                            <i class="fa fa-download me-1"></i>
                            Download all prompts
                        </a>
                        <a download="prompts.csv" href="${url.forPath(Paths.ADMIN_ASSISTANT)}?action=downloadPrompts&fileType=csv"
                           type="button" class="btn btn-sm btn-outline-secondary" id="download-csv">
                            as CSV
                        </a>
                        <a download="prompts.json" href="${url.forPath(Paths.ADMIN_ASSISTANT)}?action=downloadPrompts&fileType=json"
                           type="button" class="btn btn-sm btn-outline-secondary" id="download-json">
                            as JSON
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <t:modal id="prompt-explanation" title="Prompt Explanation">
        <jsp:attribute name="content">
            <p>
                EMPTY
            </p>
        </jsp:attribute>
    </t:modal>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';

        async function makeGetRequest(url, params, callBack) {
            var request = new XMLHttpRequest();
            request.onreadystatechange = () => {
                callBack(request);
            }
            request.open("GET", url + "?" + params);
            request.send();
        }

        async function makePostRequest(url, contentType, body, callBack) {
            var request = new XMLHttpRequest();
            request.onreadystatechange = () => {
                callBack(request);
            }
            request.open("POST", url);
            if(contentType !== null) {
                request.setRequestHeader("Content-Type", contentType);
            }
            request.send(body);
        }

        (function() {

            var userSettingsManager = new UsersSettingsManager();

            userSettingsManager.getSettingsList();
            userSettingsManager.registerEvents();

            function UsersSettingsManager() {

                this.currentSettings = [];
                this.changesMap = {};

                this.registerEvents = function() {
                    document.getElementById("users-save-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let body = {
                            "action": "usersSettingsUpdate",
                            "usersSettings": this.changesMap
                        };
                        body = JSON.stringify(body);
                        makePostRequest("/admin/assistant", "application/json;charset=UTF-8", body, (request) => {
                            if(request.readyState === XMLHttpRequest.DONE) {
                                if(request.status === 200) {
                                    let responseBody = request.responseText;
                                    let bodyType = request.getResponseHeader("Content-Type");
                                    if(bodyType === "application/json;charset=UTF-8") {
                                        responseBody = JSON.parse(responseBody);
                                        window.location.replace(responseBody.redirect);
                                    }
                                    else {
                                        history.replaceState(null, "", request.responseURL);
                                        document.open();
                                        document.write(responseBody);
                                        document.close();
                                        // The browser back button appears to go back 2 times since the displayed html
                                        // is not in a real new page
                                    }
                                }
                                else {
                                    let responseBody = request.responseText;
                                    history.replaceState(null, "", "/admin/assistant");
                                    document.open();
                                    document.write(responseBody);
                                    document.close();
                                    // The back button appears to go back 2 times since the error page is not really a
                                    // new page
                                }
                            }
                        });
                    });

                    document.getElementById("users-cancel-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        this.changesMap = {};
                        this.updateUsersSettingsList(this.currentSettings);
                    });

                    document.getElementById("prompt-save-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let prompt = document.getElementById("prompt-text").value;
                        let flag = document.getElementById("set-default-prompt").checked;
                        let body = {
                            "action": "newPrompt",
                            "prompt": prompt,
                            "defaultFlag": flag
                        };
                        body = JSON.stringify(body);
                        makePostRequest("/admin/assistant", "application/json;charset=UTF-8", body, (request) => {
                            if(request.readyState === XMLHttpRequest.DONE) {
                                if(request.status === 200) {
                                    let responseBody = request.responseText;
                                    let bodyType = request.getResponseHeader("Content-Type");
                                    if(bodyType === "application/json;charset=UTF-8") {
                                        responseBody = JSON.parse(responseBody);
                                        window.location.replace(responseBody.redirect);
                                    }
                                    else {
                                        history.replaceState(null, "", request.responseURL);
                                        document.open();
                                        document.write(responseBody);
                                        document.close();
                                        // The browser back button appears to go back 2 times since the displayed html
                                        // is not in a real new page
                                    }
                                }
                                else {
                                    let responseBody = request.responseText;
                                    history.replaceState(null, "", "/admin/assistant");
                                    document.open();
                                    document.write(responseBody);
                                    document.close();
                                    // The back button appears to go back 2 times since the error page is not really a
                                    // new page
                                }
                            }
                        });
                    });

                    document.getElementById("prompt-restore-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let body = {
                            "action": "restorePrompt"
                        };
                        body = JSON.stringify(body);
                        makePostRequest("/admin/assistant", "application/json;charset=UTF-8", body, (request) => {
                            if(request.readyState === XMLHttpRequest.DONE) {
                                if(request.status === 200) {
                                    let responseBody = request.responseText;
                                    let bodyType = request.getResponseHeader("Content-Type");
                                    if(bodyType === "application/json;charset=UTF-8") {
                                        responseBody = JSON.parse(responseBody);
                                        window.location.replace(responseBody.redirect);
                                    }
                                    else {
                                        history.replaceState(null, "", request.responseURL);
                                        document.open();
                                        document.write(responseBody);
                                        document.close();
                                        // The browser back button appears to go back 2 times since the displayed html
                                        // is not in a real new page
                                    }
                                }
                                else {
                                    let responseBody = request.responseText;
                                    history.replaceState(null, "", "/admin/assistant");
                                    document.open();
                                    document.write(responseBody);
                                    document.close();
                                    // The back button appears to go back 2 times since the error page is not really a
                                    // new page
                                }
                            }
                        });
                    });

                    document.getElementById("enable-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let body = {
                            "action": "assistantEnabledUpdate",
                            "assistantEnabled": true
                        };
                        body = JSON.stringify(body);
                        makePostRequest("/admin/assistant", "application/json;charset=UTF-8", body, (request) => {
                            if(request.readyState === XMLHttpRequest.DONE) {
                                if(request.status === 200) {
                                    let responseBody = request.responseText;
                                    let bodyType = request.getResponseHeader("Content-Type");
                                    if(bodyType === "application/json;charset=UTF-8") {
                                        responseBody = JSON.parse(responseBody);
                                        window.location.replace(responseBody.redirect);
                                    }
                                    else {
                                        history.replaceState(null, "", request.responseURL);
                                        document.open();
                                        document.write(responseBody);
                                        document.close();
                                        // The browser back button appears to go back 2 times since the displayed html
                                        // is not in a real new page
                                    }
                                }
                                else {
                                    let responseBody = request.responseText;
                                    history.replaceState(null, "", "/admin/assistant");
                                    document.open();
                                    document.write(responseBody);
                                    document.close();
                                    // The back button appears to go back 2 times since the error page is not really a
                                    // new page
                                }
                            }
                        });
                    });

                    document.getElementById("disable-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let body = {
                            "action": "assistantEnabledUpdate",
                            "assistantEnabled": false
                        };
                        body = JSON.stringify(body);
                        makePostRequest("/admin/assistant", "application/json;charset=UTF-8", body, (request) => {
                            if(request.readyState === XMLHttpRequest.DONE) {
                                if(request.status === 200) {
                                    let responseBody = request.responseText;
                                    let bodyType = request.getResponseHeader("Content-Type");
                                    if(bodyType === "application/json;charset=UTF-8") {
                                        responseBody = JSON.parse(responseBody);
                                        window.location.replace(responseBody.redirect);
                                    }
                                    else {
                                        history.replaceState(null, "", request.responseURL);
                                        document.open();
                                        document.write(responseBody);
                                        document.close();
                                        // The browser back button appears to go back 2 times since the displayed html
                                        // is not in a real new page
                                    }
                                }
                                else {
                                    let responseBody = request.responseText;
                                    history.replaceState(null, "", "/admin/assistant");
                                    document.open();
                                    document.write(responseBody);
                                    document.close();
                                    // The back button appears to go back 2 times since the error page is not really a
                                    // new page
                                }
                            }
                        });
                    });
                }

                this.getSettingsList = function() {
                    makeGetRequest("/admin/assistant", "action=allSettings", (request) => {
                        if(request.readyState === XMLHttpRequest.DONE) {
                            if(request.status === 200) {
                                let responseBody = request.responseText;
                                let bodyType = request.getResponseHeader("Content-Type");
                                if(bodyType === "application/json;charset=UTF-8") {
                                    responseBody = JSON.parse(responseBody);
                                    this.currentSettings = responseBody.usersSettings;
                                    this.updateUsersSettingsList(this.currentSettings);
                                    new DataTable('#users-settings-table', {
                                        searching: true,
                                        order: [[1, "asc"]],
                                        "columnDefs": [{
                                            "targets": 4,
                                            "orderable": false
                                        }],
                                        scrollY: '800px',
                                        scrollCollapse: true,
                                        paging: false,
                                        language: {info: 'Showing _TOTAL_ entries'}
                                    });
                                    this.updateQuestionsAmountTable(responseBody.questionsPerDay);
                                    this.updateTotalQuestionsAmount(responseBody.totalQuestions);
                                    this.updatePromptText(responseBody.prompt);
                                    this.updateAssistantStatus(responseBody.assistantEnabled)
                                }
                                else {
                                    history.replaceState(null, "", request.responseURL);
                                    document.open();
                                    document.write(responseBody);
                                    document.close();
                                    // The browser back button appears to go back 2 times since the displayed html
                                    // is not in a real new page
                                }
                            }
                            else {
                                let responseBody = request.responseText;
                                history.replaceState(null, "", "/admin/assistant");
                                document.open();
                                document.write(responseBody);
                                document.close();
                                // The back button appears to go back 2 times since the error page is not really a
                                // new page
                            }
                        }
                    });
                }

                this.updateUsersSettingsList = function(settings) {
                    let tableBody = document.getElementById("users-settings-table-body");
                    let columnsList = ["userId", "username", "email", "questionsNumber"];
                    let assistantTypes = ["None", "Not Guided"];
                    tableBody.replaceChildren();
                    for(let user of settings) {
                        let row = document.createElement("tr");
                        for(let prop of columnsList) {
                            let column = document.createElement("td");
                            column.innerText = user[prop];
                            row.appendChild(column);
                        }
                        let column = document.createElement("td");
                        let select = document.createElement("select");
                        select.classList.add("form-select");
                        select.addEventListener("change", (e) => {
                            let id = user["userId"];
                            this.changesMap[id] = e.target.value;
                        });
                        for(let type of assistantTypes) {
                            let option = document.createElement("option");
                            option.value = type.toUpperCase().replace(" ", "_");
                            option.innerText = type;
                            if(type.toUpperCase().replace(" ", "_") === user["assistantType"]) {
                                option.selected = true;
                            }
                            select.appendChild(option);
                        }
                        column.appendChild(select)
                        row.appendChild(column);
                        tableBody.appendChild(row);
                    }
                }

                this.updateQuestionsAmountTable = function(questionsPerDay) {
                    let tableBody = document.getElementById("questions-amount-table-body");
                    tableBody.replaceChildren();
                    for(let day in questionsPerDay) {
                        let row = document.createElement("tr");
                        let column = document.createElement("td");
                        column.innerText = day;
                        row.appendChild(column);
                        column = document.createElement("td");
                        column.innerText = questionsPerDay[day];
                        row.appendChild(column);
                        tableBody.appendChild(row);
                    }
                    new DataTable('#questions-amount-table', {
                        searching: false,
                        order: [[0, "desc"]],
                        scrollY: '800px',
                        scrollCollapse: true,
                        paging: false,
                        info: false
                    });
                }

                this.updateTotalQuestionsAmount = function(amount) {
                    document.getElementById("total-questions-amount").innerText = amount;
                }

                this.updatePromptText = function(prompt) {
                    document.getElementById("prompt-text").value = prompt;
                }

                this.updateAssistantStatus = function(enabled) {
                    let status = document.getElementById("assistant-status");
                    let enableButton = document.getElementById("enable-btn");
                    let disableButton = document.getElementById("disable-btn");
                    if(enabled) {
                        status.innerText = "ENABLED";
                        status.classList.add("enabled");
                        status.classList.remove("disabled");
                        enableButton.disabled = true;
                        disableButton.disabled = false;
                    }
                    else {
                        status.innerText = "DISABLED";
                        status.classList.add("disabled");
                        status.classList.remove("enabled");
                        disableButton.disabled = true;
                        enableButton.disabled = false;
                    }
                }

            }

        }());
    </script>

</div>

<%@ include file="/jsp/footer.jsp" %>
