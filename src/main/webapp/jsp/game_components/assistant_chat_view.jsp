
<%@ page import="org.codedefenders.game.AbstractGame" %>

<%
    int assistantGameId = ((AbstractGame) request.getAttribute("game")).getId();
%>

<link href="${url.forPath("/css/specific/smart_assistant.css")}" rel="stylesheet">

<div>
    <div class="game-component-header">
        <h3>Smart Assistant</h3>
    </div>
    <div id="new-question">
        <form>
            <div id="current-question-div" class="card game-component-resize assistant-container">
                <textarea id="current-question" name="question" placeholder="Write your question here" class="card-body"></textarea>
            </div>
            <input id="assistant-game-id" type="hidden" name="gameId" value="<%= assistantGameId %>">
            <div class="row g-2 justify-content-end mt-0">
                <div id="submit-error" class="col-auto align-items-center" hidden>
                    Your question is too long! Use at most 1500 words
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-primary assistant-button" id="sub-question-btn" disabled>Submit</button>
                </div>
            </div>
        </form>
    </div>

    <div id="last-question" hidden>
        <div class="card game-component-resize assistant-container">
            <div class="card-header">
                <b>Question: </b>
                <p id="last-question-text" class="assistant-formatted-text"></p>
            </div>
            <div id="last-answer-box" class="card-body">

            </div>
        </div>
        <div class="row g-2 justify-content-end mt-0">
            <div class="col-auto d-flex align-items-center">Was the answer useful?</div>
            <div class="col-auto">
                <button class="btn btn-success assistant-button" id="yes-btn">Yes</button>
            </div>
            <div class="col-auto">
                <button class="btn btn-danger assistant-button" id="no-btn">No</button>
            </div>
            <div class="col-auto">
                <button class="btn btn-primary assistant-button" id="new-question-btn">New Question</button>
            </div>
        </div>
    </div>
</div>

<script>
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

    var currentQuestionManager = new CurrentQuestionManager();
    var newQuestionBox = document.getElementById("new-question");
    var currentQuestionBox = document.getElementById("current-question")
    var submitButton = document.getElementById("sub-question-btn");
    var submitError = document.getElementById("submit-error");
    var lastQuestionBox = document.getElementById("last-question")
    var newButton = document.getElementById("new-question-btn");
    var yesButton = document.getElementById("yes-btn");
    var noButton = document.getElementById("no-btn");

    currentQuestionManager.registerEvents();

    function CurrentQuestionManager() {
        this.registerEvents = function() {
            currentQuestionBox.addEventListener("input", () => {
                let question = currentQuestionBox.value.trim();
                if(question === "") {
                    submitButton.disabled = true;
                }
                else {
                    submitButton.disabled = false;
                }
            });

            submitButton.addEventListener("click", (e) => {
                e.preventDefault();
                let question = currentQuestionBox.value.trim();
                if(question.split(/\s+/g).length > 1500) {
                    submitError.hidden = false;
                    submitError.classList.add("d-flex");
                    return;
                }
                submitButton.disabled = true;
                document.getElementById("current-question-div").classList.add("loading");
                submitError.hidden = true;
                submitError.classList.remove("d-flex");
                let gameId = document.getElementById("assistant-game-id").value;
                let body = "action=question" + "&question=" + question + "&gameId=" + gameId;
                if(question !== "") {
                    makePostRequest("/assistant", "application/x-www-form-urlencoded", body, (request) => {
                        if(request.readyState === XMLHttpRequest.DONE) {
                            if(request.status === 200) {
                                let responseBody = request.responseText;
                                let bodyType = request.getResponseHeader("Content-Type");
                                if(bodyType === "application/json;charset=UTF-8") {
                                    responseBody = JSON.parse(responseBody);
                                    if(responseBody.redirect === undefined) {
                                        this.displayAnswer(responseBody.question, responseBody.answer);
                                    }
                                    else {
                                        window.location.replace(responseBody.redirect);
                                    }
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
                                history.replaceState(null, "", "/assistant");
                                document.open();
                                document.write(responseBody);
                                document.close();
                                // The back button appears to go back 2 times since the error page is not really a
                                // new page
                            }
                        }
                    });
                }
            });

            yesButton.addEventListener("click", () => {
                let useful = true;
                this.updateFeedbackButtons(useful);
                let gameId = document.getElementById("assistant-game-id").value;
                let body = "action=feedback" + "&feedback=" + useful + "&gameId=" + gameId;
                makePostRequest("/assistant", "application/x-www-form-urlencoded", body, (request) => {
                    if(request.readyState === XMLHttpRequest.DONE) {
                        if(request.status === 200) {
                            let responseBody = request.responseText;
                            let bodyType = request.getResponseHeader("Content-Type");
                            if(bodyType === "application/json;charset=UTF-8") {
                                responseBody = JSON.parse(responseBody);
                                window.location.replace(responseBody.redirect);
                            }
                            else if(bodyType === "text/html;charset=UTF-8") {
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
                            history.replaceState(null, "", "/assistant");
                            document.open();
                            document.write(responseBody);
                            document.close();
                            // The back button appears to go back 2 times since the error page is not really a
                            // new page
                        }
                    }
                });
            });

            noButton.addEventListener("click", () => {
                let useful = false;
                this.updateFeedbackButtons(useful);
                let gameId = document.getElementById("assistant-game-id").value;
                let body = "action=feedback" + "&feedback=" + useful + "&gameId=" + gameId;
                makePostRequest("/assistant", "application/x-www-form-urlencoded", body, (request) => {
                    if(request.readyState === XMLHttpRequest.DONE) {
                        if(request.status === 200) {
                            let responseBody = request.responseText;
                            let bodyType = request.getResponseHeader("Content-Type");
                            if(bodyType === "application/json;charset=UTF-8") {
                                responseBody = JSON.parse(responseBody);
                                window.location.replace(responseBody.redirect);
                            }
                            else if(bodyType === "text/html;charset=UTF-8") {
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
                            history.replaceState(null, "", "/assistant");
                            document.open();
                            document.write(responseBody);
                            document.close();
                            // The back button appears to go back 2 times since the error page is not really a
                            // new page
                        }
                    }
                });
            });

            newButton.addEventListener("click", () => {
                this.newQuestion();
            });
        }

        this.displayAnswer = function(question, answer) {
            document.getElementById("last-question-text").textContent = question;
            let answerBox = document.getElementById("last-answer-box");
            answerBox.replaceChildren();
            try {
                let jsonAnswer = JSON.parse(answer);
                for(let key in jsonAnswer) {
                    let value = jsonAnswer[key];
                    if(value !== undefined && value !== null && value.trim() !== "") {
                        let b = document.createElement("b");
                        b.textContent = key[0].toUpperCase() + key.slice(1) + ": ";
                        answerBox.appendChild(b);
                        let p = document.createElement("p");
                        p.textContent = value;
                        p.classList.add("assistant-formatted-text");
                        answerBox.appendChild(p);
                    }
                }
            } catch (error) {
                answerBox.replaceChildren();
                let b = document.createElement("b");
                b.textContent = "Answer: ";
                answerBox.appendChild(b);
                let p = document.createElement("p");
                p.textContent = answer;
                p.classList.add("assistant-formatted-text");
                answerBox.appendChild(p);
            }
            newQuestionBox.hidden = true;
            document.getElementById("current-question-div").classList.remove("loading");
            lastQuestionBox.hidden = false;
            previousQuestionsManager.addLastQuestion(question, answer);
        }

        this.updateFeedbackButtons = function(useful) {
            if(useful) {
                yesButton.disabled = true;
                yesButton.classList.add("feedback-disabled");
                noButton.disabled = false;
                noButton.classList.remove("feedback-disabled");
            } else {
                yesButton.disabled = false;
                yesButton.classList.remove("feedback-disabled");
                noButton.disabled = true;
                noButton.classList.add("feedback-disabled");
            }
        }

        this.newQuestion = function() {
            currentQuestionBox.value = "";
            submitButton.disabled = true;
            submitError.hidden = true;
            submitError.classList.remove("d-flex");
            lastQuestionBox.hidden = true;
            newQuestionBox.hidden = false;
        }
    }
</script>
