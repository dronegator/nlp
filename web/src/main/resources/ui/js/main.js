$(
    function () {
        if ("WebSocket" in window) {
            var ws = new WebSocket("ws:/" + window.location.hostname + ":" + window.location.port + "/websocket");
            ws.onopen = function () {
                ws.send("Message to send");

            };
            ws.onmessage = function (evt) {
                var msg = JSON.parse(evt.data);
                console.log(msg);

                if (msg.kind == "Advice") {
                    ensureVisibility("#advice");
                    $("#advice .data table tr").remove();

                    $.each(msg.event.suggest.slice(0, 20), function (key, val) {
                        $("#advice .data table").append(
                            "<tr><td class=\"phrase\">" + val.value + "</td><td>" + val.weight + "</td></tr>"
                        );

                        $("#advice .phrase").last().on("click", function () {
                            $("#editor textarea").val($(this).text())
                            onTextAreaUpdate();
                            $(this).parent("tr").remove();
                        })
                    });
                } else if (msg.kind == "Ping") {
                    $("#ping").text(msg.event.n)
                }
            };

            ws.onclose = function () {
                console.log("Connection is closed...");
            };
        } else {
            // The browser doesn't support WebSocket
            alert("WebSocket NOT supported by your Browser!");
        }

        $("#menu").menu();
        $(".ndraggable").draggable();
        $(".resizable").resizable();
        $("#sortable").sortable({
          "handle" : ".ui-widget-header"
        });
        //$("#sortable").disableSelection();
        $(".widget input[type=submit], .widget a, .widget button").button();
        $("button, input, a").click(function (event) {
            event.preventDefault();
        });

        $(document).tooltip();

        $.getJSON("/system/version", "", function (data) {
            $("#sysinfo .version").html(
                $("<span/>", {
                    "class": "",
                    text: data.name + "-" + data.version + "-" + data.branch + "-" + data.buildTime
                })
            );
        });

        $.getJSON("/system/vocabulary", "", function (data) {
            $("#sysinfo .vocabulary").html(
                $("<span/>", {
                    "class": "",
                    text:

                        data.meaningSize + ", " +
                        data.nGram1Size + ", " +
                        data.nGram2Size + ", " +
                        data.nGram3Size + ", " +
                        data.phraseCorrelationInnerSize + ", " +
                        data.phraseCorrelationOuterSize
                })
            );
        });

        function extractPath() {
            return $("#editor textarea").val().trim().split(/\s+/).join("/");
        }

        function resize(q) {
            var w = $(this).width();
            $(".wide").width(w - 80);
            $(".wide textarea").width(w - 180);
            $(".half-wide").width(w / 2 - 40 - 4);
            $(".quart-wide").width(w / 4 - 20 - 6);
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

        function ensureVisibility(selector) {
            if (!$(selector).is(":visible")) {
                $($(selector).parents(".ui-widget").show("puff").data("companion")).hide("slide");
            }
        }


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

                    if (items.length > 0) {
                        ensureVisibility("#promptNext");

                        $("#promptNext .data").append(
                            $("<table/>", {
                                "class": "",
                                html: items.join("")
                            })
                        );
                        $("#promptNext .word").on("click", add);
                    }
                }
            });
        }

        function onTextAreaUpdate() {
            if ($("#editor textarea").val().endsWith(" ")) {
                $.getJSON("/phrase/" + extractPath() + "?data={}", "", function (data) {
                    var highlight = $(data.theSame)
                        .map(function (n, item) {
                            return item.value;
                        })
                        .slice(0, 20);

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

                        if (items.length > 0) {
                            ensureVisibility("#promptTheSame");
                            $("#promptTheSame .data").html(
                                $("<table/>", {
                                    "class": "",
                                    html: items.join("")
                                })
                            );

                            $("#promptTheSame .word").on("click", add);
                        }

                    }

                    {
                        $("#statistic .placeholder-probability").text(data.probability);
                        $("#statistic .placeholder-equalizedProbability").text(data.equalizedProbability);
                        $("#statistic .placeholder-keywords").text(data.keywords.join());
                    }
                })
            }
        }

        $(".ui-widget .close").on("click", function () {
            $($(this).parents(".ui-widget").hide("puff").data("companion")).show("slide");
        });

        $(".ui-widget .open").on("click", function () {
            $($(this).hide("slide").data("companion")).parents(".ui-widget").show("puff");
        }).map(function () {
            if ($($(this).data("companion")).is(":visible")) {
                $(this).hide(0);
            }
        });

        $("#reload").on("click", function () {
            location.reload();
        });

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

        $("#doGenerate").button({
            "icon": "ui-icon-arrowreturnthick-1-w",
            "showLabel": true
        }).on("click change selectmenuchange",
            function () {
                console.log(extractPath())
                $.getJSON("/phrase/" + extractPath() + "/generate?data={}", "", function (data) {
                    console.log(data);
                    ensureVisibility("#generate");

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
                    })
                })
            });

        $("#doAdvice").button({
            "icon": "ui-icon-arrowreturnthick-1-w",
            "showLabel": true
        }).on("click change selectmenuchange",
            function () {
                console.log(extractPath())

                var adviceOption = $("#adviceOption").val()
                var changeLimit = Math.max(extractPath().split("/").length / 3, 3);
                var data = {}

                if (adviceOption == "auxiliary") {
                    data = {
                        "varyAuxiliary": true,
                        "stickKeywords": false,
                        "changeLimit": changeLimit
                    }
                } else if (adviceOption == "keywords") {
                    data = {
                        "varyAuxiliary": false,
                        "stickKeywords": true,
                        "changeLimit": changeLimit
                    }
                } else if (adviceOption == "everything") {
                    data = {
                        "varyAuxiliary": false,
                        "stickKeywords": false,
                        "changeLimit": changeLimit
                    }
                } else if (adviceOption == "strong") {
                    data = {
                        "varyAuxiliary": false,
                        "stickKeywords": false,
                        "changeLimit": extractPath().split("/").length
                    }
                } else if (adviceOption == "impossible") {
                    data = {
                        "varyAuxiliary": false,
                        "stickKeywords": false,
                        "changeLimit": changeLimit,
                        "variability": 4,
                        "uncertainty": -1

                    }
                };

                $.post({
                    "url": "/phrase/" + extractPath() + "/advice",
                    "data": JSON.stringify(data),
                    "success": function (data) {
                        console.log(data);
                        ensureVisibility("#advice");
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
                        });
                    },

                    "contentType": "application/json",
                    "content": "json"
                });
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
