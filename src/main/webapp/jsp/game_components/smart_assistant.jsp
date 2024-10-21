
<link href="${url.forPath("/css/specific/smart_assistant.css")}" rel="stylesheet">

<div class="row">
    <div class="col">
        <div id="assistant-collapsable" class="row accordion-collapse collapse show">
            <div class="col-xl-6 col-12" id="assistant-chat-div">
                <jsp:include page="../game_components/assistant_chat_view.jsp"/>
            </div>
            <div class="col-xl-6 col-12" id=previous-questions-div">
                <jsp:include page="../game_components/previous_questions_view.jsp"/>
            </div>
        </div>
        <div class="row justify-content-end mt-2">
            <div class="col-auto">
                <button id="collapse-assistant-btn" class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#assistant-collapsable" aria-controls="ma-collapse-all" aria-expanded="true">
                    Hide Smart Assistant
                </button>
            </div>
        </div>
    </div>
</div>

<script type="module">
    document.getElementById("assistant-collapsable").addEventListener("hide.bs.collapse", () => {
        document.getElementById("collapse-assistant-btn").textContent = "Show Smart Assistant";
    });
    document.getElementById("assistant-collapsable").addEventListener("show.bs.collapse", () => {
        document.getElementById("collapse-assistant-btn").textContent = "Hide Smart Assistant";
    });
</script>
