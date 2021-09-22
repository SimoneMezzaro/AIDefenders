<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="testAccordion" type="org.codedefenders.beans.game.TestAccordionBean"--%>

<%--
    Displays an accordion of tables of tests, grouped by which of the CUT's methods they cover.

    The accordion is generated by the JSP, the tables in the accordion sections as well as popovers and models are
    generated through JavaScript.
--%>

<style>
    <%-- Prefix all classes with "ta-" to avoid conflicts.
    We probably want to extract some common CSS when we finally tackle the CSS issue. --%>

    /* Customization of Bootstrap 5 accordion style.
    ----------------------------------------------------------------------------- */

    #tests-accordion .accordion-button {
        padding: .6rem .8rem;
        background-color: rgba(0,0,0,.03);
    }

    /* Clear the box shadow from .accordion-button. This removes the blue outline when selecting a button, and the
       border between the header and content of accordion items when expanded. */
    #tests-accordion .accordion-button {
        box-shadow: none;
    }
    /* Add back the border between header and content of accordion items. */
    #tests-accordion .accordion-body {
        border-top: 1px solid rgba(0, 0, 0, .125);
    }
    /* Always display the chevron icon in black. */
    #tests-accordion .accordion-button:not(.collapsed)::after {
        /* Copied from Bootstrap 5. */
        background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='%23212529'%3e%3cpath fill-rule='evenodd' d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e");
    }

    /* Categories.
    ----------------------------------------------------------------------------- */

    #tests-accordion .accordion-button:not(.ta-covered) {
        color: #B0B0B0;
    }
    #tests-accordion .accordion-button.ta-covered {
        color: black;
    }

    /* Tables.
    ----------------------------------------------------------------------------- */

    #tests-accordion thead {
        display: none;
    }
    #tests-accordion .dataTables_scrollHead {
        display: none;
    }
    #tests-accordion td {
        vertical-align: middle;
    }
    #tests-accordion table {
        font-size: inherit;
    }
    #tests-accordion tr:last-child > td {
        border-bottom: none;
    }

    /* Inline elements.
    ----------------------------------------------------------------------------- */

    #tests-accordion .ta-column-name {
        color: #B0B0B0;
    }
    #tests-accordion .ta-covered-link,
    #tests-accordion .ta-killed-link,
    #tests-accordion .ta-smells-link {
        cursor: default;
    }
</style>

<div class="accordion" id="tests-accordion">
    <c:forEach items="${testAccordion.categories}" var="category">
        <div class="accordion-item">
            <h2 class="accordion-header" id="ta-heading-${category.id}">
                <%-- ${empty …} doesn't work with Set --%>
                <button class="${category.testIds.size() == 0 ? "" : "ta-covered"} accordion-button collapsed"
                        type="button" data-bs-toggle="collapse"
                        data-bs-target="#ta-collapse-${category.id}"
                        aria-controls="ta-collapse-${category.id}">
                    <%-- ${empty …} doesn't work with Set --%>
                    <c:if test="${!(category.testIds.size() == 0)}">
                        <span class="badge bg-defender me-2 ta-count">${category.testIds.size()}</span>
                    </c:if>
                    ${category.description}
                </button>
            </h2>
            <div class="accordion-collapse collapse"
                 id="ta-collapse-${category.id}"
                 data-bs-parent="#tests-accordion"
                 aria-expanded="false" aria-labelledby="ta-heading-${category.id}">
                <div class="accordion-body p-0">
                    <table id="ta-table-${category.id}" class="table table-sm"></table>
                </div>
            </div>
        </div>
    </c:forEach>
</div>

<script>
    /* Wrap in a function so it has it's own scope. */
    (function () {

        /** A description and list of test ids for each category (method). */
        const categories = JSON.parse('${testAccordion.categoriesAsJSON}');

        /** Maps test ids to their DTO representation. */
        const tests = new Map(JSON.parse('${testAccordion.testsAsJSON}'));

        /** Maps test ids to modals that show the tests' code. */
        const testModals = new Map();

        /* Functions to generate table columns. */
        const genId             = row => `Test \${row.id}`;
        const genCreator        = row => row.creator.name;
        const genPoints         = row => `<span class="ta-column-name">Points:</span> \${row.points}`;
        const genCoveredMutants = row => `<span class="ta-covered-link"><span class="ta-column-name">Covered:</span> \${row.coveredMutantIds.length}</span>`;
        const genKilledMutants  = row => `<span class="ta-killed-link"><span class="ta-column-name">Killed:</span> \${row.killedMutantIds.length}</span>`;
        const genViewButton     = row => row.canView ? '<button class="ta-view-button btn btn-xs btn-primary">View</button>' : '';
        const genSmells         = row => {
            const numSmells = row.smells.length;
            let smellLevel;
            let smellColor;
            if (numSmells >= 3) {
                smellLevel = 'Bad';
                smellColor = 'btn-danger';
            } else if (numSmells >= 1) {
                smellLevel = 'Fishy';
                smellColor = 'btn-warning';
            } else {
                smellLevel = 'Good';
                smellColor = 'btn-success';
            }
            return `<a class="ta-smells-link btn btn-xs \${smellColor}">\${smellLevel}</a>`;
        };

        /**
         * Returns the test DTO that describes the row of an element in a DataTables row.
         * @param {HTMLElement} element An HTML element contained in a table row.
         * @param {object} dataTable The DataTable the row belongs to.
         * @return {object} The test DTO the row describes.
         */
        const rowData = function (element, dataTable) {
            const row = $(element).closest('tr');
            return dataTable.row(row).data();
        };

        /**
         * Sets up popovers.
         * @param {object} elements A collection of DOM elements, as returned by querySelectorAll.
         * @param {function} getData Gets called with the HTML element the popover is for,
         *                   returns data to call the other functions with.
         * @param {function} genHeading Generates the heading of the popover.
         * @param {function} genBody Generates the body of the popover.
         */
        const setupPopovers = function (elements, getData, genHeading, genBody) {
            for (const element of elements) {
                new bootstrap.Popover(element, {
                    container: document.body,
                    template:
                        `<div class="popover" role="tooltip">
                            <div class="popover-arrow"></div>
                            <h3 class="popover-header"></h3>
                            <div class="popover-body px-3 py-2" style="max-width: 250px;"></div>
                        </div>`,
                    placement: 'top',
                    trigger: 'hover',
                    html: true,
                    title: function () {
                        const data = getData(this);
                        return genHeading(data);
                    },
                    content: function () {
                        const data = getData(this);
                        return genBody(data);
                    }
                });
            }
        };

        /**
         * Creates a modal to display the given test and shows it.
         * References to created models are cached in a map so they don't need to be generated again.
         * @param {object} test The test DTO to display.
         */
        const viewTestModal = function (test) {
            let modal = testModals.get(test.id);
            if (modal !== undefined) {
                modal.modal('show');
                return;
            }

            modal = $(
                `<div class="modal fade" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-responsive">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Test \${test.id} (by \${test.creator.name})</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <div class="card">
                                    <div class="card-body p-0 codemirror-expand codemirror-test-modal-size">
                                        <pre class="m-0"><textarea name="test-\${test.id}"></textarea></pre>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>`);
            modal.appendTo(document.body);
            testModals.set(test.id, modal);

            const textarea = modal.find('textarea').get(0);
            const editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: true,
                matchBrackets: true,
                mode: "text/x-java",
                readOnly: true,
                autoRefresh: true
            });

            TestAPI.getAndSetEditorValue(textarea, editor);
            modal.modal('show');
        };

        /* Loop through the categories and create a test table for each one. */
        for (const category of categories) {
            const rows = category.testIds
                .sort()
                .map(tests.get, tests);

            /* Create the DataTable. */
            const tableElement = $('#ta-table-' + category.id);
            const dataTable = tableElement.DataTable({
                data: rows,
                columns: [
                    { data: null, title: '', defaultContent: '' },
                    { data: genId, title: '' },
                    { data: genCreator, title: '' },
                    { data: genCoveredMutants, title: '' },
                    { data: genKilledMutants, title: '' },
                    { data: genPoints, title: '' },
                    { data: genSmells, title: '' },
                    { data: genViewButton, title: '' }
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {
                    emptyTable: category.id === 'all'
                        ? 'No tests.'
                        : 'No tests cover this method.'
                }
            });

            /* Assign function to the "View" buttons. */
            tableElement.on('click', '.ta-view-button', function () {
                const test = rowData(this, dataTable);
                viewTestModal(test);
            });

            setupPopovers(
                tableElement[0].querySelectorAll('.ta-covered-link'),
                that => rowData(that, dataTable).coveredMutantIds,
                coveredIds => coveredIds.length > 0
                    ? 'Covered Mutants'
                    : '',
                coveredIds => coveredIds.length > 0
                    ? coveredIds.join(', ')
                    : 'No mutants are covered by this test.',
            );

            setupPopovers(
                tableElement[0].querySelectorAll('.ta-killed-link'),
                that => rowData(that, dataTable).killedMutantIds,
                killedIds => killedIds.length > 0
                    ? 'Killed Mutants'
                    : '',
                killedIds => killedIds.length > 0
                    ? killedIds.join(', ')
                    : 'No mutants were killed by this test.',
            );

            setupPopovers(
                tableElement[0].querySelectorAll('.ta-smells-link'),
                that => rowData(that, dataTable).smells,
                smells => smells.length > 0
                    ? 'Test Smells'
                    : '',
                smells => smells.length > 0
                    ? smells.join('<br>')
                    : 'This test does not have any smells.'
            );
        }
    })();
</script>
