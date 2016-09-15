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
            var value = $("#editor textarea").val()
            console.log(value);
            var value1 = value.trim().split(/\s+/).join("/")

            $("<p/>", {
                text: value
            }).appendTo("#text")

            $("#editor textarea").val("")

            $.getJSON("/phrase/" + value1 + "?data={}", "", function (data) {
                console.log(data); {
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

            var value = $("#editor textarea").val()
            console.log(value);
            var value1 = value.trim().split(/\s+/).join("/")

            console.log(value1);

            if (value.endsWith(" ")) {
                $.getJSON("/phrase/" + value1 + "?data={}", "", function (data) {
                    console.log(data);

                    {
                        var items = [];
                        $.each($(data.continue).slice(0, 100), function (key, val) {
                            console.log(key);
                            console.log(val);
                            console.log(val.value);
                            items.push("<span class=\"word\">" + val.value + "</span>");
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
            } else if (value.endsWith(".")) {
                onPhraseEnd();
            }
        }

        $("#submit").on("click", onPhraseEnd);

        $("#editor textarea").on("keyup", onTextAreaUpdate);

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

        $("#generate").button({
            "icon": "ui-icon-arrowreturnthick-1-w",
            "showLabel": false
        }).on("click change selectmenuchange",
            function() {
              console.log(extractPath())
              $.getJSON("/phrase/" + extractPath() + "/generate?data={}", "", function (data) {
                              console.log(data); {
                                  $("#editor textarea").val(data.suggest[0].value);
                              }
                          })
            });

        $(".toolbar").controlgroup();

        $("#zoom").on("selectmenuchange", function () {
            page.css({
                "zoom": $(this).val()
            });
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
