
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<%
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
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
            <input id="assistant-game-id" type="hidden" name="gameId" value="<%= game.getId() %>">
            <div class="row g-2 justify-content-end mt-0">
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
                <p id="last-question-text" style="white-space: pre-wrap"></p>
            </div>
            <div class="card-body">
                <b>Answer: </b>
                <p id="last-answer-text" style="white-space: pre-wrap"></p>
            </div>
        </div>
        <div class="row g-2 justify-content-end mt-0">
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
    var lastQuestionBox = document.getElementById("last-question")
    var newButton = document.getElementById("new-question-btn");

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
                submitButton.disabled = true;
                document.getElementById("current-question-div").classList.add("loading");
                let question = currentQuestionBox.value.trim();
                let gameId = document.getElementById("assistant-game-id").value;
                let body = "question=" + question + "&gameId=" + gameId;
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

            newButton.addEventListener("click", () => {
                this.newQuestion();
            });
        }

        this.displayAnswer = function(question, answer) {
            document.getElementById("last-question-text").textContent = question;
            document.getElementById("last-answer-text").textContent = answer;
            newQuestionBox.hidden = true;
            document.getElementById("current-question-div").classList.remove("loading");
            lastQuestionBox.hidden = false;
            previousQuestionsManager.addLastQuestion(question, answer);
            debugger;
        }

        this.newQuestion = function() {
            currentQuestionBox.value = "";
            submitButton.disabled = true;
            lastQuestionBox.hidden = true;
            newQuestionBox.hidden = false;
        }
    }
</script>
