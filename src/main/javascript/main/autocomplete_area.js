import $ from '../thirdparty/jquery';
import '../thirdparty/jquery-ui';

class AutocompleteArea {

    constructor(areaId, tags) {
        this.areaId = areaId;
        this.tags = tags;
        this._init();
    }

    _init() {
        let self = this;
        $(this.areaId)
                .on("keydown", function(e) {
                    if(e.keyCode === $.ui.keyCode.TAB && $(this).autocomplete("instance").menu.active) {
                        e.preventDefault();
                    }
                })
                .autocomplete({
                    minLength: 1,
                    delay: 0,
                    source: function(request, response) {
                        let curr = self._extractCurrentWord(this.element, request.term);
                        if(curr !== "" && curr[0] === '@') {
                            response($.ui.autocomplete.filter(self.tags, curr));
                        } else {
                            response([]);
                        }
                    },
                    select: function(e, ui) {
                        self._completeCurrentWord(this, ui.item.value);
                        return false;
                    },
                    focus: function(e, ui) {
                        return false;
                    }
                });
    }

     _extractCurrentWord(el, text) {
        let word = this._getWordBoundaries(text, el.prop("selectionStart"));
        return text.substr(word.start, word.end - word.start);
    }

    _completeCurrentWord(el, selected) {
        let text = el.value;
        let word = this._getWordBoundaries(text, el.selectionStart);
        let newPosition = word.start + selected.length;
        el.value = text.substring(0, word.start) + selected + text.substring(word.end);
        el.selectionStart = newPosition;
        el.selectionEnd = newPosition;
    }

    _getWordBoundaries(text, position) {
        let start = position;
        let end = position;
        while(start > 0) {
            if(/\s/.test(text[start-1])) {
                break;
            } else if(text[start-1] === '@') {
                start--;
                break;
            }
            start--;
        }
        while(end < text.length) {
            if(/[\s@]/.test(text[end])) {
                break;
            }
            end++;
        }
        return {"start": start, "end": end};
    }

}

export default AutocompleteArea;
