$(
    function () {
        $("#menu").menu();
        $(".ndraggable").draggable();
        $(".resizable").resizable();
        $("#sortable").sortable();
        $("#sortable").disableSelection();
        $(".widget input[type=submit], .widget a, .widget button").button();
        $("button, input, a").click(function (event) {
            event.preventDefault();
        });

        $(".ui-widget-content").each(function (a, content) {
            $(".ui-widget-header", content).each(function (a, header) {
                console.log(header);
                $(header).on("click", function () {

                    $(".data", content).toggle("fold", {}, 500);
                })

            })
        });

        function extractPath() {
            return $("#editor textarea").val().trim().split(/\s+/).join("/");
        }

        function resize(q) {
            var w = $(this).width();
            $(".wide").width(w - 80);
            $(".wide textarea").width(w - 180);
            $(".half-wide").width(w / 2 - 40);
        };


        $(window).resize(resize);

        resize();

        function add(qq) {
            console.log(qq);
            console.log($(this).text());

            $("#editor textarea").val(
                $("#editor textarea").val() + " " + $(this).text() + " "
            );

            onTextAreaUpdate(qq);
        };

        function onPhraseEnd() {
            $("<p/>", {
                text: $("#editor textarea").val()
            }).appendTo("#text")

            var path = extractPath();
            $("#editor textarea").val("")

            $.getJSON("/phrase/" + path + "?data={}", "", function (data) {
                {
                    var items = [];

                    $.each($(data.next).slice(0, 4), function (key, val) {
                        items.push("<tr><td  class=\"word\">" + val.value + "</td><td>" + val.weight + "</td></tr>");
                    });

                    $("#promptNext .data").append(
                        $("<table/>", {
                            "class": "",
                            html: items.join("")
                        })
                    );

                    $("#promptNext .word").on("click", add);
                }

            })
        }

        function onTextAreaUpdate() {
            if ($("#editor textarea").val().endsWith(" ")) {
                $.getJSON("/phrase/" + extractPath() + "?data={}", "", function (data) {
                    var highlight = $(data.theSame)
                        .map(function (n, item) {
                            return item.value;
                        });

                    {
                        var items = [];
                        $.each($(data.continue).slice(0, 100), function (key, item) {
                            if ($.inArray(item.value, highlight) >= 0) {
                                console.log("highlight " + item.value);
                                items.push("<span class=\"word highlight\">" + item.value + "</span>");
                            } else {
                                items.push("<span class=\"word\">" + item.value + "</span>");
                            }

                        });

                        $("#prompt").html(
                            $("<span/>", {
                                "class": "my-new-list",
                                html: items.join("<span> </span>")
                            })
                        );

                        $("#prompt .word").on("click", add);

                    }

                    {
                        var items = [];

                        $.each($(data.theSame).slice(0, 10), function (key, val) {
                            items.push("<tr><td class=\"word\">" + val.value + "</td><td>" + val.weight + "</td></tr>");
                        });

                        $("#promptTheSame .data").html(
                            $("<table/>", {
                                "class": "",
                                html: items.join("")
                            })
                        );

                        $("#promptTheSame .word").on("click", add);
                    }
                })
            }
        }

        $("#submit").on("click", onPhraseEnd);

        $("#editor textarea").on("keyup", function (event) {
            if (event.which == 32) onTextAreaUpdate()
        });

        $("#editor textarea").on("keydown", function (event) {
         //   console.log(event);
            if (event.key == "Enter") {
                onPhraseEnd();
            }
        });

        var page = $("textarea");
        var basicControls = ["#print", "#check", "#generate", "#keywords", "#probability"];
        var valueControls = ["#advice", "#zoom"];

        $("#print").button({
            "icon": "ui-icon-print",
            "showLabel": false
        });

        $("#redo").button({
            "icon": "ui-icon-arrowreturnthick-1-e",
            "showLabel": false
        });

        $("#undo").button({
            "icon": "ui-icon-arrowreturnthick-1-w",
            "showLabel": false
        });

        $("#doGenerate").button({
            "icon": "ui-icon-arrowreturnthick-1-w",
            "showLabel": true
        }).on("click change selectmenuchange",
            function () {
                console.log(extractPath())
                $.getJSON("/phrase/" + extractPath() + "/generate?data={}", "", function (data) {
                    console.log(data);
                    $.each(data.suggest, function (key, val) {
                        $("<tr><td class=\"phrase\">" + val.value + "</td><td>" + val.weight + "</td></tr>")
                            .prependTo("#generate .data table")
                            .find(".phrase")
                            .on("click", function () {
                                $("#editor textarea").val($(this).text());
                                onTextAreaUpdate();
                                $(this).parent("tr").remove();
                            });

                        $("#generate .data table tr").slice(10).each(function () {
                            $(this).remove()
                        })

                        //$("#generate .phrase").last()
                    })
                })
            });

        $("#doAdvice").button({
            "icon": "ui-icon-arrowreturnthick-1-w",
            "showLabel": true
        }).on("click change selectmenuchange",
            function () {
                console.log(extractPath())
                $.getJSON("/phrase/" + extractPath() + "/advice?data={}", "", function (data) {
                    console.log(data);
                    $("#advice .data table tr").remove();
                    $.each(data.suggest.slice(0, 20), function (key, val) {
                        $("#advice .data table").append(
                            "<tr><td class=\"phrase\">" + val.value + "</td><td>" + val.weight + "</td></tr>"
                        );

                        $("#advice .phrase").last().on("click", function () {
                            $("#editor textarea").val($(this).text())
                            onTextAreaUpdate();
                            $(this).parent("tr").remove();
                        })
                    })
                })
            });

        $(".toolbar").controlgroup();

        $("#zoom").on("selectmenuchange", function () {
            page.css({
                "zoom": $(this).val()
            });
        });

        $("#clear").on("click", function () {
            $("#editor textarea").val("");
            onTextAreaUpdate();
        });

        $(basicControls.concat(valueControls).join(", ")).on("click change selectmenuchange",
            function () {
                document.execCommand(
                    this.id,
                    false,
                    $(this).val()
                );
            });

        $("#editor textarea").val(" ");

        onTextAreaUpdate();
    }
);
