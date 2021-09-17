<%--
  ~ Copyright (C) 2021 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ tag pageEncoding="UTF-8" %>

<div id="create-game-cut-preview">
<div class="card">
    <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
        <pre class="m-0"><textarea name=""></textarea></pre>
    </div>
</div>

<script>
    (function () {
        const cutPreview = document.querySelector('#create-game-cut-preview')

        const classSelector = document.querySelector('#class-select');
        const textarea = cutPreview.querySelector('textarea');

        const updatePreview = function () {
            textarea.setAttribute('name', `class-\${classSelector.value}`)
            const codeMirrorContainer = cutPreview.querySelector('.CodeMirror');

            if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                ClassAPI.getAndSetEditorValue(textarea, codeMirrorContainer.CodeMirror);
            } else {
                const editor = CodeMirror.fromTextArea(textarea, {
                    lineNumbers: true,
                    readOnly: 'nocursor',
                    mode: 'text/x-java',
                    autoRefresh: true
                });
                ClassAPI.getAndSetEditorValue(textarea, editor);
            }
        };

        // Load initial selecte class
        document.addEventListener("DOMContentLoaded", updatePreview);

        classSelector.addEventListener('change', updatePreview);
    })();
</script>
</div>