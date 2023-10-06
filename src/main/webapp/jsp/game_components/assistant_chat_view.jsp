
<link href="${url.forPath("/css/specific/smart_assistant.css")}" rel="stylesheet">

<div>
    <div class="game-component-header">
        <h3>Smart Assistant</h3>
    </div>
    <div id="new-question">
        <form>
            <div class="card game-component-resize assistant-container">
                <textarea id="current-question" name="question" placeholder="Write your question here" class="card-body"></textarea>
            </div>
            <div class="assistant-buttons-right-container">
                <input id="sub-question-btn" type="submit" class="btn assistant-button" value="Submit" disabled>
            </div>
        </form>
    </div>

    <div id="last-question" hidden>
        <div class="card game-component-resize assistant-container">
            <div class="card-header">
                <b>Question: </b>
                <p id="last-question-text"></p>
            </div>
            <div class="card-body">
                <b>Answer: </b>
                <p id="last-answer-text"></p>
            </div>
        </div>
        <div class="assistant-buttons-right-container">
            <button id="new-question-btn" class="btn assistant-button">New Question</button>
        </div>
    </div>
</div>

<script>
    function makePostRequest(url, contentType, body, callBack) {
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
                    let question = currentQuestionBox.value.trim();
                    if(question !== "") {
                        currentQuestionBox.value = "";
                        //TODO: add loading between question and answer
                        makePostRequest("/assistant", "application/x-www-form-urlencoded", "question=" + question, (request) => {
                            if(request.readyState === XMLHttpRequest.DONE) {
                                if(request.status === 200) {
                                    var body = JSON.parse(request.responseText);
                                    this.displayAnswer(question, body.answer);
                                }
                                else {
                                    //TODO: manage errors
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
                lastQuestionBox.hidden = false;
            }

            this.newQuestion = function() {
                currentQuestionBox.value = "";
                lastQuestionBox.hidden = true;
                newQuestionBox.hidden = false;
            }
        }

    }());
</script>
