
<link href="${url.forPath("/css/specific/smart_assistant.css")}" rel="stylesheet">

<div>
    <div class="game-component-header">
        <h3>Previous Questions</h3>
    </div>
    <div class="card game-component-resize assistant-container">
        <div id="previous-question" hidden>
            <div class="card-header">
                <b>Question: </b>
                <p id="previous-question-text"></p>
            </div>
            <div class="card-body">
                <b>Answer: </b>
                <p id="previous-answer-text"></p>
            </div>
        </div>
    </div>
    <div class="assistant-buttons-right-container">
        <button id="prev-question-btn" class="btn assistant-button" disabled>&lt;</button>
        <button id="next-question-btn" class="btn assistant-button" disabled>&gt;</button>
    </div>
</div>

<script>
    async function makeGetRequest(url, params, callBack) {
        var request = new XMLHttpRequest();
        request.onreadystatechange = () => {
            callBack(request);
        }
        request.open("GET", url + "?" + params);
        request.send();
    }

    var previousQuestionsManager = new PreviousQuestionsManager();
    var prevQuestionButton = document.getElementById("prev-question-btn");
    var nextQuestionButton = document.getElementById("next-question-btn");

    previousQuestionsManager.getQuestionsList();
    previousQuestionsManager.registerEvents();

    function PreviousQuestionsManager() {

        this.questionsList = [];
        this.questionIndex = -1;
        this.maxQuestionIndex = -1;

        this.registerEvents = function() {
            prevQuestionButton.addEventListener("click", () => {
                if(this.questionIndex > 0) {
                    this.questionIndex--;
                    this.displaySelectedQuestion();
                }
            });

            nextQuestionButton.addEventListener("click", () => {
                if(this.questionIndex < this.maxQuestionIndex) {
                    this.questionIndex++;
                    this.displaySelectedQuestion();
                }
            });
        }

        this.getQuestionsList = function() {
            let gameId = document.getElementById("assistant-game-id").value;
            let params = "gameId=" + gameId;
            makeGetRequest("/assistant", params, (request) => {
                if(request.readyState === XMLHttpRequest.DONE) {
                    if(request.status === 200) {
                        let responseBody = request.responseText;
                        let bodyType = request.getResponseHeader("Content-Type");
                        if(bodyType === "application/json;charset=UTF-8") {
                            responseBody = JSON.parse(responseBody);
                            if(responseBody.redirect === undefined) {
                                this.questionsList = responseBody;
                                this.maxQuestionIndex = this.questionsList.length - 1;
                                this.questionIndex = this.maxQuestionIndex;
                                this.displaySelectedQuestion();
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

        this.displaySelectedQuestion = function() {
            if(this.questionsList.length === 0) {
                document.getElementById("previous-question").hidden = true;
                prevQuestionButton.disabled = true;
                nextQuestionButton.disabled = true;
                return;
            }
            let selected = this.questionsList[this.questionIndex];
            document.getElementById("previous-question-text").textContent = selected.question;
            document.getElementById("previous-answer-text").textContent = selected.answer;
            document.getElementById("previous-question").hidden = false;
            prevQuestionButton.disabled = (this.questionIndex === 0);
            nextQuestionButton.disabled = (this.questionIndex === this.maxQuestionIndex);
        }

        this.addLastQuestion = function(question, answer) {
            this.questionsList.push({
                question: question,
                answer: answer
            });
            this.maxQuestionIndex++;
            if(this.maxQuestionIndex === 0) {
                this.questionIndex = 0;
            }
            this.displaySelectedQuestion();
        }

    }
</script>
