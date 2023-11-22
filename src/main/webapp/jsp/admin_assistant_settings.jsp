
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
                    <th>Total Questions Sent</th>
                    <th>Remaining Questions</th>
                    <th>Smart Assistant</th>
                    <th></th>
                </tr>
            </thead>
            <tbody id="users-settings-table-body">

            </tbody>
        </table>

        <div class="row g-2 justify-content-end">
            <div class="col-auto">
                <div class="input-group">
                    <div class="input-group-prepend">
                        <button class="btn btn-primary" type="button" id="add-remove-remaining-btn">Add/Remove Remaining Questions</button>
                    </div>
                    <input type="number" class="form-control" value="0" id="add-remove-remaining-value">
                </div>
            </div>
            <div class="col-auto">
                <button type="submit" class="btn btn-primary" id="users-enable-all-btn">Enable All</button>
            </div>
            <div class="col-auto">
                <button type="submit" class="btn btn-secondary" id="users-disable-all-btn">Disable All</button>
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
                The prompt is the template used by the assistant to send a question to GPT API. Every time a player
                sends a question to the assistant, the question is embedded in the prompt and the resulting text is sent
                for chat completion to the GPT API.
            </p>
            <p>
                The question can be added to the prompt in two ways:
            </p>
            <ul>
                <li>
                    if the prompt contains the placeholder <span class="prompt-placeholder">&lt;user_question&gt;</span>
                    the question is substituted to the placeholder and the assistant sends a unique message containing
                    the resulting text
                </li>
                <li>
                    if the prompt does NOT contain the placeholder <span class="prompt-placeholder">&lt;user_question&gt;</span>
                    then the assistant sends two separate messages to GPT: the first one contains the prompt text and
                    the second one contains the question alone
                </li>
            </ul>
            <p>
                You can add other placeholders in the prompt to include additional information together with the questions.
                Each of these placeholders is substituted by the appropriate information to build the resulting text sent
                to the GPT API. The available placeholders are:
            </p>
            <ul>
                <li>
                    <span class="prompt-placeholder">&lt;class_under_test&gt;</span>: is substituted with the code under
                    test of the game where the question has been made
                </li>
                <li>
                    <span class="prompt-placeholder">&lt;mutants(<i>_message_</i>)&gt;</span>: is substituted with the list
                    of mutants tagged in the question (if any). The list contains the code of each tagged mutant visible
                    to the player making the question and contains the descriptions of the tagged mutants which are not
                    visible to the player. This placeholder takes a parameter <i>_message_</i> which is a string to be
                    inserted before the mutants list (if the list is not empty).
                </li>
                <li>
                    <span class="prompt-placeholder">&lt;tests(<i>_message_</i>)&gt;</span>: is substituted with the list
                    of tests tagged in the question (if any). The list contains the code of each tagged test. This
                    placeholder takes a parameter <i>_message_</i> which is a string to be inserted before the tests list
                    (if the list is not empty).
                </li>
            </ul>
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

                this.registerEvents = function() {
                    document.getElementById("users-enable-all-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let changesList = [];
                        for(let user of this.currentSettings) {
                            if(user["assistantType"] === "NONE") {
                                changesList.push({
                                    "userId": user["userId"],
                                    "assistantType": "NOT_GUIDED",
                                    "remainingQuestionsDelta": 0
                                });
                            }
                        }
                        let body = {
                            "action": "usersSettingsUpdate",
                            "usersSettings": changesList
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

                    document.getElementById("users-disable-all-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let changesList = [];
                        for(let user of this.currentSettings) {
                            if(user["assistantType"] === "NOT_GUIDED") {
                                changesList.push({
                                    "userId": user["userId"],
                                    "assistantType": "NONE",
                                    "remainingQuestionsDelta": 0
                                });
                            }
                        }
                        let body = {
                            "action": "usersSettingsUpdate",
                            "usersSettings": changesList
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

                    document.getElementById("add-remove-remaining-btn").addEventListener("click", (e) => {
                        e.preventDefault();
                        let changesList = [];
                        let delta = document.getElementById("add-remove-remaining-value").value;
                        for(let user of this.currentSettings) {
                            changesList.push({
                                "userId": user["userId"],
                                "assistantType": user["assistantType"],
                                "remainingQuestionsDelta": delta
                            });
                        }
                        let body = {
                            "action": "usersSettingsUpdate",
                            "usersSettings": changesList
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
                        let input = document.createElement("input");
                        input.type = "number";
                        input.value = user["remainingQuestions"];
                        input.classList.add("form-control");
                        column.append(input);
                        row.append(column);
                        column = document.createElement("td");
                        let select = document.createElement("select");
                        select.classList.add("form-select");
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
                        column = document.createElement("td");
                        let button = document.createElement("button");
                        button.disabled = true;
                        button.classList.add("btn", "btn-primary");
                        let i = document.createElement("i");
                        i.classList.add("fa", "fa-check");
                        i.ariaHidden = "true";
                        button.appendChild(i);
                        button.addEventListener("click", (e) => {
                            let row = e.target.closest("tr");
                            let assistantType = row.getElementsByTagName("select")[0].value.toUpperCase();
                            let remainingQuestions = row.getElementsByTagName("input")[0].value;
                            let body = {
                                "action": "usersSettingsUpdate",
                                "usersSettings": [
                                    {
                                        "userId": user["userId"],
                                        "assistantType": assistantType,
                                        "remainingQuestionsDelta": remainingQuestions - user["remainingQuestions"]
                                    }
                                ]
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
                        column.appendChild(button);
                        row.appendChild(column);
                        row.addEventListener("change", (e) => {
                            let button = e.currentTarget.getElementsByTagName("button")[0];
                            button.disabled = false;
                        });
                        tableBody.appendChild(row);
                    }

                    // https://datatables.net/examples/plug-ins/dom_sort.html
                    // Create an array with the values of all the input boxes in a column, parsed as numbers
                    DataTable.ext.order['dom-text-numeric'] = function (settings, col) {
                        return this.api()
                                .column(col, { order: 'index' })
                                .nodes()
                                .map(function (td, i) {
                                    let el = td.querySelector('input');
                                    return el ? el.value * 1 : 0;
                                });
                    };

                    // Create an array with the values of all the select options in a column
                    DataTable.ext.order['dom-select'] = function (settings, col) {
                        return this.api()
                                .column(col, { order: 'index' })
                                .nodes()
                                .map(function (td, i) {
                                    let el = td.querySelector('select');
                                    return el ? el.value : 0;
                                });
                    };

                    new DataTable('#users-settings-table', {
                        searching: true,
                        order: [[1, "asc"]],
                        "columnDefs": [{
                            "targets": 4,
                            "orderDataType": "dom-text-numeric"
                        }, {
                            "targets": 5,
                            "orderDataType": "dom-select"
                        }, {
                            "targets": 6,
                            "orderable": false
                        }],
                        scrollY: '679px',
                        scrollCollapse: true,
                        paging: false,
                        language: {info: 'Showing _TOTAL_ entries'}
                    });
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
