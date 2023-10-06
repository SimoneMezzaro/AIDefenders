
<div>
    <div class="col-xl-6 col-12" id="chat-div">
        <div class="game-component-header">
            <h3>Smart Assistant</h3>
        </div>
        <form id="new-question">
            <div>
                <textarea id="current-question" name="question" placeholder="Write your question here" style="width: 100%"></textarea>
            </div>
            <input id="sub-question-btn" type="submit" class="btn btn-defender btn-highlight" value="Submit" disabled>
        </form>
        <div id="question-answer" hidden>
            <div id="last-question">
            </div>
            <div id="last-answer">
            </div>
            <button id="new-question-btn" class="btn btn-defender btn-highlight">New Question</button>
        </div>
    </div>

    <div class="col-xl-6 col-12" id="questions-div"></div>
</div>

<script>
    function makePostRequest(url, contentType, body, callBack) {
        var request = new XMLHttpRequest();
        request.onreadystatechange = () => {
            callBack(request);
        }
        request.open("POST",url);
        if(contentType !== null) {
            request.setRequestHeader("Content-Type", contentType);
        }
        request.send(body);
    }

    (function() {

        var currentQuestionManager = new CurrentQuestionManager();
        var currentQuestionBox = document.getElementById("current-question")
        var submitButton = document.getElementById("sub-question-btn");
        var lastQuestionBox = document.getElementById("last-question");
        var lastAnswerBox = document.getElementById("last-answer");
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
                lastQuestionBox.textContent = question;
                lastAnswerBox.textContent = answer;
                document.getElementById("new-question").hidden = true;
                document.getElementById("question-answer").hidden = false;
            }

            this.newQuestion = function() {
                currentQuestionBox.value = "";
                document.getElementById("question-answer").hidden = true;
                document.getElementById("new-question").hidden = false;
            }
        }

    }());
</script>
